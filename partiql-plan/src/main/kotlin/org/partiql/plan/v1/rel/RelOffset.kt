package org.partiql.plan.v1.rel

import org.partiql.plan.v1.Schema
import org.partiql.plan.v1.rex.Rex

/**
 * Logical `OFFSET` operator.
 */
interface RelOffset : Rel {

    public fun getInput(): Rel

    public fun getOffset(): Rex

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitOffset(this, ctx)

    /**
     * Default [RelOffset] implementation meant for extension.
     */
    public abstract class Base(input: Rel, offset: Rex) : RelOffset {

        // DO NOT USE FINAL
        private var _input: Rel = input
        private var _offset: Rex = offset

        private var _inputs: List<Rel>? = null
        private var _schema: Schema = input.getSchema()
        private var _ordered: Boolean = input.isOrdered()

        override fun getInput(): Rel = _input

        override fun getOffset(): Rex = _offset

        override fun getInputs(): List<Rel> {
            if (_inputs == null) {
                _inputs = listOf(_input)
            }
            return _inputs!!
        }

        override fun getSchema(): Schema = _schema

        override fun isOrdered(): Boolean = _ordered

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RelOffset) return false
            if (_input != other.getInput()) return false
            if (_offset != other.getOffset()) return false
            return true
        }

        override fun hashCode(): Int {
            var result = 1
            result = 31 * result + _input.hashCode()
            result = 31 * result + _offset.hashCode()
            return result
        }
    }
}
