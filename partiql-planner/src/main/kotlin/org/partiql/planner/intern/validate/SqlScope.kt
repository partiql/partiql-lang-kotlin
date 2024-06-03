package org.partiql.planner.intern.validate

import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

/**
 * SqlBindings represents a local variables environment.
 *
 * Previously TypeEnv.
 */
internal class SqlScope(
    private val parent: SqlScope?,
    private val bindings: List<SqlBinding>,
) {

    /**
     * TODO remove me once IR modeling is improved.
     */
    fun concat(relType: Rel.Type): SqlScope = SqlScope(
        parent = this,
        bindings = relType.schema.mapIndexed { i, attr ->
            SqlBinding(
                name = attr.name,
                type = attr.type,
                ordinal = i + bindings.size,
            )
        }
    )

    /**
     * Concatenate bindings to form a new scope.
     */
    fun concat(vararg attrs: Pair<String, StaticType>): SqlScope = SqlScope(
        parent = this,
        bindings = attrs.mapIndexed { i, attr ->
            SqlBinding(
                name = attr.first,
                type = attr.second,
                ordinal = i + bindings.size,
            )
        }
    )

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
     * Resolve qualified-identifier variables; any remaining steps become path expressions.
     */
    fun resolve(id: Identifier.Qualified): Rex? = resolve(id)?.toPath(id.steps)

    /**
     * Check if id unambiguously matches a local binding name and return it; otherwise return null.
     */
    private fun match(id: Identifier.Symbol): Rex? {
        var match: Rex? = null
        for (binding in bindings) {
            if (binding.matches(id)) {
                if (match != null) {
                    // TODO root was already matched, emit ambiguous error.
                    return null
                }
                match = binding.reference()
            }
        }
        if (match == null && parent != null) {
            return parent.resolve(id)
        }
        return match
    }

    /**
     * Check if id unambiguously matches a field _within_ a local binding and return it; otherwise return null.
     */
    private fun search(id: Identifier.Symbol): Rex? {
        var match: Rex? = null
        var known = false
        for (binding in bindings) {
            when (binding.contains(id)) {
                true -> {
                    if (match != null && known) {
                        // TODO root was already definitively matched, emit ambiguous error.
                        return null
                    }
                    match = binding.reference()
                    known = true
                }
                null -> {
                    if (match != null) {
                        if (known) {
                            continue
                        } else {
                            // TODO we have more than one possible match, emit ambiguous error.
                            return null
                        }
                    }
                    match = binding.reference()
                    known = false
                }
                false -> continue
            }
        }
        if (match == null && parent != null) {
            return parent.search(id)
        }
        return match
    }

    /**
     * Return an IR node for the variable's type environment.
     */
    fun type(): Rel.Type = Rel.Type(
        schema = bindings.map { it.binding() },
        props = emptySet(),
    )

    /**
     * Return an IR node for the variable's type environment with the given properties.
     */
    fun type(vararg properties: Rel.Prop): Rel.Type = Rel.Type(
        schema = bindings.map { it.binding() },
        props = properties.toSet(),
    )

    /**
     * Debugging string, ex: < x: int, y: string >
     */
    override fun toString(): String = "< " + bindings.joinToString { "${it.name}: ${it.type}" } + " >"

    /**
     * Converts a list of [Identifier.Symbol] to a path expression.
     *
     *  1) Case SENSITIVE identifiers become string literal key lookups.
     *  2) Case INSENSITIVE identifiers become symbol lookups.
     */
    private fun Rex.toPath(steps: List<Identifier.Symbol>): Rex = steps.fold(this) { root, id ->
        val op = when (id.caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> Rex.Op.Path.Key(root, literal(id.symbol))
            Identifier.CaseSensitivity.INSENSITIVE -> Rex.Op.Path.Symbol(root, id.symbol)
        }
        rex(StaticType.ANY, op)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun literal(symbol: String): Rex = Rex(StaticType.STRING, Rex.Op.Lit(stringValue(symbol)))
}
