package org.partiql.plan.v1.rel

import org.partiql.plan.v1.Schema

/**
 * Logical `DISTINCT` operator.
 */
public interface RelDistinct : Rel {

    public fun getInput(): Rel

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitDistinct(this, ctx)

    /**
     * Default [RelDistinct] implementation meant for extension.
     */
    public abstract class Base(input: Rel) : RelDistinct {

        // DO NOT USE FINAL
        private var _input: Rel = input
        private var _inputs: List<Rel>? = null
        private var _ordered: Boolean = input.isOrdered()

        override fun getInput(): Rel = _input

        override fun getInputs(): List<Rel> {
            if (_inputs == null) {
                _inputs = listOf(_input)
            }
            return _inputs!!
        }

        override fun getSchema(): Schema = _input.getSchema()

        override fun isOrdered(): Boolean = _ordered

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other !is RelDistinct) return false
            return _input == other.getInput()
        }

        override fun hashCode(): Int {
            return _input.hashCode()
        }
    }
}
