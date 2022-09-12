package org.partiql.lang.ast.passes.inference

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.TypeParameters
import org.partiql.lang.ots_work.plugins.standard.types.BlobType
import org.partiql.lang.ots_work.interfaces.BoolType
import org.partiql.lang.ots_work.plugins.standard.types.CharType
import org.partiql.lang.ots_work.plugins.standard.types.ClobType
import org.partiql.lang.ots_work.plugins.standard.types.DecimalType
import org.partiql.lang.ots_work.plugins.standard.types.FloatType
import org.partiql.lang.ots_work.plugins.standard.types.Int2Type
import org.partiql.lang.ots_work.plugins.standard.types.Int4Type
import org.partiql.lang.ots_work.plugins.standard.types.Int8Type
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.types.TimeStampType
import org.partiql.lang.ots_work.plugins.standard.types.VarcharType
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.AnyType
import org.partiql.lang.types.CollectionType
import org.partiql.lang.types.MissingType
import org.partiql.lang.types.NullType
import org.partiql.lang.types.SingleType
import org.partiql.lang.types.StaticScalarType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StructType

internal val intTypesPrecedence = listOf(Int2Type, Int4Type, Int8Type, IntType)

internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)
internal fun StaticType.isText(): Boolean = this is StaticScalarType && (scalarType in listOf(SymbolType, StringType, VarcharType, CharType))
internal fun StaticType.isNumeric(): Boolean = this is StaticScalarType && (scalarType in listOf(Int2Type, Int4Type, Int8Type, IntType, FloatType, DecimalType))
internal fun StaticType.isLob(): Boolean = this is StaticScalarType && (scalarType === BlobType || scalarType === ClobType)
internal fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)

/**
 * Returns the maximum number of digits a decimal can hold after reserving digits for scale
 *
 * For example: The maximum value a DECIMAL(5,2) can represent is 999.99, therefore the maximum
 *  number of digits it can hold is 3 (i.e up to 999).
 */
// TODO: What's PartiQL's max allowed precision?
private fun DecimalType.maxDigits(parameters: TypeParameters): Int = (parameters[0] ?: Int.MAX_VALUE) - parameters[1]!!

/**
 * Casts [this] static to the given target type.
 *
 * This replicates the behavior of its runtime equivalent [ExprValue.cast].
 * @see [ExprValue.cast] for documentation.
 */
