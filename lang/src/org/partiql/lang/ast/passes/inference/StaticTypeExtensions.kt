package org.partiql.lang.ast.passes.inference

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots.plugins.standard.types.CompileTimeDecimalType
import org.partiql.lang.ots.plugins.standard.types.CompileTimeFloatType
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.AnyType
import org.partiql.lang.types.BlobType
import org.partiql.lang.types.BoolType
import org.partiql.lang.types.CharType
import org.partiql.lang.types.ClobType
import org.partiql.lang.types.CollectionType
import org.partiql.lang.types.Int2Type
import org.partiql.lang.types.Int4Type
import org.partiql.lang.types.Int8Type
import org.partiql.lang.types.IntType
import org.partiql.lang.types.MissingType
import org.partiql.lang.types.NullType
import org.partiql.lang.types.SingleType
import org.partiql.lang.types.StaticScalarType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StringType
import org.partiql.lang.types.StructType
import org.partiql.lang.types.SymbolType
import org.partiql.lang.types.TimestampType
import org.partiql.lang.types.VarcharType

internal val intTypesPrecedence = listOf(Int2Type::class, Int4Type::class, Int8Type::class, IntType::class)

internal fun StaticType.isDecimalType() = this is StaticScalarType && type is CompileTimeDecimalType
internal fun StaticType.isFloatType() = this is StaticScalarType && type is CompileTimeFloatType
internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)
internal fun StaticType.isText(): Boolean = (this is SymbolType || this is StringType || this is VarcharType || this is CharType)
internal fun StaticType.isNumeric(): Boolean = (this is Int2Type || this is Int4Type || this is Int8Type || this is IntType || isFloatType() || isDecimalType())
internal fun StaticType.isLob(): Boolean = (this is BlobType || this is ClobType)
internal fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)

internal fun SingleType.getLength() = when (this) {
    is CharType -> length
    is VarcharType -> length
    else -> error("Internal error: Only CHAR & VARCHAR type has length")
}

/**
 * Returns the maximum number of digits a decimal can hold after reserving digits for scale
 *
 * For example: The maximum value a DECIMAL(5,2) can represent is 999.99, therefore the maximum
 *  number of digits it can hold is 3 (i.e up to 999).
 */
