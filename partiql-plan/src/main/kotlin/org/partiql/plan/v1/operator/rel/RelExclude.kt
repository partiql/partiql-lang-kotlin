package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.Schema

/**
 * Logical `EXCLUDE` operation.
 */
public interface RelExclude : Rel {

    public fun getInput(): Rel

    public fun getPaths(): List<RelExcludePath>

    override fun getChildren(): Collection<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitExclude(this, ctx)
}

/**
 * Default [RelExclude] implementation.
 */
internal class RelExcludeImpl(input: Rel, paths: List<RelExcludePath>) : RelExclude {

    // DO NOT USE FINAL
    private var _input: Rel = input
    private var _paths: List<RelExcludePath> = paths
    private var _ordered: Boolean = input.isOrdered()

    override fun getInput(): Rel = _input

    override fun getChildren(): Collection<Rel> = listOf(_input)

    override fun getPaths(): List<RelExcludePath> = _paths

    override fun isOrdered(): Boolean = _ordered

    override fun getSchema(): Schema {
        TODO("Not yet implemented")
    }

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
