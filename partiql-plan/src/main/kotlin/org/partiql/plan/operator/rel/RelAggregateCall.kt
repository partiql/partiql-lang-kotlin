package org.partiql.plan.operator.rel

import org.partiql.plan.operator.rex.Rex
import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RelAggregateCall {

    public fun isDistinct(): Boolean

    public fun getName(): String

    public fun getType(): PType

    public fun getArgs(): List<Rex>
}
