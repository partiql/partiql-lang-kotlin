package org.partiql.plugins.local

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ListElement
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.SymbolElement
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.ionSymbol
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.GraphType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.types.TupleConstraint

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
    "list" -> error("`list` is not an atomic type")
    "sexp" -> error("`sexp` is not an atomic type")
    "struct" -> error("`struct` is not an atomic type")
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
    // Constraints
    var contentClosed = false
    val constraintsE = getOptional("constraints") ?: ionListOf()
    val constraints = (constraintsE as ListElement).values.map {
        assert(it is SymbolElement)
        it as SymbolElement
        when (it.textValue) {
            "ordered" -> TupleConstraint.Ordered
            "unique" -> TupleConstraint.UniqueAttrs(true)
            "closed" -> {
                contentClosed = true
                TupleConstraint.Open(false)
            }
            else -> error("unknown tuple constraint `${it.textValue}`")
        }
    }.toSet()
    // Fields
    val fieldsE = getAngry<ListElement>("fields")
    val fields = fieldsE.values.map {
        assert(it is StructElement) { "field definition must be as struct" }
        it as StructElement
        val name = it.getAngry<StringElement>("name").textValue
        val type = it.getAngry<IonElement>("type").toStaticType()
        StructType.Field(name, type)
    }
    return StructType(fields, contentClosed, constraints = constraints)
}

public fun StaticType.toIon(): IonElement = when (this) {
    is AnyOfType -> this.toIon()
    is AnyType -> ionString("any")
    is BlobType -> ionString("blob")
    is BoolType -> ionString("bool")
    is ClobType -> ionString("clob")
    is BagType -> this.toIon()
    is ListType -> this.toIon()
    is SexpType -> this.toIon()
    is DateType -> ionString("date")
    is DecimalType -> ionString("decimal")
    is FloatType -> ionString("float64")
    is GraphType -> ionString("graph")
    is IntType -> when (this.rangeConstraint) {
        IntType.IntRangeConstraint.SHORT -> ionString("int16")
        IntType.IntRangeConstraint.INT4 -> ionString("int32")
        IntType.IntRangeConstraint.LONG -> ionString("int64")
        IntType.IntRangeConstraint.UNCONSTRAINED -> ionString("int")
    }
    MissingType -> ionString("missing")
    is NullType -> ionString("null")
    is StringType -> ionString("string") // TODO char
    is StructType -> this.toIon()
    is SymbolType -> ionString("symbol")
    is TimeType -> ionString("time")
    is TimestampType -> ionString("timestamp")
}

private fun AnyOfType.toIon(): IonElement {
    // create some predictable ordering
    val sorted = this.types.sortedWith { t1, t2 -> t1::class.java.simpleName.compareTo(t2::class.java.simpleName) }
    val elements = sorted.map { it.toIon() }
    return ionListOf(elements)
}

private fun BagType.toIon(): IonElement = ionStructOf(
    "type" to ionString("bag"),
    "items" to elementType.toIon()
)

private fun ListType.toIon(): IonElement = ionStructOf(
    "type" to ionString("list"),
    "items" to elementType.toIon()
)

private fun SexpType.toIon(): IonElement = ionStructOf(
    "type" to ionString("sexp"),
    "items" to elementType.toIon()
)

private fun StructType.toIon(): IonElement {
    val constraintSymbols = mutableListOf<SymbolElement>()
    for (constraint in constraints) {
        val c = when (constraint) {
            is TupleConstraint.Open -> if (constraint.value) null else ionSymbol("closed")
            TupleConstraint.Ordered -> ionSymbol("ordered")
            is TupleConstraint.UniqueAttrs -> ionSymbol("unique")
        }
        if (c != null) constraintSymbols.add(c)
    }
    val fieldTypes = this.fields.map {
        ionStructOf(
            "name" to ionString(it.key),
            "type" to it.value.toIon(),
        )
    }
    return ionStructOf(
        "type" to ionString("struct"),
        "fields" to ionListOf(fieldTypes),
        "constraints" to ionListOf(constraintSymbols),
    )
}
