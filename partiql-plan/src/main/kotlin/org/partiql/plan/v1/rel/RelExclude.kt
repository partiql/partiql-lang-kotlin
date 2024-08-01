package org.partiql.plan.v1.rel

/**
 * Logical `EXCLUDE` operation.
 */
public interface RelExclude : Rel {

    public fun getInput(): Rel

    public fun getPaths(): List<RelExcludePath>

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = getInput().isOrdered()

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitExclude(this, ctx)

    /**
     * Default [RelExclude] implementation meant for extension.
     */
    public abstract class Base(input: Rel, paths: List<RelExcludePath>) : RelExclude {

        // DO NOT USE FINAL
        private var _input: Rel = input
        private var _inputs: List<Rel>? = null
        private var _paths: List<RelExcludePath> = paths
        private var _ordered: Boolean = input.isOrdered()

        override fun getInput(): Rel = _input

        override fun getInputs(): List<Rel> {
            if (_inputs == null) {
                _inputs = listOf(_input)
            }
            return _inputs!!
        }

        override fun getPaths(): List<RelExcludePath> = _paths

        override fun isOrdered(): Boolean = _ordered

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RelExclude) return false
            if (_input != other.getInput()) return false
            if (_paths != other.getPaths()) return false
            return true
        }

        override fun hashCode(): Int {
            var result = 1
            result = 31 * result + _input.hashCode()
            result = 31 * result + _paths.hashCode()
            return result
        }
    }
}
