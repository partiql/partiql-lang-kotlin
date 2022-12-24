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

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import net.pearx.kasechange.toCamelCase
import net.pearx.kasechange.toPascalCase
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
     * Map all type references back to their definitions. Use `id` as to not include `nullable` in the key `equals`
     */
    private val defs: Map<String, TypeDef> by lazy {
        val d = mutableMapOf<String, TypeDef>()
        universe.forEachType { d[it.ref.id] = it }
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

    fun def(ref: TypeRef.Path): TypeDef = defs[ref.id] ?: error("no definition found for type `$ref`")

    /**
     * Computes a type name for the given [TypeRef]
     */
    fun typeNameOf(ref: TypeRef, mutable: Boolean = false): TypeName = when (ref) {
        is TypeRef.Scalar -> typeNameOf(ref)
        is TypeRef.Path -> clazz(ref)
        is TypeRef.List -> typeNameOf(ref, mutable)
        is TypeRef.Set -> typeNameOf(ref, mutable)
        is TypeRef.Map -> typeNameOf(ref, mutable)
        is TypeRef.Import -> ClassName(ref.namespace, ref.path)
    }.copy(ref.nullable)

    // --- Internal -------------------------------

    private fun typeNameOf(ref: TypeRef.Scalar) = when (ref.type) {
        ScalarType.BOOL -> BOOLEAN
        ScalarType.INT -> INT
        ScalarType.LONG -> LONG
        ScalarType.FLOAT -> FLOAT
        ScalarType.DOUBLE -> DOUBLE
        ScalarType.BYTES -> BYTE_ARRAY
        ScalarType.STRING -> STRING
    }

    private fun typeNameOf(ref: TypeRef.List, mutable: Boolean = false): TypeName {
        val t = typeNameOf(ref.type, mutable)
        val list = if (mutable) MUTABLE_LIST else LIST
        return list.parameterizedBy(t)
    }

    private fun typeNameOf(ref: TypeRef.Set, mutable: Boolean = false): TypeName {
        val t = typeNameOf(ref.type, mutable)
        val set = if (mutable) MUTABLE_SET else SET
        return set.parameterizedBy(t)
    }

    private fun typeNameOf(ref: TypeRef.Map, mutable: Boolean = false): TypeName {
        val kt = typeNameOf(ref.keyType)
        val vt = typeNameOf(ref.valType)
        val map = if (mutable) MUTABLE_MAP else MAP
        return map.parameterizedBy(kt, vt)
    }

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
        is TypeRef.List -> "$v.map { n -> ${valueMapping(ref.type, "n")} }"
        is TypeRef.Map -> TODO("Jackson databind for maps")
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
        // Make this invoke the default deserializer and see how far the gets us
        is TypeRef.Import -> TODO("Jackson databind is currently not supported for imported types")
    }
}
