package org.partiql.eval.internal.window

import org.partiql.eval.Environment
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.spi.value.Datum

/**
 * Base class for navigation functions such as [LeadFunction] and [LagFunction].
 */
internal abstract class NavigationFunction : WindowFunction {

    protected lateinit var partition: WindowPartition
    protected var currentPosition: Long = -1L

    /**
     * Evaluate the function for the current row.
     * @param env the environment to use for evaluation
     * @return the result of the evaluation
     */
    abstract fun eval(env: Environment): Datum

    /**
     * Reset the function to its initial state.
     */
    abstract fun reset()

    override fun reset(partition: WindowPartition) {
        this.partition = partition
        currentPosition = -1L
        reset()
    }

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        currentPosition++
        return eval(env)
    }
}
