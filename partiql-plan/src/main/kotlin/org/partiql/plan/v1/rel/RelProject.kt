package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * Logical `PROJECTION` operator
 */
public interface RelProject : Rel {

    public fun getInput(): Rel

    public fun getProjections(): List<Rex>

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = getInput().isOrdered()

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitProject(this, ctx)

    /**
     * Default [RelProject] implementation meant for extension.
     */
    public abstract class Base(input: Rel, projections: List<Rex>) : RelProject {

        // DO NOT USE FINAL
        private var _input = input
        private var _projections = projections

        private var _inputs: List<Rel>? = null

        override fun getInput(): Rel = _input

        override fun getProjections(): List<Rex> = _projections

        override fun getInputs(): List<Rel> {
            if (_inputs == null) {
                _inputs = listOf(_input)
            }
            return _inputs!!
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RelProject) return false
            if (_input != other.getInput()) return false
            if (_projections != other.getProjections()) return false
            return true
        }

        override fun hashCode(): Int {
            var result = 1
            result = 31 * result + _input.hashCode()
            result = 31 * result + _projections.hashCode()
            return result
        }
    }
}
