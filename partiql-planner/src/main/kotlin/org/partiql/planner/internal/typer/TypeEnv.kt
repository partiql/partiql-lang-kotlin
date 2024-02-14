package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.ir.rexOpVarLocal
import org.partiql.planner.internal.ir.rexOpVarOuter
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

/**
 * TypeEnv represents a variables type environment.
 *
 * @property outer refers to the outer variable scopes that we have access to.
 */
internal data class TypeEnv(
    public val schema: List<Rel.Binding>,
    public val outer: List<TypeEnv>
) {

    /**
     * We resolve a local with the following rules. See, PartiQL Specification p.35.
     *
     *  1) Check if the path root unambiguously matches a local binding name, set as root.
     *  2) Check if the path root unambiguously matches a local binding struct value field.
     *
     * Convert any remaining binding names (tail) to a path expression.
     *
     * @param path
     * @return
     */
    fun resolve(path: BindingPath): Rex? {
        val head: BindingName = path.steps[0]
        var tail: List<BindingName> = path.steps.drop(1)
        var r = matchRoot(head, scopeIndex)
        if (r == null) {
            r = matchStruct(head, scopeIndex) ?: return null
            tail = path.steps
        }
        val op = r.op as Rex.Op.Var.Outer
        if (op.scope == scopeIndex) {
            r = rex(r.type, rexOpVarLocal(op.ref))
        }
        // Convert any remaining binding names (tail) to an untyped path expression.
        return if (tail.isEmpty()) r else r.toPath(tail)
    }

    private val scopeIndex: Int = this.outer.size

    /**
     * Debugging string, ex: < x: int, y: string >
     *
     * @return
     */
    override fun toString(): String = "< " + schema.joinToString { "${it.name}: ${it.type}" } + " >"

    /**
     * Check if `name` unambiguously matches a local binding name and return its reference; otherwise return null.
     *
     * @param name
     * @return
     */
    private fun matchRoot(name: BindingName, scopeIndex: Int): Rex? {
        var r: Rex? = null
        for (i in schema.indices) {
            val local = schema[i]
            val type = local.type
            if (name.matches(local.name)) {
                if (r != null) {
                    // TODO root was already matched, emit ambiguous error.
                    return null
                }
                r = rex(type, rexOpVarOuter(scopeIndex, i))
            }
        }
        if (r == null && outer.isNotEmpty()) {
            return outer.last().matchRoot(name, scopeIndex - 1)
        }
        return r
    }

    /**
     * Check if `name` unambiguously matches a field within a struct and return its reference; otherwise return null.
     *
     * @param name
     * @return
     */
    private fun matchStruct(name: BindingName, scopeIndex: Int): Rex? {
        var c: Rex? = null
        var known = false
        for (i in schema.indices) {
            val local = schema[i]
            val type = local.type
            if (type is StructType) {
                when (type.containsKey(name)) {
                    true -> {
                        if (c != null && known) {
                            // TODO root was already definitively matched, emit ambiguous error.
                            return null
                        }
                        c = rex(type, rexOpVarOuter(scopeIndex, i))
                        known = true
                    }
                    null -> {
                        if (c != null) {
                            if (known) {
                                continue
                            } else {
                                // TODO we have more than one possible match, emit ambiguous error.
                                return null
                            }
                        }
                        c = rex(type, rexOpVarOuter(scopeIndex, i))
                        known = false
                    }
                    false -> continue
                }
            }
        }
        if (c == null && outer.isNotEmpty()) {
            return outer.last().matchStruct(name, scopeIndex - 1)
        }
        return c
    }

    /**
     * Searches for the [BindingName] within the given [StructType].
     *
     * Returns
     *  - true  iff known to contain key
     *  - false iff known to NOT contain key
     *  - null  iff NOT known to contain key
     *
     * @param name
     * @return
     */
    private fun StructType.containsKey(name: BindingName): Boolean? {
        for (f in fields) {
            if (name.matches(f.key)) {
                return true
            }
        }
        val closed = constraints.contains(TupleConstraint.Open(false))
        return if (closed) false else null
    }

    companion object {

        /**
         * Converts a list of [BindingName] to a path expression.
         *
         *  1) Case SENSITIVE identifiers become string literal key lookups.
         *  2) Case INSENSITIVE identifiers become symbol lookups.
         *
         * @param steps
         * @return
         */
        @JvmStatic
        @OptIn(PartiQLValueExperimental::class)
        internal fun Rex.toPath(steps: List<BindingName>): Rex = steps.fold(this) { curr, step ->
            val op = when (step.case) {
                BindingCase.SENSITIVE -> rexOpPathKey(curr, rex(StaticType.STRING, rexOpLit(stringValue(step.name))))
                BindingCase.INSENSITIVE -> rexOpPathSymbol(curr, step.name)
            }
            rex(StaticType.ANY, op)
        }
    }
}
