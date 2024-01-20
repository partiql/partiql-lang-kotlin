package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.ir.rexOpVarResolved
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

/**
 * PartiQL Planner Global Environment of Catalogs backed by given plugins.
 *
 * @property session    Session details
 */
internal class Env(private val session: PartiQLPlanner.Session) {

    /**
     * Collect the list of all referenced catalog symbols during planning.
     */
    private val catalogs = mutableListOf<Catalog>()

    @OptIn(FnExperimental::class)
    public fun resolve(fn: Fn.Unresolved): FnSignature {
        TODO()
    }

    /**
     * The PlanTyper
     *
     * @param path
     * @param locals
     * @param strategy
     * @return
     */
    public fun resolve(path: BindingPath, locals: TypeEnv, strategy: ResolutionStrategy): Rex? = when (strategy) {
        ResolutionStrategy.LOCAL -> local(path, locals) ?: global(path)
        ResolutionStrategy.GLOBAL -> global(path) ?: local(path, locals)
    }

    /**
     * We resolve a local with the following rules.
     *
     *  1) Check if the path root unambiguously matches a local variable name, set as root.
     *  2) Else, check if path root unambiguously matches a struct field in a local value struct.
     *
     * Convert any remaining binding names (tail) to a path expression.
     *
     * @param path
     * @param locals
     * @return
     */
    private fun local(path: BindingPath, locals: TypeEnv): Rex? {
        val head: BindingName = path.steps[0]
        var tail: List<BindingName> = path.steps.drop(1)

        // 1) Check locals
        var r: Rex? = null
        for (i in locals.indices) {
            val local = locals[i]
            val type = local.type
            if (head.matches(local.name)) {
                if (r != null) {
                    // TODO root was already matched, emit ambiguous error.
                    return null
                }
                r = rex(type, rexOpVarResolved(i))
            }
        }

        // 2) Check struct fields
        if (r == null) {
            var c: Rex? = null
            var known = false
            for (i in locals.indices) {
                val local = locals[i]
                val type = local.type
                if (type is StructType) {
                    when (type.containsKey(head)) {
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
            if (c == null) {
                return null
            }
            r = c
            tail = path.steps
        }

        // Convert any remaining binding names (tail) to an untyped path expression.
        return if (tail.isEmpty()) r else r.toPath(tail)
    }

    private fun global(path: BindingPath): Rex? {
        TODO()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun Rex.toPath(steps: List<BindingName>): Rex = steps.fold(this) { curr, step ->
        val op = when (step.case) {
            BindingCase.SENSITIVE -> rexOpPathKey(curr, rex(StaticType.STRING, rexOpLit(stringValue(step.name))))
            BindingCase.INSENSITIVE -> rexOpPathSymbol(curr, step.name)
        }
        rex(StaticType.ANY, op)
    }

    /**
     * Searches for the [BindingName] withing the given [StructType].
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
