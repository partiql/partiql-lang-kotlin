package org.partiql.plan.v1.rel

import org.partiql.plan.v1.Schema
import org.partiql.plan.v1.rex.Rex

/**
 * Logical `LIMIT` operator.
 */
interface RelLimit : Rel {

    public fun getInput(): Rel

    public fun getLimit(): Rex

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitLimit(this, ctx)

    /**
     * Default [RelLimit] implementation meant for extension.
     */
    public abstract class Base(input: Rel, limit: Rex) : RelLimit {

        // DO NOT USE FINAL
        private var _input: Rel = input
        private var _limit: Rex = limit

        private var _inputs: List<Rel>? = null
        private var _schema: Schema = input.getSchema()
        private var _ordered: Boolean = input.isOrdered()

        override fun getInput(): Rel = _input

        override fun getLimit(): Rex = _limit

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
            if (other !is RelLimit) return false
            if (_input != other.getInput()) return false
            if (_limit != other.getLimit()) return false
            return true
        }

        override fun hashCode(): Int {
            var result = 1
            result = 31 * result + _input.hashCode()
            result = 31 * result + _limit.hashCode()
            return result
        }
    }
}
