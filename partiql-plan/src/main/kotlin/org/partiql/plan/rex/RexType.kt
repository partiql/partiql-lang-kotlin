package org.partiql.plan.rex

import org.partiql.types.PType

/**
 * [RexType] is a simple wrapper over [PType].
 *
 * In later releases, a [RexType] may hold metadata to aid custom planner implementations.
 */
public class RexType internal constructor(type: PType) {

    private var _type: PType = type

    public fun getPType(): PType = _type

    override fun equals(other: Any?): Boolean = _type == other

    override fun hashCode(): Int = _type.hashCode()

    override fun toString(): String = _type.toString()

    public companion object {

        /**
         * Create a [RexType] from a PType.
         *
         * @param type
         * @return
         */
        @JvmStatic
        public fun of(type: PType): RexType = RexType(type)
    }
}
