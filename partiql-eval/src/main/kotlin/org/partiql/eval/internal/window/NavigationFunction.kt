package org.partiql.eval.internal.window

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.spi.value.Datum

/**
 * Base class for navigation functions such as [LeadFunction] and [LagFunction].
 * @param direction -1 to navigate to preceding rows, 1 to navigate to following rows.
 */
internal abstract class NavigationFunction(
    private val expr: ExprValue,
    private val offset: ExprValue,
    private val default: ExprValue,
    private val ignoreNulls: Boolean,
    private val direction: Long,
) : WindowFunction {

    private lateinit var partition: WindowPartition
    private var currentPosition: Long = -1L

    override fun reset(partition: WindowPartition) {
        this.partition = partition
        currentPosition = -1L
    }

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        currentPosition++
        val offsetLong = offset.eval(env).long
        return navigate(env, direction * offsetLong)
    }

    /**
     * Evaluates [expr] against the row that is [offset] rows away from the current row (negative is preceding, positive
     * is following). With [ignoreNulls], rows whose [expr] evaluates to NULL/MISSING are skipped and not counted.
     * @return the evaluated [expr], or the evaluated [default] if no such row exists within the partition.
     */
    private fun navigate(env: Environment, offset: Long): Datum {
        if (!ignoreNulls || offset == 0L) {
            val index = currentPosition + offset
            if (index < 0 || index >= partition.size()) {
                return default.eval(env)
            }
            return expr.eval(env.push(partition.get(index)))
        }
        val step = if (offset > 0) 1L else -1L
        var remaining = if (offset > 0) offset else -offset
        var index = currentPosition
        while (true) {
            index += step
            if (index < 0 || index >= partition.size()) {
                return default.eval(env)
            }
            val value = expr.eval(env.push(partition.get(index)))
            if (value.isNull || value.isMissing) {
                continue
            }
            remaining--
            if (remaining == 0L) {
                return value
            }
        }
    }
}
