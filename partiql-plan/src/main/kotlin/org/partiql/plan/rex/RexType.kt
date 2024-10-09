package org.partiql.plan.rex

import org.partiql.types.PType

/**
 * [RexType] is a simple wrapper over [PType], but does not necessarily only hold a PType.
 *
 *
 * Developer Note: In later releases, a [RexType] may hold metadata to aid custom planner implementations.
 */
public class RexType public constructor(type: PType) {

    // PRIVATE VAR
    private var _type: PType = type

    public fun getPType(): PType = _type

    override fun equals(other: Any?): Boolean = _type == other

    override fun hashCode(): Int = _type.hashCode()

    override fun toString(): String = _type.toString()

    public companion object {

        /**
         * A [RexType] for an "untyped" logical plan node.
         */
        @JvmStatic
        public fun dynamic(): RexType = RexType(PType.dynamic())
    }
}
