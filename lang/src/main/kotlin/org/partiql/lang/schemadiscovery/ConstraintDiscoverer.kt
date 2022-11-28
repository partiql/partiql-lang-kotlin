package org.partiql.lang.schemadiscovery

import com.amazon.ion.IonBlob
import com.amazon.ion.IonBool
import com.amazon.ion.IonClob
import com.amazon.ion.IonDecimal
import com.amazon.ion.IonFloat
import com.amazon.ion.IonInt
import com.amazon.ion.IonList
import com.amazon.ion.IonNull
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.ionInt
import org.partiql.ionschema.model.IonSchemaModel
import java.math.BigInteger

// Additional constraint discovery constants
internal const val MIN_INT2 = Short.MIN_VALUE.toLong()
internal const val MAX_INT2 = Short.MAX_VALUE.toLong()
internal const val MIN_INT4 = Int.MIN_VALUE.toLong()
internal const val MAX_INT4 = Int.MAX_VALUE.toLong()
internal const val MIN_INT8 = Long.MIN_VALUE
internal const val MAX_INT8 = Long.MAX_VALUE
internal val INT2_RANGE = BigInteger.valueOf(MIN_INT2)..BigInteger.valueOf(MAX_INT2)
internal val INT4_RANGE = BigInteger.valueOf(MIN_INT4)..BigInteger.valueOf(MAX_INT4)
internal val INT8_RANGE = BigInteger.valueOf(MIN_INT8)..BigInteger.valueOf(MAX_INT8)
internal val INT2_RANGE_CONSTRAINT = IonSchemaModel.build { validValues(rangeOfValidValues(numRange(numberRange(inclusive(ionInt(MIN_INT2)), inclusive(ionInt(MAX_INT2)))))) }
internal val INT4_RANGE_CONSTRAINT = IonSchemaModel.build { validValues(rangeOfValidValues(numRange(numberRange(inclusive(ionInt(MIN_INT4)), inclusive(ionInt(MAX_INT4)))))) }
internal val INT8_RANGE_CONSTRAINT = IonSchemaModel.build { validValues(rangeOfValidValues(numRange(numberRange(inclusive(ionInt(MIN_INT8)), inclusive(ionInt(MAX_INT8)))))) }

/**
 * Defines how additional constraints are to be discovered. This is intended to be called by a [ConstraintInferer] for
 * each of the [IonType]s.
 */
internal interface ConstraintDiscoverer {
    fun discover(value: IonValue): IonSchemaModel.ConstraintList
}

/**
 * An implementation of [ConstraintDiscoverer] that supports all [IonType]s except for DATAGRAM. All base
 * implementations return an empty [IonSchemaModel.ConstraintList] and do not depend on each other (i.e. sequence
 * and struct constraint discoverers do not call the scalar constraint discoverers).
 *
 * Since this is intended to be used by the [TypeAndConstraintInferer] after inferring the
 * [IonSchemaModel.Constraint.TypeConstraint], typed nulls collapse to `null` and will not have any additional
 * constraints discovered.
 */
internal open class TypeConstraintDiscoverer : ConstraintDiscoverer {
    override fun discover(value: IonValue): IonSchemaModel.ConstraintList =
        when (value) {
            is IonBool -> constraintDiscovererBool(value)
            is IonInt -> constraintDiscovererInt(value)
            is IonFloat -> constraintDiscovererFloat(value)
            is IonDecimal -> constraintDiscovererDecimal(value)
            is IonTimestamp -> constraintDiscovererTimestamp(value)
            is IonSymbol -> constraintDiscovererSymbol(value)
            is IonString -> constraintDiscovererString(value)
            is IonClob -> constraintDiscovererClob(value)
            is IonBlob -> constraintDiscovererBlob(value)
            is IonNull -> constraintDiscovererNull(value)
            is IonSexp -> constraintDiscovererSexp(value)
            is IonList -> constraintDiscovererList(value)
            is IonStruct -> constraintDiscovererStruct(value)
            else -> error("Given type is not supported for conversion")
        }

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonBool]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererBool(value: IonBool): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonInt]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererInt(value: IonInt): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonFloat]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererFloat(value: IonFloat): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonDecimal]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererDecimal(value: IonDecimal): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonTimestamp]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererTimestamp(value: IonTimestamp): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonSymbol]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererSymbol(value: IonSymbol): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonString]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererString(value: IonString): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonClob]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererClob(value: IonClob): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonBlob]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererBlob(value: IonBlob): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonNull]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererNull(value: IonNull): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonSexp]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererSexp(value: IonSexp): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonList]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererList(value: IonList): IonSchemaModel.ConstraintList = emptyConstraintList

    /**
     * Returns a [IonSchemaModel.ConstraintList] with additional discovered constraints for [IonStruct]s.
     *
     * This implementation returns an empty constraint list.
     */
    open fun constraintDiscovererStruct(value: IonStruct): IonSchemaModel.ConstraintList = emptyConstraintList
}

