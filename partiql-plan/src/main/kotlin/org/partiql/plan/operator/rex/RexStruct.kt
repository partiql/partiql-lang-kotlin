package org.partiql.plan.operator.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexStruct : Rex {

    public fun getFields(): List<Field>

    override fun getOperands(): List<Rex> {
        val operands = mutableListOf<Rex>()
        for (field in getFields()) {
            operands.add(field.getKey())
            operands.add(field.getValue())
        }
        return operands
    }

    override fun getType(): PType = PType.typeStruct()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitStruct(this, ctx)

    /**
     * TODO DOCUMENTATION
     */
    public interface Field {
        public fun getKey(): Rex
        public fun getValue(): Rex
    }
}

/**
 * Default [RexStruct] implementation intended for extension.
 */
internal class RexStructImpl(fields: List<RexStruct.Field>) : RexStruct {

    // DO NOT USE FINAL
    private var _fields = fields
    private var _operands: List<Rex>? = null

    override fun getFields(): List<RexStruct.Field> = _fields

    override fun getOperands(): List<Rex> {
        if (_operands == null) {
            _operands = super.getOperands()
        }
        return _operands!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexStruct) return false
        if (_fields != other.getFields()) return false
        return true
    }

    override fun hashCode(): Int = _fields.hashCode()
}
