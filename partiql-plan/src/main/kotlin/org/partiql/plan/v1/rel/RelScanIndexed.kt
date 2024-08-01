package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * Logical scan with index corresponding to the clause `FROM <expression> AS <v> AT <i>`.
 */
public interface RelScanIndexed : Rel {

    public fun getInput(): Rex

    override fun getInputs(): List<Rel> = emptyList()

    override fun isOrdered(): Boolean = true

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitScanIndexed(this, ctx)

    /**
     * Default [RelScanIndexed] implementation meant for extension.
     */
    public abstract class Base(input: Rex) : RelScanIndexed {

        // DO NOT USE FINAL
        private var _input: Rex = input

        override fun getInput(): Rex = _input

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other !is RelScanIndexed) return false
            return _input == other.getInput()
        }

        override fun hashCode(): Int {
            return _input.hashCode()
        }
    }
}
