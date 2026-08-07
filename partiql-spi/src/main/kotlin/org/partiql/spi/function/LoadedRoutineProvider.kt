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

import org.partiql.spi.catalog.Name
import org.partiql.spi.types.PType
import java.util.Collections

/**
 * An immutable, validated snapshot of one [RoutineProvider].
 *
 * Loading alone does not expose routines to SQL. A host passes this value to an explicit mount configuration.
 */
public class LoadedRoutineProvider private constructor(
    functions: Collection<RoutineDefinition<FnOverload>>,
    aggregations: Collection<RoutineDefinition<AggOverload>>,
) {
    internal val functions: List<RoutineDefinition<FnOverload>> = immutableCopy(functions)
    internal val aggregations: List<RoutineDefinition<AggOverload>> = immutableCopy(aggregations)

    public companion object {

        /**
         * Reads each provider inventory once, captures its definition metadata, and validates the complete snapshot.
         *
         * @throws RoutineProviderValidationException if provider access or validation fails
         */
        @JvmStatic
        public fun load(provider: RoutineProvider): LoadedRoutineProvider {
            val functions = access(ProviderInventory.SCALAR) {
                provider.getFunctions().map(::snapshotFunction)
            }
            val aggregations = access(ProviderInventory.AGGREGATE) {
                provider.getAggregations().map(::snapshotAggregation)
            }
            val issues = validate(functions, aggregations)
            if (issues.isNotEmpty()) {
                throw RoutineProviderValidationException.create(issues)
            }
            return LoadedRoutineProvider(functions, aggregations)
        }

        private fun snapshotFunction(
            definition: RoutineDefinition<FnOverload>,
        ): RoutineDefinition<FnOverload> =
            RoutineDefinition(
                definition.sourceName,
                definition.overloads.map { overload ->
                    SnapshotFnOverload(overload, snapshot(overload.signature))
                },
            )

        private fun snapshotAggregation(
            definition: RoutineDefinition<AggOverload>,
        ): RoutineDefinition<AggOverload> =
            RoutineDefinition(
                definition.sourceName,
                definition.overloads.map { overload ->
                    SnapshotAggOverload(overload, snapshot(overload.signature))
                },
            )

        private fun snapshot(signature: RoutineOverloadSignature): RoutineOverloadSignature =
            RoutineOverloadSignature(signature.name, signature.parameterTypes)

        private fun validate(
            functions: List<RoutineDefinition<FnOverload>>,
            aggregations: List<RoutineDefinition<AggOverload>>,
        ): List<RoutineProviderValidationIssue> =
            validateKind(
                ProviderInventory.SCALAR,
                functions.mapIndexed { index, definition ->
                    Declaration(index, definition.sourceName, definition.overloads.map { it.signature })
                },
            ) +
                validateKind(
                    ProviderInventory.AGGREGATE,
                    aggregations.mapIndexed { index, definition ->
                        Declaration(index, definition.sourceName, definition.overloads.map { it.signature })
                    },
                )

        private fun validateKind(
            inventory: ProviderInventory,
            declarations: List<Declaration>,
        ): List<RoutineProviderValidationIssue> {
            val issues = mutableListOf<RoutineProviderValidationIssue>()
            val firstBySourceName = mutableMapOf<Name, Declaration>()
            declarations.sortedWith(DECLARATION_COMPARATOR).forEach { declaration ->
                if (declaration.sourceName.any(String::isEmpty)) {
                    issues += issue(RoutineProviderValidationReason.EMPTY_SOURCE_SEGMENT, inventory, declaration)
                }
                if (declaration.signatures.isEmpty()) {
                    issues += issue(RoutineProviderValidationReason.EMPTY_OVERLOADS, inventory, declaration)
                }

                val sameSourceName = firstBySourceName[declaration.sourceName]
                if (sameSourceName == null) {
                    firstBySourceName[declaration.sourceName] = declaration
                } else {
                    issues += issue(
                        RoutineProviderValidationReason.DUPLICATE_SOURCE_NAME,
                        inventory,
                        declaration,
                        conflicting = sameSourceName,
                    )
                }

                val signatures = mutableListOf<List<PType>>()
                declaration.signatures.forEach { signature ->
                    if (signature.name != declaration.sourceName.getName()) {
                        issues += issue(
                            RoutineProviderValidationReason.SIGNATURE_NAME_MISMATCH,
                            inventory,
                            declaration,
                            signature = signature,
                        )
                    }
                    val parameterTypes = signature.parameterTypes
                    if (signatures.any { it == parameterTypes }) {
                        issues += issue(
                            RoutineProviderValidationReason.DUPLICATE_OVERLOAD_SIGNATURE,
                            inventory,
                            declaration,
                            signature = signature,
                        )
                    } else {
                        signatures += parameterTypes
                    }
                }
            }
            return issues
        }

        private fun issue(
            reason: RoutineProviderValidationReason,
            inventory: ProviderInventory,
            declaration: Declaration,
            conflicting: Declaration? = null,
            signature: RoutineOverloadSignature? = null,
        ): RoutineProviderValidationIssue =
            RoutineProviderValidationIssue.create(
                reason = reason,
                isAggregate = inventory.isAggregate,
                sourceName = declaration.sourceName,
                conflictingSourceName = conflicting?.sourceName,
                signatureName = when (reason) {
                    RoutineProviderValidationReason.SIGNATURE_NAME_MISMATCH -> signature?.name
                    else -> null
                },
                parameterTypes = when (reason) {
                    RoutineProviderValidationReason.DUPLICATE_OVERLOAD_SIGNATURE -> signature?.parameterTypes
                    else -> null
                },
            )

        private fun <T> access(
            inventory: ProviderInventory,
            action: () -> List<T>,
        ): List<T> =
            try {
                immutableCopy(action())
            } catch (cause: Exception) {
                val issue = RoutineProviderValidationIssue.create(
                    reason = RoutineProviderValidationReason.PROVIDER_ACCESS_FAILED,
                    isAggregate = inventory.isAggregate,
                )
                throw RoutineProviderValidationException.create(listOf(issue), cause)
            }

        private fun compareSourceNames(first: Name, second: Name): Int {
            val firstParts = first.toList()
            val secondParts = second.toList()
            for (index in 0 until minOf(firstParts.size, secondParts.size)) {
                val comparison = firstParts[index].compareTo(secondParts[index])
                if (comparison != 0) {
                    return comparison
                }
            }
            return firstParts.size.compareTo(secondParts.size)
        }

        private fun <T> immutableCopy(values: Collection<T>): List<T> =
            Collections.unmodifiableList(ArrayList(values))

        private val DECLARATION_COMPARATOR: Comparator<Declaration> =
            Comparator { first, second ->
                val sourceComparison = compareSourceNames(first.sourceName, second.sourceName)
                if (sourceComparison != 0) sourceComparison else first.index.compareTo(second.index)
            }
    }

    private class Declaration(
        val index: Int,
        val sourceName: Name,
        val signatures: List<RoutineOverloadSignature>,
    )

    private enum class ProviderInventory(
        val isAggregate: Boolean,
    ) {
        SCALAR(false),
        AGGREGATE(true),
    }

    private class SnapshotFnOverload(
        private val delegate: FnOverload,
        private val signature: RoutineOverloadSignature,
    ) : FnOverload() {
        override fun getSignature(): RoutineOverloadSignature = signature

        override fun getInstance(args: Array<PType>): Fn? = delegate.getInstance(args)
    }

    private class SnapshotAggOverload(
        private val delegate: AggOverload,
        private val signature: RoutineOverloadSignature,
    ) : AggOverload() {
        override fun getSignature(): RoutineOverloadSignature = signature

        override fun getInstance(args: Array<PType>): Agg? = delegate.getInstance(args)
    }
}
