package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.ir.rexOpVarLocal
import org.partiql.types.PType
import org.partiql.types.PType.Kind
import org.partiql.types.StaticType
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

    internal fun getScope(depth: Int): TypeEnv {
        return when (depth) {
            0 -> this
            else -> outer[outer.size - depth]
        }
    }

    /**
     * We resolve a local with the following rules. See, PartiQL Specification p.35.
     *
     *  1) Check if the path root unambiguously matches a local binding name, set as root.
     *  2) Check if the path root unambiguously matches a local binding struct value field.
     *
     * Convert any remaining binding names (tail) to a path expression.
     */
    fun resolve(id: Identifier): Rex? = when (id) {
        is Identifier.Qualified -> resolve(id)
        is Identifier.Symbol -> resolve(id)
    }

    /**
     * Resolve single-identifier variables.
     *
     *  1) Match  — Check if id unambiguously matches a local binding name.
     *  2) Search — Check if id unambiguously matches a local binding struct field.
     */
    fun resolve(id: Identifier.Symbol): Rex? = match(id) ?: search(id)

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
    fun resolve(path: Identifier.Qualified): Rex? {
        val head: Identifier.Symbol = path.steps[0]
        var tail: List<Identifier.Symbol> = path.steps.drop(1)
        var r = match(head)
        if (r == null) {
            r = search(head) ?: return null
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
    private fun match(name: Identifier.Symbol, depth: Int = 0): Rex? {
        var r: Rex? = null
        for (i in schema.indices) {
            val local = schema[i]
            val type = local.type
            if (name.matches(local.name)) {
                if (r != null) {
                    // TODO root was already matched, emit ambiguous error.
                    return null
                }
                r = rex(type, rexOpVarLocal(depth, i))
            }
        }
        if (r == null && outer.isNotEmpty()) {
            return outer.last().match(name, depth + 1)
        }
        return r
    }

    /**
     * Check if `name` unambiguously matches a field within a struct and return its reference; otherwise return null.
     *
     * @param name
     * @return
     */
    private fun search(name: Identifier.Symbol, depth: Int = 0): Rex? {
        var c: Rex? = null
        var known = false
        for (i in schema.indices) {
            val local = schema[i]
            val type = local.type
            when (type.containsKey(name)) {
                true -> {
                    if (c != null && known) {
                        // TODO root was already definitively matched, emit ambiguous error.
                        return null
                    }
                    c = rex(type, rexOpVarLocal(depth, i))
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
                    c = rex(type, rexOpVarLocal(depth, i))
                    known = false
                }
                false -> continue
            }
        }
        if (c == null && outer.isNotEmpty()) {
            return outer.last().search(name, depth + 1)
        }
        return c
    }

    /**
     * Searches for the [Identifier.Symbol] within the given [StaticType].
     *
     * Returns
     *  - true  iff known to contain key
     *  - false iff known to NOT contain key
     *  - null  iff NOT known to contain key
     *
     * @param name
     * @return
     */
    private fun CompilerType.containsKey(name: Identifier.Symbol): Boolean? {
        return when (this.kind) {
            Kind.ROW -> this.fields.any { name.matches(it.name) }
            Kind.STRUCT -> null
            Kind.DYNAMIC -> null
            else -> false
        }
    }

    companion object {

        /**
         * Converts a list of [Identifier.Symbol] to a path expression.
         *
         *  1) Case SENSITIVE identifiers become string literal key lookups.
         *  2) Case INSENSITIVE identifiers become symbol lookups.
         *
         * @param steps
         * @return
         */
        @JvmStatic
        @OptIn(PartiQLValueExperimental::class)
        internal fun Rex.toPath(steps: List<Identifier.Symbol>): Rex = steps.fold(this) { curr, step ->
            val op = when (step.caseSensitivity) {
                Identifier.CaseSensitivity.SENSITIVE -> rexOpPathKey(curr, rex(CompilerType(PType.typeString()), rexOpLit(stringValue(step.symbol))))
                Identifier.CaseSensitivity.INSENSITIVE -> rexOpPathSymbol(curr, step.symbol)
            }
            rex(CompilerType(PType.typeDynamic()), op)
        }
    }

    /**
     * Compares [Identifier.Symbol] to [target] String using.
     */
    private fun Identifier.Symbol.matches(target: String): Boolean = when (caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE -> target == symbol
        Identifier.CaseSensitivity.INSENSITIVE -> target.equals(symbol, ignoreCase = true)
    }
}
