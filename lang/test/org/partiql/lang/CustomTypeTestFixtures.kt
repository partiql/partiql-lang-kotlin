package org.partiql.lang

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.numberValue
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.CustomTypeFunction
import org.partiql.lang.types.DecimalType
import org.partiql.lang.types.IntType
import org.partiql.lang.types.NumberConstraint
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StringType
import org.partiql.lang.types.TypeParameter
import org.partiql.lang.util.compareTo

/**
 * The types in this file are a set of bare-bones fixtures to assure proper behavior with the *interface* to custom types.
 *
 * They should assure proper structure, but the individual validation functions are contrived for ease of testing.
 * For example, the `numberValue() < 100L` found in [esFloatType] simulates a type that overflows at `100` for the
 * purposes of testing custom type integration; It is not a spec for the `ES_FLOAT` type, but rather a testing contrivance.
 */

// Boolean
private val booleanType = object: CustomTypeFunction {
    override fun constructStaticType(args: List<TypeParameter>): StaticType = StaticType.BOOL
}

// String
private val stringType = object: CustomTypeFunction {
    override fun constructStaticType(args: List<TypeParameter>): StaticType = StaticType.STRING
}

// ES_INT
private val esIntType = object: CustomTypeFunction {
    override fun constructStaticType(args: List<TypeParameter>): StaticType =
        IntType(IntType.IntRangeConstraint.LONG)
}

// ES_FLOAT
private val esFloatType = object: CustomTypeFunction {
    override val validateExprValue =  { exprValue: ExprValue ->
        exprValue.numberValue() < 100L
    }

    override fun constructStaticType(args: List<TypeParameter>): StaticType = StaticType.FLOAT
}

fun anyOfType(vararg types: StaticType) = AnyOfType(setOf(*types))

/** Emulate the recursive validation function for ES_ANY. */
private fun esValidateAny(value: ExprValue): Boolean =
    when (value.type){
        ExprValueType.FLOAT -> esFloatType.validateExprValue?.invoke(value) ?: true
        ExprValueType.MISSING,
        ExprValueType.NULL,
        ExprValueType.BOOL,
        ExprValueType.INT,
        ExprValueType.STRING -> true
        ExprValueType.LIST,
        ExprValueType.STRUCT -> value.all { esValidateAny(it) }
        else -> false
    }

// ES_ANY - this is an abbreviated one for testing purposes
internal val esAnyType = object: CustomTypeFunction {
    override val validateExprValue: ((ExprValue) -> Boolean) = ::esValidateAny

    override fun constructStaticType(args: List<TypeParameter>): StaticType = anyOfType(
        StaticType.NULL,
        StaticType.BOOL,
        esIntType.constructStaticType(),
        esFloatType.constructStaticType(),
        StaticType.STRING,
        StaticType.LIST,
        StaticType.STRUCT
    )
}
// RS_INTEGER
private val rsIntegerType = object: CustomTypeFunction {
    override fun constructStaticType(args: List<TypeParameter>): StaticType =
        IntType(IntType.IntRangeConstraint.INT4)
}

// RS_BIGINT
private val rsBigintType = object: CustomTypeFunction {
    override fun constructStaticType(args: List<TypeParameter>): StaticType =
        IntType(IntType.IntRangeConstraint.LONG)
}

// RS_VARCHAR_PARAMETERIZED
private val rsVarcharType = object: CustomTypeFunction {
    override val arity: IntRange = 1..1
    override fun constructStaticType(args: List<TypeParameter>): StaticType{
        val byteLength = args[0].intValue()
        return StringType(StringType.StringLengthConstraint.ByteLengthConstrained(NumberConstraint.UpTo(byteLength)))
    }
}

// RS_VARCHAR_MAX
private val rsStringType = object: CustomTypeFunction {
    override fun constructStaticType(args: List<TypeParameter>): StaticType =
        StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(10)))
}

// RS_REAL
private val rsRealType = object: CustomTypeFunction {
    override val validateExprValue = { exprValue: ExprValue ->
        exprValue.numberValue() < 11L
    }

    override fun constructStaticType(args: List<TypeParameter>): StaticType = StaticType.FLOAT
}

