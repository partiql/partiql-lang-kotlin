package org.partiql.planner.internal.typer

import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.planner.PlanningProblemDetails
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Identifier.CaseSensitivity
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.types.AnyOfType
import org.partiql.types.CollectionType
import org.partiql.types.StaticType
import org.partiql.types.StructType

internal object ExcludeUtils {
    /**
     * Checks for every exclude path in [paths], that a value in the [bindings] is excluded. If there is no value excluded,
     * a warning is added to the [onProblem] callback.
     */
    internal fun checkForInvalidExcludePaths(bindings: List<Rel.Binding>, paths: List<Rel.Op.Exclude.Item>, onProblem: ProblemCallback) {
        paths.forEach { excludePath ->
            val root = excludePath.root
            val steps = excludePath.steps
            val excludesSomething = bindings.any { binding ->
                when (root) {
                    is Rex.Op.Var.Unresolved -> {
                        when (val id = root.identifier) {
                            is Identifier.Symbol -> {
                                if (id.isEquivalentTo(binding.name)) {
                                    binding.type.checkExclude(steps)
                                } else {
                                    false
                                }
                            }
                            is Identifier.Qualified -> {
                                if (id.root.isEquivalentTo(binding.name)) {
                                    binding.type.checkExclude(steps)
                                } else {
                                    false
                                }
                            }
                        }
                    }
                    is Rex.Op.Var.Resolved -> false // root should be unresolved
                }
            }
            // If nothing is excluded by `excludePath`, add a warning
            if (!excludesSomething) {
                onProblem(
                    Problem(
                        sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                        details = PlanningProblemDetails.InvalidExcludePath(
                            excludePath.toProblemString()
                        )
                    )
                )
            }
        }
    }

    /**
     * Checks whether [steps] will exclude a value from [this] [StaticType].
     */
    private fun StaticType.checkExclude(steps: List<Rel.Op.Exclude.Step>): Boolean {
        return when (this) {
            is StructType -> this.checkExclude(steps)
            is CollectionType -> this.checkExclude(steps)
            is AnyOfType -> this.types.any { it.checkExclude(steps) }
            else -> steps.isEmpty()
        }
    }

    /**
     * Checks whether [steps] will exclude a value from [this] [StructType].
     */
    private fun StructType.checkExclude(steps: List<Rel.Op.Exclude.Step>): Boolean {
        // Ignore open structs
        if (steps.isEmpty() || !this.contentClosed) {
            return true
        }
        val step = steps.first()
        return fields.any { field ->
            when (step) {
                is Rel.Op.Exclude.Step.StructField -> {
                    step.symbol.isEquivalentTo(field.key) && field.value.checkExclude(steps.drop(1))
                }
                is Rel.Op.Exclude.Step.StructWildcard -> field.value.checkExclude(steps.drop(1))
                else -> false
            }
        }
    }

    /**
     * Checks whether [steps] will exclude a value from [this] [CollectionType].
     */
    private fun CollectionType.checkExclude(steps: List<Rel.Op.Exclude.Step>): Boolean {
        if (steps.isEmpty()) {
            return true
        }
        return when (steps.first()) {
            is Rel.Op.Exclude.Step.CollIndex, is Rel.Op.Exclude.Step.CollWildcard -> {
                val e = this.elementType
                e.checkExclude(steps.drop(1))
            }
            else -> false
        }
    }

    // `EXCLUDE` path printing functions for problem printing
    private fun Rel.Op.Exclude.Item.toProblemString(): String {
        val root = when (root) {
            is Rex.Op.Var.Resolved -> root.ref.toString()
            is Rex.Op.Var.Unresolved -> root.identifier.toProblemString()
        }
        val steps = steps.map {
            when (it) {
                is Rel.Op.Exclude.Step.CollIndex -> "[${it.index}]"
                is Rel.Op.Exclude.Step.CollWildcard -> "[*]"
                is Rel.Op.Exclude.Step.StructField -> ".${it.symbol.toProblemString()}"
                is Rel.Op.Exclude.Step.StructWildcard -> ".*"
            }
        }
        return root + steps.joinToString(separator = "")
    }

    private fun Identifier.toProblemString(): String {
        return when (val id = this) {
            is Identifier.Symbol -> {
                id.toProblemString()
            }
            is Identifier.Qualified -> {
                val root = id.root.toProblemString()
                val steps = id.steps.map { it.toProblemString() }
                root + steps.joinToString(separator = "")
            }
        }
    }

    private fun Identifier.Symbol.toProblemString(): String {
        return when (this.caseSensitivity) {
            CaseSensitivity.SENSITIVE -> "\"${symbol}\""
            CaseSensitivity.INSENSITIVE -> symbol
        }
    }
}
