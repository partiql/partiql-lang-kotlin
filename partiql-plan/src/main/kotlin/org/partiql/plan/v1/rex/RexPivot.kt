package org.partiql.plan.v1.rex

import org.partiql.plan.v1.rel.Rel
import org.partiql.plan.v1.rex.builder.RexPivotBuilder
import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexPivot : Rex {

    public fun getInput(): Rel

    public fun getKey(): Rex

    public fun getValue(): Rex

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitPivot(this, ctx)

    public companion object {

        @JvmStatic
        public fun builder(): RexPivotBuilder = RexPivotBuilder()
    }

    /**
     * An abstract [RexPivot] implementation intended for extension.
     *
     * @property input
     * @property key
     * @property value
     */
    public abstract class Base(
        private val input: Rel,
        private val key: Rex,
        private val value: Rex,
    ) : RexPivot {

        private var operands: List<Rex>? = null
        private var type:  PType? = null

        override fun getInput(): Rel = input

        override fun getKey(): Rex = key

        override fun getValue(): Rex = value

        override fun getType(): PType {
            if (type == null) {
                type = PType.typeStruct()
            }
            return type!!
        }

        override fun getOperands(): List<Rex> {
            if (operands == null) {
                operands = listOf(key, value)
            }
            return operands!!
        }
    }
}