// RS_DOUBLE_PRECISION
private val rsDoublePrecisionType = object: CustomTypeFunction {
    override val validateExprValue = { exprValue: ExprValue ->
        exprValue.numberValue() < 100L
    }

    override fun constructStaticType(args: List<TypeParameter>): StaticType = StaticType.FLOAT
}
// SPARK_SHORT
private val sparkShortType = object: CustomTypeFunction {
    override fun constructStaticType(args: List<TypeParameter>): StaticType =
        IntType(IntType.IntRangeConstraint.SHORT)
}


// SPARK_INTEGER
private val sparkIntegerType = object: CustomTypeFunction {
    override fun constructStaticType(args: List<TypeParameter>): StaticType =
        IntType(IntType.IntRangeConstraint.INT4)
}


// SPARK_LONG
private val sparkLongType = object: CustomTypeFunction {
    override fun constructStaticType(args: List<TypeParameter>): StaticType =
        IntType(IntType.IntRangeConstraint.LONG)
}

// SPARK_FLOAT
private val sparkFloatType = object: CustomTypeFunction {
    override val validateExprValue = { exprValue: ExprValue ->
        exprValue.numberValue() < 11L
    }

    override fun constructStaticType(args: List<TypeParameter>): StaticType = StaticType.FLOAT
}
// SPARK_DOUBLE
private val sparkDoubleType = object: CustomTypeFunction {
    override val validateExprValue = { exprValue: ExprValue ->
        exprValue.numberValue() < 100L
    }

    override fun constructStaticType(args: List<TypeParameter>): StaticType = StaticType.FLOAT
}

// TOY VARCHAR
private val toyVarcharType = object: CustomTypeFunction {
    override val arity: IntRange = 1..1
    override fun constructStaticType(args: List<TypeParameter>): StaticType {
        val stringLength = args[0].intValue()
        return StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(stringLength)))
    }
}

// TOY DECIMAL
private val toyDecimalType = object: CustomTypeFunction {
    override val arity: IntRange = 0..2
    override fun constructStaticType(args: List<TypeParameter>): StaticType {
        return when (args.size) {
            0 -> StaticType.DECIMAL
            1 -> {
                val precision = args[0].intValue()
                DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(precision))
            }
            else -> {
                val precision = args[0].intValue()
                val scale = args[1].intValue()
                DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(precision, scale))
            }
        }
    }
}

/**
 * These are the custom types including SPARK, ES and RS types used for testing.
 */
internal val CUSTOM_TEST_TYPES_MAP = mapOf(
    /* ElasticSearch */
    listOf("ES_INTEGER") to esIntType,
    listOf("ES_BOOLEAN") to booleanType,
    listOf("ES_FLOAT") to esFloatType,
    listOf("ES_TEXT") to stringType,
    listOf("ES_ANY") to esAnyType,
    /* Redshift */
    listOf("RS_INTEGER") to rsIntegerType,
    listOf("RS_BIGINT") to rsBigintType,
    listOf("RS_BOOLEAN") to booleanType,
    listOf("RS_VARCHAR") to rsVarcharType,
    listOf("RS_VARCHAR_MAX") to rsStringType,
    listOf("RS_REAL", "RS_FLOAT4") to rsRealType,
    listOf("RS_DOUBLE_PRECISION", "RS_FLOAT", "RS_FLOAT8") to rsDoublePrecisionType,
    /* Spark */
    listOf("SPARK_SHORT") to sparkShortType,
    listOf("SPARK_INTEGER") to sparkIntegerType,
    listOf("SPARK_LONG") to sparkLongType,
    listOf("SPARK_FLOAT") to sparkFloatType,
    listOf("SPARK_DOUBLE") to sparkDoubleType,
    listOf("SPARK_BOOLEAN") to booleanType,
    /* Toy types */
    listOf("TOY_VARCHAR") to toyVarcharType,
    listOf("TOY_DECIMAL") to toyDecimalType
).flatMap { (aliases, type) ->
    aliases.map { alias ->
        Pair(alias.toLowerCase(), type)
    }
}.toMap()