// TODO: What's PartiQL's max allowed precision?
private fun CompileTimeDecimalType.maxDigits(): Int = (precision ?: Int.MAX_VALUE) - scale

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
                is BoolType -> when {
                    this is BoolType || this.isNumeric() || this.isText() -> return targetType
                }
                is Int2Type,
                is Int4Type,
                is Int8Type,
                is IntType -> when {
                    this is BoolType -> return targetType
                    this is Int2Type || this is Int4Type || this is Int8Type || this is IntType -> {
                        when (targetType) {
                            is Int2Type -> when (this) {
                                is Int2Type -> return targetType
                                is Int4Type,
                                is Int8Type,
                                is IntType -> return StaticType.unionOf(StaticType.MISSING, targetType)
                            }
                            is Int4Type -> when (this) {
                                is Int2Type,
                                is Int4Type -> return targetType
                                is Int8Type,
                                is IntType -> return StaticType.unionOf(StaticType.MISSING, targetType)
                            }
                            is Int8Type -> when (this) {
                                is Int2Type,
                                is Int4Type,
                                is Int8Type -> return targetType
                                is IntType -> return StaticType.unionOf(StaticType.MISSING, targetType)
                            }
                            is IntType -> return targetType
                        }
                    }
                    this is StaticScalarType -> when (type) {
                        is CompileTimeFloatType -> return when (targetType) {
                            is IntType -> targetType
                            else -> StaticType.unionOf(StaticType.MISSING, targetType)
                        }
                        is CompileTimeDecimalType -> return when (targetType) {
                            is IntType -> targetType
                            is Int2Type,
                            is Int4Type,
                            is Int8Type -> return when (type.precision) {
                                null -> StaticType.unionOf(StaticType.MISSING, targetType)
                                else -> {

                                    // Max value of SMALLINT is 32767.
                                    // Conversion to SMALLINT will work as long as the decimal holds up 4 to digits. There is a chance of overflow beyond that.
                                    // Similarly -
                                    //   Max value of INT4 is 2,147,483,647
                                    //   Max value of BIGINT is 9,223,372,036,854,775,807 for BIGINT
                                    // TODO: Move these magic numbers out.
                                    val maxDigitsWithoutPrecisionLoss = when (targetType) {
                                        is Int2Type -> 4
                                        is Int4Type -> 9
                                        is Int8Type -> 18
                                        is IntType -> error("Un-constrained is handled above. This code shouldn't be reached.")
                                        else -> error("Unreachable code")
                                    }

                                    if (type.maxDigits() > maxDigitsWithoutPrecisionLoss) {
                                        StaticType.unionOf(StaticType.MISSING, targetType)
                                    } else {
                                        targetType
                                    }
                                }
                            }
                            else -> error("Unreachable code")
                        }
                    }
                    this.isText() -> return StaticType.unionOf(targetType, StaticType.MISSING)
                }
                is StaticScalarType -> when (targetType.type) {
                    is CompileTimeFloatType -> when {
                        this is BoolType -> return targetType
                        // Conversion to float will always succeed for numeric types
                        this.isNumeric() -> return targetType
                        this.isText() -> return StaticType.unionOf(targetType, StaticType.MISSING)
                    }
                    is CompileTimeDecimalType -> when {
                        this is StaticScalarType && type is CompileTimeDecimalType -> {
                            return if (targetType.type.maxDigits() >= type.maxDigits()) {
                                targetType
                            } else {
                                StaticType.unionOf(targetType, StaticType.MISSING)
                            }
                        }
                        this is Int2Type || this is Int4Type || this is Int8Type || this is IntType -> return when (targetType.type.precision) {
                            null -> targetType
                            else -> when (this) {
                                is IntType -> StaticType.unionOf(StaticType.MISSING, targetType)
                                is Int2Type ->
                                    // TODO: Move the magic numbers out
                                    // max smallint value 32,767, so the decimal needs to be able to hold at least 5 digits
                                    if (targetType.type.maxDigits() >= 5) {
                                        targetType
                                    } else {
                                        StaticType.unionOf(StaticType.MISSING, targetType)
                                    }
                                is Int4Type ->
                                    // max int4 value 2,147,483,647 so the decimal needs to be able to hold at least 10 digits
                                    if (targetType.type.maxDigits() >= 10) {
                                        targetType
                                    } else {
                                        StaticType.unionOf(StaticType.MISSING, targetType)
                                    }
                                is Int8Type ->
                                    // max bigint value 9,223,372,036,854,775,807 so the decimal needs to be able to hold at least 19 digits
                                    if (targetType.type.maxDigits() >= 19) {
                                        targetType
                                    } else {
                                        StaticType.unionOf(StaticType.MISSING, targetType)
                                    }
                                else -> error("Unreachable code")
                            }
                        }

                        this is BoolType || this.isNumeric() -> return targetType
                        this.isText() -> return StaticType.unionOf(targetType, StaticType.MISSING)
                    }
                }
                is TimestampType -> when {
                    this is TimestampType -> return targetType
                    this.isText() -> return StaticType.unionOf(targetType, StaticType.MISSING)
                }
                is VarcharType,
                is CharType,
                is StringType,
                is SymbolType -> when {
                    this.isNumeric() || this.isText() -> return targetType
                    this is BoolType || this is TimestampType -> return targetType
                }
                is ClobType -> when (this) {
                    is ClobType, is BlobType -> return targetType
                }
                is BlobType -> when (this) {
                    is ClobType, is BlobType -> return targetType
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
