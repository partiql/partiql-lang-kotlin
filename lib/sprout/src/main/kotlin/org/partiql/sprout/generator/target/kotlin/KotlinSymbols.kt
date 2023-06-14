package org.partiql.sprout.generator.target.kotlin

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import net.pearx.kasechange.toCamelCase
import net.pearx.kasechange.toPascalCase
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
class KotlinSymbols private constructor(
    private val universe: Universe,
    private val options: KotlinOptions,
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

    /**
     * Id Property for interfaces and classes
     */
    val idProp = PropertySpec.builder("_id", String::class).addModifiers(KModifier.OVERRIDE).initializer("_id").build()

    /**
     * Id Parameter for internal constructors
     */
    val idPara = ParameterSpec.builder("_id", String::class).build()

    /**
     * Memoize converting a TypeRef.Path to a camel case identifier to be used as method/function names
     */
    private val camels: MutableMap<TypeRef.Path, String> = mutableMapOf()

    /**
     * Memoize converting a TypeRef.Path to a pascal case identifier
     */
    private val pascals: MutableMap<TypeRef.Path, String> = mutableMapOf()

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
        fun init(universe: Universe, options: KotlinOptions): KotlinSymbols = KotlinSymbols(universe, options)
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

    /**
     * Again, this causes naming conflicts for names like `a_b` and `a.b`
     */
    fun pascal(ref: TypeRef.Path): String = pascals.computeIfAbsent(ref) {
        ref.path.joinToString("_").toPascalCase()
    }

    // fun def(ref: TypeRef.Path): TypeDef = defs[ref.id] ?: error("no definition found for type `$ref`")
    fun def(ref: TypeRef.Path): TypeDef {
        val def = defs[ref.id]
        if (def == null) {
            error("no definition found for type `$ref`")
        } else {
            return def
        }
    }

    /**
     * Computes a type name for the given [TypeRef]
     */
    fun typeNameOf(ref: TypeRef, mutable: Boolean = false): TypeName = when (ref) {
        is TypeRef.Scalar -> typeNameOf(ref)
        is TypeRef.Path -> clazz(ref)
        is TypeRef.List -> typeNameOf(ref, mutable)
        is TypeRef.Set -> typeNameOf(ref, mutable)
        is TypeRef.Map -> typeNameOf(ref, mutable)
        is TypeRef.Import -> import(ref.symbol)
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
     * Determine the appropriate mapping method from a JsonNode to the Kotlin value; this could certainly be improved.
     * https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/JsonNode.html
     */
    fun valueMapping(ref: TypeRef, v: String): String = when (ref) {
        is TypeRef.List -> "$v.map { n -> ${valueMapping(ref.type, "n")} }"
        is TypeRef.Map -> "$v.fields().asSequence().associate { e -> e.key to ${valueMapping(ref.valType, "e.value")} }"
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
        // invoke the default deserializer
        is TypeRef.Import -> "ctxt.readValue($v, ${import(ref.symbol).canonicalName}.javaClass)"
    }

    /**
     * Parse the ClassLoader string to a KotlinPoet ClassName. Could improve error handling here..
     */
    private fun import(symbol: String): ClassName {
        if (!universe.imports.containsKey("kotlin")) {
            error("Missing `kotlin` target from imports")
        } else if (!universe.imports["kotlin"]!!.containsKey(symbol)) {
            error("Missing `kotlin` target for `$symbol` in imports")
        }
        val path = universe.imports["kotlin"]!![symbol]!!
        val i = path.lastIndexOf(".")
        val packageName = path.substring(0, i)
        val simpleNames = path.substring(i + 1).split("$")
        return ClassName(packageName, simpleNames)
    }
}
