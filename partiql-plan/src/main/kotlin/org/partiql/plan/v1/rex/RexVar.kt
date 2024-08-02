package org.partiql.plan.v1.rex

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

    public override fun getOperands(): List<Rex> = emptyList()

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitVar(this, ctx)

    /**
     * Default [RexVar] implementation intended for extension.
     */
    public abstract class Base(depth: Int, offset: Int) : RexVar {

        // DO NOT USE FINAL
        private var _depth = depth
        private var _offset = offset

        public override fun getDepth(): Int = _depth

        public override fun getOffset(): Int = _offset

        override fun getType(): PType {
            TODO("Not yet implemented")
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexVar) return false
            if (_depth != other.getDepth()) return false
            if (_offset != other.getOffset()) return false
            return true
        }

        public override fun hashCode(): Int {
            var result = 1
            result = 31 * result + _depth
            result = 31 * result + _offset
            return result
        }
    }
}
