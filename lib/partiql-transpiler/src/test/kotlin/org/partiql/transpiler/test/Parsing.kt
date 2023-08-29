package org.partiql.transpiler.test

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ListElement
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import org.partiql.types.BagType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StructType

// Use some generated serde eventually

public inline fun <reified T : IonElement> StructElement.getAngry(name: String): T {
    val f = getOptional(name) ?: error("Expected field `$name`")
    if (f !is T) {
        error("Expected field `name` to be of type ${T::class.simpleName}")
    }
    return f
}

/**
 * Parses an IonElement to a StaticType.
 *
 * The format used is effectively Avro JSON, but with PartiQL type names.
 */
public fun IonElement.toStaticType(): StaticType {
    return when (this) {
        is StringElement -> this.toStaticType()
        is ListElement -> this.toStaticType()
        is StructElement -> this.toStaticType()
        else -> error("Invalid element, expected string, list, or struct")
    }
}

// Atomic type
public fun StringElement.toStaticType(): StaticType = when (textValue) {
    "any" -> StaticType.ANY
    "bool" -> StaticType.BOOL
    "int8" -> error("`int8` is currently not supported")
    "int16" -> StaticType.INT2
    "int32" -> StaticType.INT4
    "int64" -> StaticType.INT8
    "int" -> StaticType.INT
    "decimal" -> StaticType.DECIMAL
    "float32" -> StaticType.FLOAT
    "float64" -> StaticType.FLOAT
    "char" -> StaticType.CHAR
    "string" -> StaticType.STRING
    "symbol" -> StaticType.SYMBOL
    "binary" -> error("`binary` is currently not supported")
    "byte" -> error("`byte` is currently not supported")
    "blob" -> StaticType.BLOB
    "clob" -> StaticType.CLOB
    "date" -> StaticType.DATE
    "time" -> StaticType.TIME
    "timestamp" -> StaticType.TIMESTAMP
    "interval" -> error("`interval` is currently not supported")
    "bag" -> error("`bag` is not an atomic type")
    "list" -> error("`bag` is not an atomic type")
    "sexp" -> error("`bag` is not an atomic type")
    "struct" -> error("`bag` is not an atomic type")
    "null" -> StaticType.NULL
    "missing" -> StaticType.MISSING
    else -> error("Invalid type `$textValue`")
}

// Union type
public fun ListElement.toStaticType(): StaticType {
    val types = values.map { it.toStaticType() }.toSet()
    return StaticType.unionOf(types)
}

// Complex type
public fun StructElement.toStaticType(): StaticType {
    val type = getAngry<StringElement>("type").textValue
    return when (type) {
        "bag" -> toBagType()
        "list" -> toListType()
        "sexp" -> toSexpType()
        "struct" -> toStructType()
        else -> error("Unknown complex type $type")
    }
}

public fun StructElement.toBagType(): StaticType {
    val items = getAngry<IonElement>("items").toStaticType()
    return BagType(items)
}

public fun StructElement.toListType(): StaticType {
    val items = getAngry<IonElement>("items").toStaticType()
    return ListType(items)
}

public fun StructElement.toSexpType(): StaticType {
    val items = getAngry<IonElement>("items").toStaticType()
    return SexpType(items)
}

public fun StructElement.toStructType(): StaticType {
    val fieldsE = getAngry<ListElement>("fields")
    val fields = fieldsE.values.map {
        assert(it is StructElement) { "field definition must be as struct" }
        it as StructElement
        val name = it.getAngry<StringElement>("name").textValue
        val type = it.getAngry<IonElement>("type").toStaticType()
        StructType.Field(name, type)
    }
    return StructType(fields)
}
