package org.partiql.lang

import OTS.IMP.org.partiql.ots.legacy.types.CharType
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.numberValue
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.CustomType
import org.partiql.lang.types.StaticScalarType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.TypedOpParameter
import org.partiql.lang.util.compareTo

/**
 * The types in this file are a set of bare-bones fixtures to assure proper behavior with the *interface* to custom types.
 *
 * They should assure proper structure, but the individual validation functions are contrived for ease of testing.
 * For example, the `numberValue() < 100L` found in [esFloatParameter] simulates a type that overflows at `100` for the
 * purposes of testing custom type integration; It is not a spec for the `ES_FLOAT` type, but rather a testing contrivance.
 */

// Boolean
private val booleanParameter = TypedOpParameter(StaticType.BOOL)

// String
private val stringParameter = TypedOpParameter(StaticType.STRING)

// ES_INT
val esIntParameter = TypedOpParameter(StaticType.INT8)

// ES_FLOAT
val esFloatParameter = TypedOpParameter(StaticType.FLOAT) {
    it.numberValue() < 100L
}

/** Emulate the recursive validation function for ES_ANY. */
private fun esValidateAny(value: ExprValue): Boolean =
    when (value.type) {
        ExprValueType.FLOAT -> esFloatParameter.validationThunk?.invoke(value) ?: true
        ExprValueType.MISSING,
        ExprValueType.NULL,
        ExprValueType.BOOL,
        ExprValueType.INT,
        ExprValueType.STRING -> true
        ExprValueType.LIST,
        ExprValueType.STRUCT -> value.all { esValidateAny(it) }
        else -> false
    }

fun anyOfType(vararg types: StaticType) = AnyOfType(setOf(*types))

// ES_ANY - this is an abbreviated one for testing purposes
val esAny = TypedOpParameter(
    anyOfType(
        StaticType.NULL,
        StaticType.BOOL,
        esIntParameter.staticType,
        esFloatParameter.staticType,
        StaticType.STRING,
        StaticType.LIST,
        StaticType.STRUCT
    ),
    ::esValidateAny
)

// RS_INTEGER
private val rsIntegerPrecisionParameter = TypedOpParameter(StaticType.INT4)

// RS_BIGINT
private val rsBigintPrecisionParameter = TypedOpParameter(StaticType.INT8)

// RS_VARCHAR_MAX
private val rsStringParameter = TypedOpParameter(StaticScalarType(CharType, listOf(10)))

// RS_REAL
private val rsRealParameter = TypedOpParameter(StaticType.FLOAT) {
    it.numberValue() < 11L
}

// RS_DOUBLE_PRECISION
private val rsDoublePrecisionParameter = TypedOpParameter(StaticType.FLOAT) {
    it.numberValue() < 100L
}

// SPARK_SHORT
private val sparkShortPrecisionParameter = TypedOpParameter(StaticType.INT2)

// SPARK_INTEGER
private val sparkIntegerPrecisionParameter = TypedOpParameter(StaticType.INT4)

// SPARK_LONG
private val sparkLongPrecisionParameter = TypedOpParameter(StaticType.INT8)

// SPARK_FLOAT
private val sparkFloatPrecisionParameter = TypedOpParameter(StaticType.FLOAT) {
    it.numberValue() < 11L
}

// SPARK_DOUBLE
private val sparkDoublePrecisionParameter = TypedOpParameter(StaticType.FLOAT) {
    it.numberValue() < 100L
}

/**
 * These are the custom types including SPARK, ES and RS types used for testing.
 */
internal val CUSTOM_TEST_TYPES = listOf(
    /* ElasticSearch */
    CustomType(
        "ES_INTEGER",
        typedOpParameter = esIntParameter
    ),
    CustomType(
        "ES_BOOLEAN",
        typedOpParameter = booleanParameter
    ),
    CustomType(
        "ES_FLOAT",
        typedOpParameter = esFloatParameter
    ),
    CustomType(
        "ES_TEXT",
        typedOpParameter = stringParameter
    ),
    CustomType(
        "ES_ANY",
        typedOpParameter = esAny
    ),
    /* Redshift */
    CustomType(
        "RS_INTEGER",
        typedOpParameter = rsIntegerPrecisionParameter
    ),
    CustomType(
        "RS_BIGINT",
        typedOpParameter = rsBigintPrecisionParameter
    ),
    CustomType(
        "RS_BOOLEAN",
        typedOpParameter = booleanParameter
    ),
    CustomType(
        "RS_VARCHAR_MAX",
        typedOpParameter = rsStringParameter
    ),
    CustomType(
        "RS_REAL",
        typedOpParameter = rsRealParameter,
        aliases = listOf("RS_FLOAT4")
    ),
    CustomType(
        "RS_DOUBLE_PRECISION",
        typedOpParameter = rsDoublePrecisionParameter,
        aliases = listOf("RS_FLOAT", "RS_FLOAT8")
    ),
    /* Spark */
    CustomType(
        "SPARK_SHORT",
        typedOpParameter = sparkShortPrecisionParameter
    ),
    CustomType(
        "SPARK_INTEGER",
        typedOpParameter = sparkIntegerPrecisionParameter
    ),
    CustomType(
        "SPARK_LONG",
        typedOpParameter = sparkLongPrecisionParameter
    ),
    CustomType(
        "SPARK_FLOAT",
        typedOpParameter = sparkFloatPrecisionParameter
    ),
    CustomType(
        "SPARK_DOUBLE",
        typedOpParameter = sparkDoublePrecisionParameter
    ),
    CustomType(
        "SPARK_BOOLEAN",
        typedOpParameter = booleanParameter
    )
)
