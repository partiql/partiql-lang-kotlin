package org.partiql.sprout.model

import net.pearx.kasechange.toPascalCase

/**
 * Top-level model of a Sprout grammar
 */
class Universe(
    val id: String,
    val types: List<TypeDef>,
    val domains: List<Domain> = emptyList()
) {

    fun forEachType(action: (TypeDef) -> Unit) {
        fun List<TypeDef>.applyToAll() {
            forEach {
                action(it)
                it.children.applyToAll()
            }
        }
        types.applyToAll()
    }
}

/**
 * Named subset of the universe
 */
class Domain(
    val id: String,
    val members: List<TypeRef>
)

/**
 * Definition of some type
 */
sealed class TypeDef(
    val ref: TypeRef.Path,
) {

    open val children: List<TypeDef> = emptyList()

    /**
     * TypeDef.Sum represents a list of type variants
     */
    class Sum(ref: TypeRef.Path, val variants: List<TypeDef>) : TypeDef(ref) {

        override val children = variants

        override fun toString() = "sum::$ref"
    }

    /**
     * TypeDef.Product represents a structure of name/value pairs
     */
    class Product(ref: TypeRef.Path, val props: List<TypeProp>) : TypeDef(ref) {

        override val children: List<TypeDef> = props.filterIsInstance<TypeProp.Enum>().map { it.def }

        override fun toString() = "product::$ref(${props.joinToString()})"
    }

    /**
     * TypeDef.Enum represents a set of named constants — explicitly not modelled with [Sum]
     *
     * Enums routinely required special treatment.. :upside_down_face:
     */
    class Enum(ref: TypeRef.Path, val values: List<String>) : TypeDef(ref) {

        override fun toString() = "enum::$ref"
    }
}

/**
 * Reference to some type
 */
sealed class TypeRef(
    val id: String,
    val nullable: Boolean,
) {

    override fun equals(other: kotlin.Any?) =
        if (other !is TypeRef) false else (id == other.id && nullable == other.nullable)

    override fun hashCode() = id.hashCode()

    override fun toString() = if (nullable) "$id?" else id

    class Scalar(
        val type: ScalarType,
        nullable: Boolean = false,
    ) : TypeRef(
        id = type.toString().toLowerCase(),
        nullable = nullable,
    )

    class Any(nullable: Boolean = false) : TypeRef(
        "any",
        nullable = nullable
    )

    class List(
        val type: TypeRef,
        nullable: Boolean = false,
    ) : TypeRef(
        id = "list<$type>",
        nullable = nullable,
    )

    class Set(
        val type: TypeRef,
        nullable: Boolean = false,
    ) : TypeRef(
        id = "set<$type>",
        nullable = nullable,
    )

    /**
     * TODO determine keyType restrictions following protobuf learnings
     * https://developers.google.com/protocol-buffers/docs/proto3#maps
     */
    class Map(
        val keyType: TypeRef,
        val valType: TypeRef,
        nullable: Boolean = false,
    ) : TypeRef(
        id = "map<$keyType, $valType>",
        nullable = nullable,
    )

    class Path(
        nullable: Boolean = false,
        vararg ids: String,
    ) : TypeRef(
        id = ids.joinToString("."),
        nullable = nullable,
    ) {
        val path = ids.asList()
        val name = ids.last().toPascalCase()
    }
}

/**
 * Product type property
 */
sealed class TypeProp(
    val name: String,
    val ref: TypeRef,
) {

    override fun toString() = "$name: $ref"

    class Ref(name: String, ref: TypeRef) : TypeProp(name, ref)

    class Enum(name: String, val def: TypeDef.Enum) : TypeProp(name, def.ref)
}

/**
 * References
 *  - https://docs.oracle.com/cd/E26161_02/html/GettingStartedGuide/avroschemas.html#avro-primitivedatatypes
 *  - https://developers.google.com/protocol-buffers/docs/proto3#scalar
 *  - https://en.wikipedia.org/wiki/JSON#Data_types (scalars only)
 *  - https://amzn.github.io/ion-docs/docs/spec.html (scalars only)
 */
enum class ScalarType {
    BOOL, // binary value
    INT, // int32
    LONG, // int64
    FLOAT, // IEEE 754 (32 bit)
    DOUBLE, // IEEE 754 (64 bit)
    BYTES, // Array of unsigned bytes
    STRING // Unicode char sequence
}
