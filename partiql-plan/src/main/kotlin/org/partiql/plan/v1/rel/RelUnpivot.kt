package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelUnpivot : Rel {

    public fun getInput(): Rex

    override fun getInputs(): List<Rel> = emptyList()

    override fun isOrdered(): Boolean = false

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitUnpivot(this, ctx)

    /**
     * Default [RelUnpivot] implementation meant for extension.
     */
    public abstract class Base(input: Rex) : RelUnpivot {

        // DO NOT USE FINAL
        private var _input: Rex = input

        override fun getInput(): Rex = _input

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other !is RelUnpivot) return false
            return _input == other.getInput()
        }

        override fun hashCode(): Int {
            return _input.hashCode()
        }
    }
}