/**
 * A [ConstraintDiscoverer] that infers additional constraints for the following [IonType]s:
 *  [IonInt]- valid_values
 *  [IonDecimal]- scale and precision
 *  [IonString]- codepoint_length
 *
 * Currently, this class is not open and not intended to be extended. Creating a new [ConstraintDiscoverer] with
 * some specific discovered constraints can be done through overriding [TypeConstraintDiscoverer]'s extensible
 * `constraintDiscoverer...` functions.
 */
internal class StandardConstraintDiscoverer : TypeConstraintDiscoverer() {
    override fun constraintDiscovererInt(value: IonInt) = INT_VALID_VALUES_DISCOVERER(value)
    override fun constraintDiscovererDecimal(value: IonDecimal) = DECIMAL_SCALE_AND_PRECISION_DISCOVERER(value)
    override fun constraintDiscovererString(value: IonString) = STRING_CODEPOINT_LENGTH_DISCOVERER(value)
}

/**
 * Given an [IonInt], returns a constraint list with [IonSchemaModel.Constraint.ValidValues] range depending on its
 * value. If the int is between:
 *   [Short.MIN_VALUE] to [Short.MAX_VALUE]: range(Short.MIN_VALUE, Short.MAX_VALUE)
 *   [Int.MIN_VALUE] to [Int.MAX_VALUE]: range(Int.MIN_VALUE, Int.MAX_VALUE)
 *   [Long.MIN_VALUE] to [Long.MAX_VALUE]: range(Long.MIN_VALUE, Long.MAX_VALUE)
 *   else: no valid_values range included
 */
internal val INT_VALID_VALUES_DISCOVERER = { value: IonInt ->
    IonSchemaModel.build {
        when (value.bigIntegerValue()) {
            in INT2_RANGE -> constraintList(INT2_RANGE_CONSTRAINT)
            in INT4_RANGE -> constraintList(INT4_RANGE_CONSTRAINT)
            in INT8_RANGE -> constraintList(INT8_RANGE_CONSTRAINT)
            else -> constraintList() // unconstrained int has no constraint added
        }
    }
}

/**
 * Given an [IonDecimal], returns a constraint list with the decimal's [IonSchemaModel.Constraint.Scale] and
 * [IonSchemaModel.Constraint.Precision].
 */
internal val DECIMAL_SCALE_AND_PRECISION_DISCOVERER = { value: IonDecimal ->
    val decimal = value.decimalValue()
    val scale = decimal.scale().toLong()
    val precision = decimal.precision().toLong()

    IonSchemaModel.build {
        constraintList(
            scale(equalsNumber(ionInt(scale))),
            precision(equalsNumber(ionInt(precision)))
        )
    }
}

/**
 * Given an [IonString], returns a constraint list with the string's [IonSchemaModel.Constraint.CodepointLength].
 */
internal val STRING_CODEPOINT_LENGTH_DISCOVERER = { value: IonString ->
    val s = value.stringValue()
    val len = s.length.toLong()
    IonSchemaModel.build { constraintList(codepointLength(equalsNumber(ionInt(len)))) }
}
