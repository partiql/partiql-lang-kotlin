package org.partiql.spi.datetime

import java.math.BigDecimal

/**
 * This is experimental and, at present, cannot be instantiated.
 *
 * An interval is composed of a contiguous subset of the fields.
 * The subset of the fields is called the “precision” of the value.
 * The leading field, either “YEAR” or “DAY” is only constrained by the “leading-field” precision.
 * All fields are integers except for “SECOND” which has “fractional seconds precision”.
 */
public class Interval private constructor(
    private val years: Int,
    private val months: Int,
    private val days: Int,
    private val hours: Int,
    private val minutes: Int,
    private val seconds: BigDecimal
) {

    /**
     * Years, range: unconstrained.
     */
    public fun getYears(): Int = 0

    /**
     * Months, range: [0,11]
     */
    public fun getMonths(): Int = 0

    /**
     * Days, range: unconstrained
     */
    public fun getDays(): Int = 0

    /**
     * Hours, range: [0,23]
     */
    public fun getHours(): Int = 0

    /**
     * Minutes, range: [0,59]
     */
    public fun getMinutes(): Int = 0

    /**
     * Seconds, range: [0,59.999999999...]
     *
     * NOTE: The BigDecimal value precision reflects the interval seconds precision.
     */
    public fun getSeconds(): BigDecimal = BigDecimal.ZERO
}
