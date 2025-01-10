package org.partiql.planner.internal.util

import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.spi.types.PType

internal object TypeUtils {
    /**
     * Applies the given exclusion path to produce the reduced [CompilerType]. [lastStepOptional] indicates if a previous
     * step in the exclude path includes a collection index exclude step. Currently, for paths with the last step as
     * a struct symbol/key, the type inference will define that struct value as optional if [lastStepOptional] is true.
     * Note, this specific behavior could change depending on `EXCLUDE`'s static typing behavior in a future RFC.
     *
     * e.g. EXCLUDE t.a[1].field_x will define the struct value `field_x` as optional
     *
     * @param steps
     * @param lastStepOptional
     * @return
     */
    internal fun CompilerType.exclude(steps: List<Rel.Op.Exclude.Step>, lastStepOptional: Boolean = false): CompilerType {
        val type = this
        return steps.fold(type) { acc, step ->
            when (acc.code()) {
                PType.DYNAMIC -> CompilerType(PType.dynamic())
                PType.ROW -> acc.excludeStruct(step, lastStepOptional)
                PType.STRUCT -> acc
                PType.ARRAY, PType.BAG -> acc.excludeCollection(step, lastStepOptional)
                else -> acc
            }
        }
    }

    /**
     * Applies exclusions to struct fields.
     *
     * @param step
     * @param lastStepOptional
     * @return
     */
    internal fun CompilerType.excludeStruct(step: Rel.Op.Exclude.Step, lastStepOptional: Boolean = false): CompilerType {
        val type = step.type
        val substeps = step.substeps
        val output = fields.mapNotNull { field ->
            val newField = if (substeps.isEmpty()) {
                if (lastStepOptional) {
                    CompilerType.Field(field.name, field.type)
                } else {
                    null
                }
            } else {
                val k = field.name
                val v = field.type.exclude(substeps, lastStepOptional)
                CompilerType.Field(k, v)
            }
            when (type) {
                is Rel.Op.Exclude.Type.StructSymbol -> {
                    if (type.symbol.equals(field.name, ignoreCase = true)) {
                        newField
                    } else {
                        field
                    }
                }

                is Rel.Op.Exclude.Type.StructKey -> {
                    if (type.key == field.name) {
                        newField
                    } else {
                        field
                    }
                }
                is Rel.Op.Exclude.Type.StructWildcard -> newField
                else -> field
            }
        }
        return CompilerType(PType.row(output))
    }

    /**
     * Applies exclusions to collection element type.
     *
     * @param step
     * @param lastStepOptional
     * @return
     */
    internal fun CompilerType.excludeCollection(step: Rel.Op.Exclude.Step, lastStepOptional: Boolean = false): CompilerType {
        var e = this.typeParameter
        val substeps = step.substeps
        when (step.type) {
            is Rel.Op.Exclude.Type.CollIndex -> {
                if (substeps.isNotEmpty()) {
                    e = e.exclude(substeps, lastStepOptional = true)
                }
            }

            is Rel.Op.Exclude.Type.CollWildcard -> {
                if (substeps.isNotEmpty()) {
                    e = e.exclude(substeps, lastStepOptional)
                }
                // currently no change to elementType if collection wildcard is last element; this behavior could
                // change based on RFC definition
            }

            else -> {
                // currently no change to elementType and no error thrown; could consider an error/warning in
                // the future
            }
        }
        return when (this.code()) {
            PType.ARRAY -> PType.array(e).toCType()
            PType.BAG -> PType.bag(e).toCType()
            else -> throw IllegalStateException()
        }
    }
}
