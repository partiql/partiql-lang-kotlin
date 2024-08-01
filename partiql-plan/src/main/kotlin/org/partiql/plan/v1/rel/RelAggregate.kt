package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelAggregate : Rel {

    public fun getInput(): Rel

    public fun getCalls(): List<RelAggregateCall>

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = false

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitAggregate(this, ctx)

    /**
     * Default [RelAggregate] implementation meant for extension.
     */
    public abstract class Base(input: Rel, collations: List<RelAggregateCall>) : RelAggregate {

        private var _input = input
        private var _collations = collations

        private var _inputs: List<Rel>? = null

        override fun getInput(): Rel = _input

        override fun getCalls(): List<RelAggregateCall> = _collations

        override fun getInputs(): List<Rel> {
            if (_inputs == null) {
                _inputs = listOf(_input)
            }
            return _inputs!!
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RelAggregate) return false
            if (_input != other.getInput()) return false
            if (_collations != other.getCalls()) return false
            return true
        }

        override fun hashCode(): Int {
            var result = 1
            result = 31 * result + _input.hashCode()
            result = 31 * result + _collations.hashCode()
            return result
        }
    }
}
