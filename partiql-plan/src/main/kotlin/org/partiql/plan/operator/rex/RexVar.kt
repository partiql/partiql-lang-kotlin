package org.partiql.plan.operator.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 * TODO NAMING??
 */
public interface RexVar : Rex {

    /**
     * 0-indexed scope offset.
     */
    public fun getDepth(): Int

    /**
     * 0-index tuple offset.
     */
    public fun getOffset(): Int

    override fun getOperands(): List<Rex> = emptyList()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitVar(this, ctx)

    /**
     * Default [RexVar] implementation intended for extension.
     */
    abstract class Base(depth: Int, offset: Int) : RexVar {

        // DO NOT USE FINAL
        private var _depth = depth
        private var _offset = offset

        override fun getDepth(): Int = _depth

        override fun getOffset(): Int = _offset

        override fun getType(): PType {
            TODO("Not yet implemented")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexVar) return false
            if (_depth != other.getDepth()) return false
            if (_offset != other.getOffset()) return false
            return true
        }

        override fun hashCode(): Int {
            var result = 1
            result = 31 * result + _depth
            result = 31 * result + _offset
            return result
        }
    }
}
