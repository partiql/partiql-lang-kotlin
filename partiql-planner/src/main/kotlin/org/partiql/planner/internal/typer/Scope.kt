package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.ir.rexOpVarLocal
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Represents local variable scopes.
 *
 * @property outer refers to the outer variable scopes that we have access to.
 */
internal data class Scope(
    val schema: List<Rel.Binding>,
    val outer: List<Scope>,
    val withElements: List<Rel.Op.With.WithListElement> = emptyList(),
    /**
     * True if this scope binds the common table expressions of a WITH clause, rather than the columns of a row.
     */
    val isWith: Boolean = false,
    /**
     * Non-null if this is the scope of an inlined WITH list element; see [Boundary].
     */
    val boundary: Boundary? = null,
) {

    /**
     * Records whether the definition of an inlined WITH list element references a variable of an enclosing scope (an
     * outer reference). An inlined definition is re-typed where the element is referenced, where such a variable may
     * not be in the same scope; therefore, this is not supported. References to enclosing WITH list elements are
     * themselves inlined, and are not outer references.
     */
    internal class Boundary {
        var outerReference: Boolean = false
    }

    /**
     * Resolves using the enclosing scope, and records a resolved variable of the enclosing scope in [boundary].
     */
    private inline fun resolveOuter(resolve: (Scope) -> Rex?): Rex? {
        val r = resolve(outer.last())
        if (r != null && boundary != null && r.op is Rex.Op.Var.Local) {
            boundary.outerReference = true
        }
        return r
    }

    internal fun getScope(depth: Int): Scope {
        return when (depth) {
            0 -> this
            else -> outer[outer.size - depth]
        }
    }

    /**
     * Attempts to resolve using just the local binding name.
     */
    fun resolveName(identifier: Identifier): Rex? {
        val path = identifier.toList()
        val head = path.first()
        val tail = path.drop(1)
        // Try exact match first
        matchRoot(head)?.let { r ->
            return if (tail.isEmpty()) r else r.toPath(tail)
        }
        // Try qualified match only if exact match fails and we have multiple parts
        return if (path.size >= 2) matchQualified(identifier) else null
    }

    /**
     * Check if the path root unambiguously matches a local binding struct value field.
     * Convert any remaining binding names (tail) to a path expression.
     */
    fun resolveField(identifier: Identifier): Rex? {
        val head = identifier.first()
        val tail = identifier.toList()
        // Match first identifier to a struct field
        val r = matchStruct(head) ?: return null
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
     * Check if qualified identifier matches a binding with qualifier and name.
     */
    private fun matchQualified(identifier: Identifier, depth: Int = 0): Rex? {
        val path = identifier.toList()
        val qualifierParts = path.dropLast(1)
        val bindingName = path.last()
        for (i in schema.indices) {
            val local = schema[i]
            val type = local.type
            if (local.qualifier == null) continue
            if (!bindingName.matches(local.name)) continue
            val localQualifierParts = local.qualifier
            if (qualifierParts.size != localQualifierParts.size) continue
            if (!qualifierParts.zip(localQualifierParts).all { (identPart, localPart) ->
                identPart.matches(localPart.getText())
            }
            ) continue
            return rex(type, rexOpVarLocal(depth, i))
        }
        return if (outer.isNotEmpty()) resolveOuter { it.matchQualified(identifier, depth + 1) } else null
    }

    /**
     * Check if `name` unambiguously matches a local binding name and return its reference; otherwise return null.
     *
     * @param name
     * @return
     */
    private fun matchRoot(name: Identifier.Simple, depth: Int = 0): Rex? {
        var r: Rex? = null
        for (i in withElements.indices) {
            val withElement = withElements[i]
            if (name.matches(withElement.name)) {
                return withElement.representation
            }
        }
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
            return resolveOuter { it.matchRoot(name, depth + 1) }
        }
        return r
    }

    /**
     * Check if `name` unambiguously matches a field within a struct and return its reference; otherwise return null.
     *
     * @param name
     * @return
     */
    private fun matchStruct(name: Identifier.Simple, depth: Int = 0): Rex? {
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
            return resolveOuter { it.matchStruct(name, depth + 1) }
        }
        return c
    }

    /**
     * Searches for the [Identifier.Simple] within the given [CompilerType].
     *
     * Returns
     *  - true  iff known to contain key
     *  - false iff known to NOT contain key
     *  - null  iff NOT known to contain key
     *
     * @param name
     * @return
     */
    private fun CompilerType.containsKey(name: Identifier.Simple): Boolean? {
        return when (this.code()) {
            PType.ROW -> this.fields.any { name.matches(it.name) }
            PType.STRUCT -> null
            PType.DYNAMIC -> null
            PType.VARIANT -> null
            else -> false
        }
    }

    companion object {

        /**
         * Converts a list of [Identifier.Simple] to a path expression.
         *
         *  1) Case SENSITIVE identifiers become string literal key lookups.
         *  2) Case INSENSITIVE identifiers become symbol lookups.
         *
         * @param parts
         * @return
         */
        @JvmStatic
        internal fun Rex.toPath(parts: List<Identifier.Simple>): Rex = parts.fold(this) { curr, part ->
            val type = PType.dynamic().toCType()
            val text = part.getText()
            val op = when (part.isRegular()) {
                true -> rexOpPathSymbol(curr, text)
                else -> rexOpPathKey(curr, string(text))
            }
            rex(type, op)
        }

        private fun string(text: String) = rex(CompilerType(PType.string()), rexOpLit(Datum.string(text)))
    }
}
