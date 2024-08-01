package org.partiql.plan.v1.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexTupleUnion : Rex {

    public fun getArgs(): List<Rex>

    public override fun getOperands(): List<Rex> = getArgs()

    public override fun getType(): PType = PType.typeStruct()

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitTupleUnion(this, ctx)

    /**
     * Default [RexTupleUnion] operator intended for extension.
     */
    public abstract class Base(args: List<Rex>) : RexTupleUnion {

        // DO NOT USE FINAL
        private var _args = args

        public override fun getArgs(): List<Rex> = _args

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexTupleUnion) return false
            if (_args != other.getArgs()) return false
            return true
        }

        public override fun hashCode(): Int {
            return _args.hashCode()
        }
    }
}
