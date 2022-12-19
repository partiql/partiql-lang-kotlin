/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.sprout.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import net.pearx.kasechange.toCamelCase
import net.pearx.kasechange.toPascalCase
import org.partiql.sprout.generator.types.KotlinTypes
import org.partiql.sprout.generator.types.ScalarTypes
import org.partiql.sprout.model.Domain
import org.partiql.sprout.model.ScalarType
import org.partiql.sprout.model.TypeDef
import org.partiql.sprout.model.TypeRef
import org.partiql.sprout.model.Universe

/**
 * A lookup for type references in code generation
 *
 * @property universe
 * @property options
 *
 * TODO consider thread safe memoization — that was removed because of iterating the refs while updating the backing map
 */
class Symbols private constructor(
    private val universe: Universe,
    private val options: Options,
) {

    /**
     * Kotlin/Java package name
     */
    val rootPackage = options.packageRoot

    /**
     * Universe identifier often prefixed to generated classes
     */
    val rootId = universe.id.toPascalCase()

    /**
     * Base node for the Universe
     */
    val base: ClassName = ClassName(rootPackage, "${rootId}Node")

    private val domains: MutableMap<Domain, ClassName> = mutableMapOf()

    /**
     * Memoize converting a TypeRef.Path to a camel case identifier to be used as method/function names
     */
    private val camels: MutableMap<TypeRef.Path, String> = mutableMapOf()

    /**
     * Map all type references back to their definitions
     */
    private val defs: Map<TypeRef.Path, TypeDef> by lazy {
        val d = mutableMapOf<TypeRef.Path, TypeDef>()
        universe.forEachType { d[it.ref] = it }
        d
    }

    companion object {

        /**
         * Named constructor somewhat hints at the initial emptiness (when memoized) of the symbol table
         */
        fun init(universe: Universe, options: Options): Symbols = Symbols(universe, options)
    }

    /**
     * Returns the base ClassName for the given TypeRef.Path
     */
    fun clazz(ref: TypeRef.Path) = ClassName(
        packageName = rootPackage,
        simpleNames = ref.path.map { it.toPascalCase() }
    )

    /**
     * Returns a camel-case representation of the path.
     *   - This causes naming conflicts, i.e. the path `a_b` conflicts with the path `a.b`
     *   - I'd prefer to start with this naive approach
     */
    fun camel(ref: TypeRef.Path): String = camels.computeIfAbsent(ref) {
        ref.path.joinToString("_").toCamelCase()
    }

    fun def(ref: TypeRef.Path): TypeDef = defs[ref]!!

    /**
     * Computes a type name for the given [TypeRef]
     */
    fun typeNameOf(ref: TypeRef): TypeName = when (ref) {
        is TypeRef.Scalar -> typeNameOf(ref)
        is TypeRef.Any -> typeNameOf(ref)
        is TypeRef.Path -> clazz(ref)
        is TypeRef.List -> typeNameOf(ref)
        is TypeRef.Set -> typeNameOf(ref)
        is TypeRef.Map -> typeNameOf(ref)
    }

    fun typeNameOf(type: ScalarType): TypeName = ScalarTypes.typeNameOf(type)

    // --- Internal -------------------------------

    private fun typeNameOf(ref: TypeRef.Scalar) = typeNameOf(ref.type)

    private fun typeNameOf(ref: TypeRef.Any) = KotlinTypes.any

    private fun typeNameOf(ref: TypeRef.List) = KotlinTypes.list.parameterizedBy(typeNameOf(ref.type))

    private fun typeNameOf(ref: TypeRef.Set) = KotlinTypes.set.parameterizedBy(typeNameOf(ref.type))

    private fun typeNameOf(ref: TypeRef.Map) = KotlinTypes.map.parameterizedBy(
        typeNameOf(ref.keyType),
        typeNameOf(ref.valType)
    )

    /**
     * Returns the subset of type definitions for the given domain
     */
    fun membersOf(domain: Domain): List<TypeDef> {
        val refs = domain.members.toSet()
        return universe.types.filter { it.ref in refs }
    }

    fun typeNameOf(domain: Domain): ClassName = domains.computeIfAbsent(domain) {
        ClassName(
            packageName = "$rootPackage.${domain.id}".toLowerCase(),
            simpleNames = listOf(domain.id.toPascalCase())
        )
    }

    /**
     * Determine the appropriate mapping method from a JsonNode to the Kotlin value; this could certainly be improved.
     *
     * Lives here for now because of the special treatment primitives
     */
    fun valueMapping(ref: TypeRef, v: String): String = when (ref) {
        is TypeRef.Any -> v // TODO!
        is TypeRef.List -> "$v.map { n -> ${valueMapping(ref.type, "n")} }"
        is TypeRef.Map -> TODO("determine if a map key can be anything other than strings")
        is TypeRef.Set -> "$v.map { n -> ${valueMapping(ref.type, "n")} }.toSet()"
        is TypeRef.Scalar -> when (ref.type) {
            ScalarType.BOOL -> "$v.asBoolean()"
            ScalarType.INT -> "$v.asInt()"
            ScalarType.LONG -> "$v.asLong()"
            ScalarType.FLOAT -> "$v.floatValue()"
            ScalarType.DOUBLE -> "$v.asDouble()"
            ScalarType.BYTES -> "$v.binaryValue()"
            ScalarType.STRING -> "$v.asText()"
        }
        is TypeRef.Path -> {
            when (def(ref)) {
                is TypeDef.Enum -> "${clazz(ref).canonicalName}.valueOf($v.asText().uppercase())"
                is TypeDef.Product,
                is TypeDef.Sum -> "_${camel(ref)}($v)"
            }
        }
    }
}
