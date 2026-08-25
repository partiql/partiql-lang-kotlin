/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.spi.function

import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Namespace
import org.partiql.spi.catalog.RoutineBinding
import org.partiql.spi.types.PType
import java.util.Collections

/**
 * Immutable in-memory [RoutineProvider] built from directly registered overloads.
 *
 * Registration names are complete within a catalog and never include the host catalog name. Overloads with distinct
 * parameter signatures compose under one exact name and routine kind. Registered overloads and their signatures are
 * retained and must be treated as immutable.
 */
public class MemRoutineProvider private constructor(
    private val functions: List<LookupEntry<FnOverload>>,
    private val aggregations: List<LookupEntry<AggOverload>>,
) : RoutineProvider {

    override fun getFunctions(identifier: Identifier): Collection<RoutineBinding<FnOverload>> =
        resolve(identifier, functions)

    override fun getAggregations(identifier: Identifier): Collection<RoutineBinding<AggOverload>> =
        resolve(identifier, aggregations)

    public companion object {

        /**
         * Creates an empty registration builder.
         */
        @JvmStatic
        public fun builder(): Builder = Builder()

        private fun <T> resolve(
            identifier: Identifier,
            entries: List<LookupEntry<T>>,
        ): Collection<RoutineBinding<T>> {
            val identifierParts = identifier.getParts()
            val entry = entries.firstOrNull { candidate ->
                val nameParts = candidate.canonicalNameParts
                identifierParts.size == nameParts.size &&
                    identifierParts.indices.all { identifierParts[it].matches(nameParts[it]) }
            }
            return if (entry == null) {
                Collections.emptyList()
            } else {
                Collections.singletonList(entry.binding)
            }
        }

        private fun regularEquivalent(
            first: Name,
            second: Name,
        ): Boolean {
            val firstParts = first.toList()
            val secondParts = second.toList()
            return firstParts.size == secondParts.size &&
                firstParts.indices.all { firstParts[it].equals(secondParts[it], ignoreCase = true) }
        }
    }

    /**
     * Collects scalar and aggregate overload registrations.
     *
     * A builder is mutable and not safe for concurrent use. Each [build] call returns an independent immutable snapshot.
     */
    public class Builder internal constructor() {
        private val functions = mutableListOf<Registration<FnOverload>>()
        private val aggregations = mutableListOf<Registration<AggOverload>>()

        /**
         * Registers [fn] at the catalog-local root using its signature name.
         */
        public fun register(fn: FnOverload): Builder =
            register(fn, Name.of(fn.signature.name))

        /**
         * Registers [fn] under [namespace] using its signature name as the leaf.
         */
        public fun register(
            fn: FnOverload,
            namespace: Namespace,
        ): Builder =
            register(fn, Name.of(namespace.toList() + fn.signature.name))

        /**
         * Registers [fn] under the complete catalog-local [name]. The final name part must equal the signature name.
         */
        public fun register(
            fn: FnOverload,
            name: Name,
        ): Builder {
            functions += Registration(Name.of(name.toList()), fn)
            return this
        }

        /**
         * Registers [agg] at the catalog-local root using its signature name.
         */
        public fun register(agg: AggOverload): Builder =
            register(agg, Name.of(agg.signature.name))

        /**
         * Registers [agg] under [namespace] using its signature name as the leaf.
         */
        public fun register(
            agg: AggOverload,
            namespace: Namespace,
        ): Builder =
            register(agg, Name.of(namespace.toList() + agg.signature.name))

        /**
         * Registers [agg] under the complete catalog-local [name]. The final name part must equal the signature name.
         */
        public fun register(
            agg: AggOverload,
            name: Name,
        ): Builder {
            aggregations += Registration(Name.of(name.toList()), agg)
            return this
        }

        /**
         * Validates the registrations and returns an immutable provider snapshot.
         *
         * @throws IllegalArgumentException when a registration violates the naming or signature rules
         */
        public fun build(): MemRoutineProvider {
            validateCanonicalNames(
                (functions.map { it.name } + aggregations.map { it.name }).distinct(),
            )
            return MemRoutineProvider(
                functions = bindings("scalar", functions) { it.signature },
                aggregations = bindings("aggregate", aggregations) { it.signature },
            )
        }

        private fun validateCanonicalNames(names: List<Name>) {
            names.forEach { name ->
                require(name.none(String::isEmpty)) {
                    "Routine registration name contains an empty segment: $name"
                }
            }
            names.forEachIndexed { index, name ->
                val conflicting = names.take(index).firstOrNull { regularEquivalent(it, name) }
                require(conflicting == null) {
                    "Routine registration names conflict under regular SQL matching: $conflicting and $name"
                }
            }
        }

        private fun <T> bindings(
            kind: String,
            registrations: List<Registration<T>>,
            signature: (T) -> RoutineOverloadSignature,
        ): List<LookupEntry<T>> {
            val byName = linkedMapOf<Name, MutableList<T>>()
            registrations.forEach { registration ->
                val overloadSignature = signature(registration.overload)
                require(registration.name.getName() == overloadSignature.name) {
                    "Registered $kind routine leaf ${registration.name.getName()} " +
                        "does not match overload name ${overloadSignature.name}"
                }
                val overloads = byName.getOrPut(registration.name) { mutableListOf() }
                val duplicate = overloads.firstOrNull {
                    sameParameters(signature(it).parameterTypes, overloadSignature.parameterTypes)
                }
                require(duplicate == null) {
                    "Duplicate $kind routine signature at ${registration.name}: " +
                        overloadSignature.parameterTypes
                }
                overloads += registration.overload
            }
            return Collections.unmodifiableList(
                byName.map { (name, overloads) ->
                    LookupEntry(
                        canonicalNameParts = name.toList(),
                        binding = RoutineBinding(name, overloads),
                    )
                },
            )
        }

        private fun sameParameters(
            first: List<PType>,
            second: List<PType>,
        ): Boolean =
            first.size == second.size && first.indices.all { first[it] == second[it] }
    }

    private class Registration<T>(
        val name: Name,
        val overload: T,
    )

    private class LookupEntry<T>(
        val canonicalNameParts: List<String>,
        val binding: RoutineBinding<T>,
    )
}
