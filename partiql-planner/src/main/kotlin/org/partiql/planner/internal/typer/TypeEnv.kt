package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.ir.rexOpVarResolved
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
 */
internal class TypeEnv(public val schema: List<Rel.Binding>) {

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
        var r = matchRoot(head)
        if (r == null) {
            r = matchStruct(head) ?: return null
            tail = path.steps
        }
        // Convert any remaining binding names (tail) to an untyped path expression.
        return if (tail.isEmpty()) r else r.toPath(tail)
    }

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
    private fun matchRoot(name: BindingName): Rex? {
        var r: Rex? = null
        for (i in schema.indices) {
            val local = schema[i]
            val type = local.type
            if (name.matches(local.name)) {
                if (r != null) {
                    // TODO root was already matched, emit ambiguous error.
                    return null
                }
                r = rex(type, rexOpVarResolved(i))
            }
        }
        return r
    }

    /**
     * Check if `name` unambiguously matches a field within a struct and return its reference; otherwise return null.
     *
     * @param name
     * @return
     */
    private fun matchStruct(name: BindingName): Rex? {
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
                        c = rex(type, rexOpVarResolved(i))
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
                        c = rex(type, rexOpVarResolved(i))
                        known = false
                    }
                    false -> continue
                }
            }
        }
        return c
    }

    /**
     * Converts a list of [BindingName] to a path expression.
     *
     *  1) Case SENSITIVE identifiers become string literal key lookups.
     *  2) Case INSENSITIVE identifiers become symbol lookups.
     *
     * @param steps
     * @return
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun Rex.toPath(steps: List<BindingName>): Rex = steps.fold(this) { curr, step ->
        val op = when (step.case) {
            BindingCase.SENSITIVE -> rexOpPathKey(curr, rex(StaticType.STRING, rexOpLit(stringValue(step.name))))
            BindingCase.INSENSITIVE -> rexOpPathSymbol(curr, step.name)
        }
        rex(StaticType.ANY, op)
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
}
