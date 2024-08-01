package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexCoalesce : Rex {

    public fun getArgs(): List<Rex>

    public override fun getOperands(): List<Rex> = getArgs()

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCoalesce(this, ctx)

    public abstract class Base(args: List<Rex>) : RexCoalesce {

        // DO NOT USE FINAL
        private var _args = args

        public override fun getArgs(): List<Rex> = _args

        public override fun getOperands(): List<Rex> = _args

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexCoalesce) return false
            if (_args != other.getArgs()) return false
            return true
        }

        public override fun hashCode(): Int = _args.hashCode()
    }
}
