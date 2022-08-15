package org.partiql.lang.ots_work.plugins.standard.operators

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.fillErrorContext
import org.partiql.lang.ots_work.interfaces.ScalarType
import org.partiql.lang.util.bigDecimalOf

/** Regex to match DATE strings of the format yyyy-MM-dd */
internal val datePatternRegex = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d")

/** Types that are cast to the [ExprValueType.isText] types by calling `IonValue.toString()`. */
internal val ION_TEXT_STRING_CAST_TYPES = setOf(ExprValueType.BOOL, ExprValueType.TIMESTAMP)

internal val longMaxDecimal = bigDecimalOf(Long.MAX_VALUE)
internal val longMinDecimal = bigDecimalOf(Long.MIN_VALUE)

internal val ion = IonSystemBuilder.standard().build()

internal fun castExceptionContext(
    sourceValue: ExprValue,
    targetScalarType: ScalarType,
    locationMeta: SourceLocationMeta?
): PropertyValueMap {
    val errorContext = PropertyValueMap().also {
        it[Property.CAST_FROM] = sourceValue.type.toString()
        it[Property.CAST_TO] = targetScalarType.runTimeType.toString()
    }

    locationMeta?.let { fillErrorContext(errorContext, it) }

    return errorContext
}

internal fun castFailedErr(
    sourceValue: ExprValue,
    message: String,
    internal: Boolean,
    cause: Throwable? = null,
    targetScalarType: ScalarType,
    locationMeta: SourceLocationMeta?
): Nothing {
    val errorContext = castExceptionContext(sourceValue, targetScalarType, locationMeta)

    val errorCode = if (locationMeta == null) {
        ErrorCode.EVALUATOR_CAST_FAILED_NO_LOCATION
    } else {
        ErrorCode.EVALUATOR_CAST_FAILED
    }

    throw EvaluationException(
        message = message,
        errorCode = errorCode,
        errorContext = errorContext,
        internal = internal,
        cause = cause
    )
}

/**
 * Remove leading spaces in decimal notation and the plus sign
 *
 * Examples:
 * - `"00001".normalizeForIntCast() == "1"`
 * - `"-00001".normalizeForIntCast() == "-1"`
 * - `"0x00001".normalizeForIntCast() == "0x00001"`
 * - `"+0x00001".normalizeForIntCast() == "0x00001"`
 * - `"000a".normalizeForIntCast() == "a"`
 */
internal fun String.normalizeForCastToInt(): String {
    fun Char.isSign() = this == '-' || this == '+'
    fun Char.isHexOrBase2Marker(): Boolean {
        val c = this.toLowerCase()

        return c == 'x' || c == 'b'
    }

    fun String.possiblyHexOrBase2() = (length >= 2 && this[1].isHexOrBase2Marker()) ||
        (length >= 3 && this[0].isSign() && this[2].isHexOrBase2Marker())

    return when {
        length == 0 -> this
        possiblyHexOrBase2() -> {
            if (this[0] == '+') {
                this.drop(1)
            } else {
                this
            }
        }
        else -> {
            val (isNegative, startIndex) = when (this[0]) {
                '-' -> Pair(true, 1)
                '+' -> Pair(false, 1)
                else -> Pair(false, 0)
            }

            var toDrop = startIndex
            while (toDrop < length && this[toDrop] == '0') {
                toDrop += 1
            }

            when {
                toDrop == length -> "0" // string is all zeros
                toDrop == 0 -> this
                toDrop == 1 && isNegative -> this
                toDrop > 1 && isNegative -> '-' + this.drop(toDrop)
                else -> this.drop(toDrop)
            }
        }
    }
}
