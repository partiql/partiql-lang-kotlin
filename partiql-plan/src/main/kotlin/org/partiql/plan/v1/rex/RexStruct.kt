package org.partiql.plan.v1.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexStruct : Rex {

    public fun getFields(): List<RexStructField>

    public override fun getOperands(): List<Rex> {
        val operands = mutableListOf<Rex>()
        for (field in getFields()) {
            operands.add(field.getKey())
            operands.add(field.getValue())
        }
        return operands
    }

    public override fun getType(): PType = PType.struct()

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitStruct(this, ctx)

    /**
     * Default [RexStruct] implementation intended for extension.
     */
    public abstract class Base(fields: List<RexStructField>) : RexStruct {

        // DO NOT USE FINAL
        private var _fields = fields
        private var _operands: List<Rex>? = null

        public override fun getFields(): List<RexStructField> = _fields

        public override fun getOperands(): List<Rex> {
            if (_operands == null) {
                _operands = super.getOperands()
            }
            return _operands!!
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexStruct) return false
            if (_fields != other.getFields()) return false
            return true
        }

        public override fun hashCode(): Int = _fields.hashCode()
    }
}
