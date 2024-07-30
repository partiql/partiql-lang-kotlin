package org.partiql.plan.v1.rex

import org.partiql.plan.v1.Node
import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface Rex : Node {

    public fun getType(): PType
}
