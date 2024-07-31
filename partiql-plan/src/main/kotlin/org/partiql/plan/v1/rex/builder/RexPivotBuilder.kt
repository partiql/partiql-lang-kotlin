package org.partiql.plan.v1.rex.builder

import org.partiql.plan.v1.rel.Rel
import org.partiql.plan.v1.rex.Rex
import org.partiql.plan.v1.rex.RexPivot

/**
 * TODO DOCUMENTATION
 */
public class RexPivotBuilder {

    private var input: Rel? = null
    private var key: Rex? = null
    private var value: Rex? = null

    public fun input(input: Rel?): RexPivotBuilder {
        this.input = input
        return this
    }

    public fun key(key: Rex): RexPivotBuilder {
        this.key = key
        return this
    }

    public fun value(`value`: Rex?): RexPivotBuilder {
        this.value = value
        return this
    }

    /**
     * Instantiate the default [RexPivot].
     */
    public fun build(): RexPivot = object : RexPivot.Base(
        input = input ?: error("RexPivot input is required"),
        key = key ?: error("RexPivot key is required"),
        value = value ?: error("RexPivot value is required"),
    ) {}
}