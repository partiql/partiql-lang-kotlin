package org.partiql.lang.ast.passes.inference

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.AnyType
import org.partiql.lang.types.BlobType
import org.partiql.lang.types.BoolType
import org.partiql.lang.types.ClobType
import org.partiql.lang.types.CollectionType
import org.partiql.lang.types.DecimalType
import org.partiql.lang.types.FloatType
import org.partiql.lang.types.IntType
import org.partiql.lang.types.MissingType
import org.partiql.lang.types.NullType
import org.partiql.lang.types.SingleType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StringType
import org.partiql.lang.types.StructType
import org.partiql.lang.types.SymbolType
import org.partiql.lang.types.TimestampType

internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)
internal fun StaticType.isNumeric(): Boolean = (this is IntType || this is FloatType || this is DecimalType)
internal fun StaticType.isText(): Boolean = (this is SymbolType || this is StringType)
internal fun StaticType.isLob(): Boolean = (this is BlobType || this is ClobType)
internal fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)
internal fun StaticType.isKnown(): Boolean = !isUnknown()

/**
 * Returns the maximum number of digits a decimal can hold after reserving digits for scale
 *
 * For example: The maximum value a DECIMAL(5,2) can represent is 999.99, therefore the maximum
 *  number of digits it can hold is 3 (i.e up to 999).
 */
private fun DecimalType.maxDigits(): Int {
    val precision = when (precisionScaleConstraint) {
        // TODO: What's PartiQL's max allowed precision?
        DecimalType.PrecisionScaleConstraint.Unconstrained -> Int.MAX_VALUE
        is DecimalType.PrecisionScaleConstraint.Constrained -> precisionScaleConstraint.precision
    }

    val scale = when (precisionScaleConstraint) {
        DecimalType.PrecisionScaleConstraint.Unconstrained -> 0
        is DecimalType.PrecisionScaleConstraint.Constrained -> precisionScaleConstraint.scale
    }

    return precision - scale
}

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
                is IntType -> when {
                    this is BoolType -> return targetType
                    this is IntType -> {
                        return when (targetType.rangeConstraint) {
                            IntType.IntRangeConstraint.SHORT -> when (this.rangeConstraint) {
                                IntType.IntRangeConstraint.SHORT -> targetType
                                IntType.IntRangeConstraint.INT4, IntType.IntRangeConstraint.LONG, IntType.IntRangeConstraint.UNCONSTRAINED -> StaticType.unionOf(StaticType.MISSING, targetType)
                            }
                            IntType.IntRangeConstraint.INT4 -> when (this.rangeConstraint) {
                                IntType.IntRangeConstraint.SHORT, IntType.IntRangeConstraint.INT4 -> targetType
                                IntType.IntRangeConstraint.LONG, IntType.IntRangeConstraint.UNCONSTRAINED -> StaticType.unionOf(StaticType.MISSING, targetType)
                            }
                            IntType.IntRangeConstraint.LONG -> when (this.rangeConstraint) {
                                IntType.IntRangeConstraint.SHORT, IntType.IntRangeConstraint.INT4, IntType.IntRangeConstraint.LONG -> targetType
                                IntType.IntRangeConstraint.UNCONSTRAINED -> StaticType.unionOf(StaticType.MISSING, targetType)
                            }
                            IntType.IntRangeConstraint.UNCONSTRAINED -> targetType
                        }
                    }
                    this is FloatType -> return when (targetType.rangeConstraint) {
                        IntType.IntRangeConstraint.UNCONSTRAINED -> targetType
                        else -> StaticType.unionOf(StaticType.MISSING, targetType)
                    }

                    this is DecimalType -> return when (targetType.rangeConstraint) {
                        IntType.IntRangeConstraint.UNCONSTRAINED -> targetType
                        IntType.IntRangeConstraint.SHORT, IntType.IntRangeConstraint.INT4, IntType.IntRangeConstraint.LONG ->
                            return when (this.precisionScaleConstraint) {
                                DecimalType.PrecisionScaleConstraint.Unconstrained -> StaticType.unionOf(StaticType.MISSING, targetType)
                                is DecimalType.PrecisionScaleConstraint.Constrained -> {

                                    // Max value of SMALLINT is 32767.
                                    // Conversion to SMALLINT will work as long as the decimal holds up 4 to digits. There is a chance of overflow beyond that.
                                    // Similarly -
                                    //   Max value of INT4 is 2,147,483,647
                                    //   Max value of BIGINT is 9,223,372,036,854,775,807 for BIGINT
                                    // TODO: Move these magic numbers out.
                                    val maxDigitsWithoutPrecisionLoss = when (targetType.rangeConstraint) {
                                        IntType.IntRangeConstraint.SHORT -> 4
                                        IntType.IntRangeConstraint.INT4 -> 9
                                        IntType.IntRangeConstraint.LONG -> 18
                                        IntType.IntRangeConstraint.UNCONSTRAINED -> error("Un-constrained is handled above. This code shouldn't be reached.")
                                    }

                                    if (this.maxDigits() > maxDigitsWithoutPrecisionLoss) {
                                        StaticType.unionOf(StaticType.MISSING, targetType)
                                    } else {
                                        targetType
                                    }
                                }
                            }
                    }
                    this.isText() -> return StaticType.unionOf(targetType, StaticType.MISSING)
                }
                is FloatType -> when {
                    this is BoolType -> return targetType
                    // Conversion to float will always succeed for numeric types
                    this.isNumeric() -> return targetType
                    this.isText() -> return StaticType.unionOf(targetType, StaticType.MISSING)
                }
                is DecimalType -> when {
                    this is DecimalType -> {
                        return if (targetType.maxDigits() >= this.maxDigits()) {
                            targetType
                        } else {
                            StaticType.unionOf(targetType, StaticType.MISSING)
                        }
                    }
                    this is IntType -> return when (targetType.precisionScaleConstraint) {
                        DecimalType.PrecisionScaleConstraint.Unconstrained -> targetType
                        is DecimalType.PrecisionScaleConstraint.Constrained -> when (this.rangeConstraint) {
                            IntType.IntRangeConstraint.UNCONSTRAINED -> StaticType.unionOf(StaticType.MISSING, targetType)
                            IntType.IntRangeConstraint.SHORT ->
                                // TODO: Move the magic numbers out
                                // max smallint value 32,767, so the decimal needs to be able to hold at least 5 digits
                                if (targetType.maxDigits() >= 5) {
                                    targetType
                                } else {
                                    StaticType.unionOf(StaticType.MISSING, targetType)
                                }
                            IntType.IntRangeConstraint.INT4 ->
                                // max int4 value 2,147,483,647 so the decimal needs to be able to hold at least 10 digits
                                if (targetType.maxDigits() >= 10) {
                                    targetType
                                } else {
                                    StaticType.unionOf(StaticType.MISSING, targetType)
                                }
                            IntType.IntRangeConstraint.LONG ->
                                // max bigint value 9,223,372,036,854,775,807 so the decimal needs to be able to hold at least 19 digits
                                if (targetType.maxDigits() >= 19) {
                                    targetType
                                } else {
                                    StaticType.unionOf(StaticType.MISSING, targetType)
                                }
                        }
                    }

                    this is BoolType || this.isNumeric() -> return targetType
                    this.isText() -> return StaticType.unionOf(targetType, StaticType.MISSING)
                }
                is TimestampType -> when {
                    this is TimestampType -> return targetType
                    this.isText() -> return StaticType.unionOf(targetType, StaticType.MISSING)
                }
                is StringType, is SymbolType -> when {
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
