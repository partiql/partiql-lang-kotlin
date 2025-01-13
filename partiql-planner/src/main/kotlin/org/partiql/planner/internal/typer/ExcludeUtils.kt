package org.partiql.planner.internal.typer

import org.partiql.planner.internal.PErrors
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.types.PType

internal object ExcludeUtils {
    /**
     * Checks for every exclude path in [paths], that a value in the [bindings] is excluded. If there is no value excluded,
     * a warning is added to the [onProblem] callback.
     */
    internal fun checkForInvalidExcludePaths(bindings: List<Rel.Binding>, paths: List<Rel.Op.Exclude.Path>, onProblem: PErrorListener) {
        paths.forEach { excludePath ->
            val root = excludePath.root
            val steps = excludePath.steps
            val excludesSomething = bindings.any { binding ->
                when (root) {
                    is Rex.Op.Var.Unresolved -> {
                        if (root.identifier.first().matches(binding.name)) {
                            binding.type.checkExclude(steps)
                        } else {
                            false
                        }
                    }
                    else -> false // root should be unresolved
                }
            }
            // If nothing is excluded by `excludePath`, add a warning
            if (!excludesSomething) {
                onProblem.report(PErrors.invalidExcludePath(excludePath.toProblemString()))
            }
        }
    }

    /**
     * Checks whether [steps] will exclude a value from [this].
     */
    private fun PType.checkExclude(steps: List<Rel.Op.Exclude.Step>): Boolean {
        return when (this.code()) {
            PType.ROW -> this.checkRowExclude(steps)
            PType.ARRAY, PType.BAG -> this.checkCollectionExclude(steps)
            PType.DYNAMIC, PType.VARIANT -> true
            else -> steps.isEmpty()
        }
    }

    /**
     * Checks whether [steps] will exclude a value from [this] [PType.ROW].
     */
    private fun PType.checkRowExclude(steps: List<Rel.Op.Exclude.Step>): Boolean {
        // Ignore open structs
        if (steps.isEmpty()) {
            return true
        }
        return steps.all { step ->
            fields.any { field ->
                when (val type = step.type) {
                    is Rel.Op.Exclude.Type.StructSymbol -> {
                        Identifier.regular(type.symbol).first().matches(field.name) && field.type.checkExclude(step.substeps)
                    }
                    is Rel.Op.Exclude.Type.StructKey -> {
                        type.key == field.name && field.type.checkExclude(step.substeps)
                    }
                    is Rel.Op.Exclude.Type.StructWildcard -> field.type.checkExclude(step.substeps)
                    else -> false
                }
            }
        }
    }

    /**
     * Checks whether [steps] will exclude a value from [this] [PType.BAG]/[PType.ARRAY].
     */
    private fun PType.checkCollectionExclude(steps: List<Rel.Op.Exclude.Step>): Boolean {
        if (steps.isEmpty()) {
            return true
        }
        return steps.all { step ->
            when (step.type) {
                is Rel.Op.Exclude.Type.CollIndex, is Rel.Op.Exclude.Type.CollWildcard -> {
                    val e = this.typeParameter
                    e.checkExclude(step.substeps)
                }
                else -> false
            }
        }
    }

    // `EXCLUDE` path printing functions for problem printing
    private fun Rel.Op.Exclude.Path.toProblemString(): String {
        val root = when (root) {
            is Rex.Op.Var.Unresolved -> root.identifier.toProblemString()
            is Rex.Op.Var.Local -> root.ref.toString()
            is Rex.Op.Var.Global -> root.ref.toString()
            else -> error("This isn't supported.")
        }
        val steps = steps.map {
            when (val type = it.type) {
                is Rel.Op.Exclude.Type.CollIndex -> "[${type.index}]"
                is Rel.Op.Exclude.Type.CollWildcard -> "[*]"
                is Rel.Op.Exclude.Type.StructSymbol -> ".${type.symbol}"
                is Rel.Op.Exclude.Type.StructKey -> ".\"${type.key}\""
                is Rel.Op.Exclude.Type.StructWildcard -> ".*"
            }
        }
        return root + steps.joinToString(separator = "")
    }

    private fun Identifier.toProblemString(): String {
        return this.joinToString("") { it.toProblemString() }
    }

    private fun Identifier.Simple.toProblemString(): String {
        return when (this.isRegular()) {
            false -> "\"${getText()}\""
            true -> getText()
        }
    }
}