internal fun StaticType.cast(targetType: StaticType): StaticType {
    when (targetType) {
        is AnyOfType -> {
            // TODO we should do more sophisticated inference based on the source like we do for single types
            val includesNull = this.allTypes.any { it.isNullable() }
            return when {
                includesNull -> StaticType.unionOf(StaticType.MISSING, StaticType.NULL, targetType)
                else -> StaticType.unionOf(StaticType.MISSING, targetType)
            }
        }
        is AnyType -> {
            // casting to `ANY` is the identity
            return this
        }
        is SingleType -> {}
    }

    // union source types, recursively process them
    when (this) {
        is AnyType -> return AnyOfType(this.toAnyOfType().types.map { it.cast(targetType) }.toSet()).flatten()
        is AnyOfType -> return when (val flattened = this.flatten()) {
            is SingleType, is AnyType -> flattened.cast(targetType)
            is AnyOfType -> AnyOfType(flattened.types.map { it.cast(targetType) }.toSet()).flatten()
        }
    }

    // single source type
    when {
        this.isNullOrMissing() && targetType == StaticType.MISSING -> return StaticType.MISSING
        this.isNullOrMissing() && targetType == StaticType.NULL -> return StaticType.NULL
        // `MISSING` and `NULL` always convert to themselves no matter the target type
        this.isNullOrMissing() -> return this
        else -> {
            when (targetType) {
                is StaticScalarType -> when (targetType.scalarType) {
                    is VarcharType,
                    is CharType,
                    is StringType,
                    is SymbolType -> when {
                        this.isNumeric() || this.isText() -> return targetType
                        this is StaticScalarType && (scalarType in listOf(BoolType, TimeStampType)) -> return targetType
                    }
                    is Int2Type,
                    is Int4Type,
                    is Int8Type,
                    is IntType -> when {
                        this is StaticScalarType -> when (scalarType) {
                            is Int2Type,
                            is Int4Type,
                            is Int8Type,
                            is IntType -> {
                                when (targetType.scalarType) {
                                    is Int2Type -> when (scalarType) {
                                        is Int2Type -> return targetType
                                        is Int4Type,
                                        is Int8Type,
                                        is IntType -> return StaticType.unionOf(StaticType.MISSING, targetType)
                                    }
                                    is Int4Type -> when (scalarType) {
                                        is Int2Type,
                                        is Int4Type -> return targetType
                                        is Int8Type,
                                        is IntType -> return StaticType.unionOf(StaticType.MISSING, targetType)
                                    }
                                    is Int8Type -> when (scalarType) {
                                        is Int2Type,
                                        is Int4Type,
                                        is Int8Type -> return targetType
                                        is IntType -> return StaticType.unionOf(StaticType.MISSING, targetType)
                                    }
                                    is IntType -> return targetType
                                }
                            }
                            is BoolType -> return targetType
                            is FloatType -> return when (targetType.scalarType) {
                                IntType -> targetType
                                else -> StaticType.unionOf(StaticType.MISSING, targetType)
                            }
                            is DecimalType -> return when (targetType.scalarType) {
                                IntType -> targetType
                                Int2Type,
                                Int4Type,
                                Int8Type -> return when (parameters[0]) {
                                    null -> StaticType.unionOf(StaticType.MISSING, targetType)
                                    else -> {
                                        // Max value of SMALLINT is 32767.
                                        // Conversion to SMALLINT will work as long as the decimal holds up 4 to digits. There is a chance of overflow beyond that.
                                        // Similarly -
                                        //   Max value of INT4 is 2,147,483,647
                                        //   Max value of BIGINT is 9,223,372,036,854,775,807 for BIGINT
                                        // TODO: Move these magic numbers out.
                                        val maxDigitsWithoutPrecisionLoss = when (targetType.scalarType) {
                                            Int2Type -> 4
                                            Int4Type -> 9
                                            Int8Type -> 18
                                            IntType -> error("Un-constrained is handled above. This code shouldn't be reached.")
                                            else -> error("Unreachable code")
                                        }

                                        if (scalarType.maxDigits(parameters) > maxDigitsWithoutPrecisionLoss) {
                                            StaticType.unionOf(StaticType.MISSING, targetType)
                                        } else {
                                            targetType
                                        }
                                    }
                                }
                                else -> error("Unreachable code")
                            }
                            is SymbolType,
                            is StringType,
                            is CharType,
                            is VarcharType -> return StaticType.unionOf(targetType, StaticType.MISSING)
                        }
                    }
                    is BoolType -> when {
                        this is StaticScalarType && scalarType === BoolType || this.isNumeric() || this.isText() -> return targetType
                    }
                    is FloatType -> when {
                        this is StaticScalarType && scalarType === BoolType -> return targetType
                        // Conversion to float will always succeed for numeric types
                        this.isNumeric() -> return targetType
                        this.isText() -> return StaticType.unionOf(targetType, StaticType.MISSING)
                    }
                    is DecimalType -> when {
                        this is StaticScalarType -> when (scalarType) {
                            is DecimalType ->
                                return if (targetType.scalarType.maxDigits(targetType.parameters) >= scalarType.maxDigits(parameters)) {
                                    targetType
                                } else {
                                    StaticType.unionOf(targetType, StaticType.MISSING)
                                }
                            is Int2Type,
                            is Int4Type,
                            is Int8Type,
                            is IntType -> return when (targetType.parameters[0]) {
                                null -> targetType
                                else -> when (scalarType) {
                                    is IntType -> StaticType.unionOf(StaticType.MISSING, targetType)
                                    is Int2Type ->
                                        // TODO: Move the magic numbers out
                                        // max smallint value 32,767, so the decimal needs to be able to hold at least 5 digits
                                        if (targetType.scalarType.maxDigits(targetType.parameters) >= 5) {
                                            targetType
                                        } else {
                                            StaticType.unionOf(StaticType.MISSING, targetType)
                                        }
                                    is Int4Type ->
                                        // max int4 value 2,147,483,647 so the decimal needs to be able to hold at least 10 digits
                                        if (targetType.scalarType.maxDigits(targetType.parameters) >= 10) {
                                            targetType
                                        } else {
                                            StaticType.unionOf(StaticType.MISSING, targetType)
                                        }
                                    is Int8Type ->
                                        // max bigint value 9,223,372,036,854,775,807 so the decimal needs to be able to hold at least 19 digits
                                        if (targetType.scalarType.maxDigits(targetType.parameters) >= 19) {
                                            targetType
                                        } else {
                                            StaticType.unionOf(StaticType.MISSING, targetType)
                                        }
                                    else -> error("Unreachable code")
                                }
                            }
                            is BoolType,
                            is FloatType -> return targetType
                            is SymbolType,
                            is StringType,
                            is CharType,
                            is VarcharType -> return StaticType.unionOf(targetType, StaticType.MISSING)
                        }
                    }
                    is ClobType,
                    is BlobType -> when {
                        isLob() -> return targetType
                    }
                    is TimeStampType -> when {
                        this is StaticScalarType && scalarType === TimeStampType -> return targetType
                        this.isText() -> return StaticType.unionOf(targetType, StaticType.MISSING)
                    }
                }
                is CollectionType -> when (this) {
                    is CollectionType -> return targetType
                }
                is StructType -> when (this) {
                    is StructType -> return targetType
                }
            }
            // TODO:  support non-permissive mode(s) here by throwing an exception to indicate cast is not possible
            return StaticType.MISSING
        }
    }
}

/**
 * For [this] [StaticType], filters out [NullType] and [MissingType] from [AnyOfType]s. Otherwise, returns [this].
 */
internal fun StaticType.filterNullMissing(): StaticType =
    when (this) {
        is AnyOfType -> AnyOfType(this.types.filter { !it.isNullOrMissing() }.toSet()).flatten()
        else -> this
    }

/**
 * Returns a human-readable string of [argTypes]. Additionally, for each [AnyOfType], [NullType] and [MissingType] will
 * be filtered.
 */
internal fun stringWithoutNullMissing(argTypes: List<StaticType>): String =
    argTypes.joinToString { it.filterNullMissing().toString() }
