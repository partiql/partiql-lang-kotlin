package org.partiql.planner.internal.exclude

import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.relOpExcludeStep
import org.partiql.planner.internal.ir.relOpExcludeTypeCollWildcard
import org.partiql.planner.internal.ir.relOpExcludeTypeStructSymbol
import org.partiql.planner.internal.ir.relOpExcludeTypeStructWildcard

/**
 * Store as a map of exclude type to its substeps; previous representation as a set of `Rel.Op.Exclude.Step` didn't
 * work since retrieval of element in set required looping through the whole list.
 */
internal class ExcludeRepr(val steps: Map<Rel.Op.Exclude.Type, ExcludeRepr>) {
    companion object {
        fun toExcludeRepr(excludeSteps: List<Rel.Op.Exclude.Step>): ExcludeRepr {
            return ExcludeRepr(
                excludeSteps.associate { e ->
                    e.type to toExcludeRepr(e.substeps)
                }.toMap()
            )
        }
    }

    internal fun toPlanRepr(): List<Rel.Op.Exclude.Step> {
        return steps.map { (type, substeps) ->
            relOpExcludeStep(type, substeps.toPlanRepr())
        }
    }

    internal fun removeRedundantSteps(): ExcludeRepr {
        val newSubsteps = mutableMapOf<Rel.Op.Exclude.Type, ExcludeRepr>()
        val collIndexSteps = mutableMapOf<Rel.Op.Exclude.Type, ExcludeRepr>()
        val structSymbolSteps = mutableMapOf<Rel.Op.Exclude.Type, ExcludeRepr>()
        val structKeySteps = mutableMapOf<Rel.Op.Exclude.Type, ExcludeRepr>()
        // Group the steps by their type and add wildcards to `substeps`
        steps.forEach { (type, substeps) ->
            when (type) {
                is Rel.Op.Exclude.Type.StructWildcard -> {
                    // Insert struct wildcard into substeps
                    newSubsteps[relOpExcludeTypeStructWildcard()] = substeps.removeRedundantSteps()
                }
                is Rel.Op.Exclude.Type.CollWildcard -> {
                    // Insert coll wildcard into substeps
                    newSubsteps[relOpExcludeTypeCollWildcard()] = substeps.removeRedundantSteps()
                }
                is Rel.Op.Exclude.Type.CollIndex -> collIndexSteps[type] = substeps.removeRedundantSteps()
                is Rel.Op.Exclude.Type.StructSymbol -> structSymbolSteps[type] = substeps.removeRedundantSteps()
                is Rel.Op.Exclude.Type.StructKey -> structKeySteps[type] = substeps.removeRedundantSteps()
            }
        }
        // Next add non-wildcard steps
        stepsSubsume(newSubsteps, collIndexSteps)?.let {
            newSubsteps.putAll(it)
        }
        stepsSubsume(newSubsteps, structSymbolSteps)?.let {
            newSubsteps.putAll(it)
        }
        stepsSubsume(newSubsteps, structKeySteps)?.let {
            newSubsteps.putAll(it)
        }
        return ExcludeRepr(newSubsteps)
    }
}

private fun stepsSubsume(lhs: Map<Rel.Op.Exclude.Type, ExcludeRepr>, rhs: Map<Rel.Op.Exclude.Type, ExcludeRepr>?): Map<Rel.Op.Exclude.Type, ExcludeRepr>? {
    if (rhs == null) {
        return null
    }
    val result = rhs.mapNotNull { (type, substeps) ->
        var newSubsteps: Map<Rel.Op.Exclude.Type, ExcludeRepr>? = substeps.steps
        when (type) {
            is Rel.Op.Exclude.Type.CollWildcard -> {
                val lhsCollWildcard = lhs[relOpExcludeTypeCollWildcard()]
                if (lhsCollWildcard != null) {
                    newSubsteps = if (lhsCollWildcard.steps.isEmpty()) {
                        // coll wildcard leaf in lhs
                        null
                    } else {
                        stepsSubsume(lhsCollWildcard.steps, newSubsteps)
                    }
                }
            }
            is Rel.Op.Exclude.Type.CollIndex -> {
                val lhsCollWildcard = lhs[relOpExcludeTypeCollWildcard()]
                if (lhsCollWildcard != null) {
                    newSubsteps = if (lhsCollWildcard.steps.isEmpty()) {
                        // coll wildcard leaf in lhs
                        null
                    } else {
                        stepsSubsume(lhsCollWildcard.steps, newSubsteps)
                    }
                }
                val lhsCollIndex = lhs[type]
                if (lhsCollIndex != null) {
                    newSubsteps = if (lhsCollIndex.steps.isEmpty()) {
                        // coll index leaf in lhs
                        null
                    } else {
                        stepsSubsume(lhsCollIndex.steps, newSubsteps)
                    }
                }
            }
            is Rel.Op.Exclude.Type.StructWildcard -> {
                val lhsStructWildcard = lhs[relOpExcludeTypeStructWildcard()]
                if (lhsStructWildcard != null) {
                    newSubsteps = if (lhsStructWildcard.steps.isEmpty()) {
                        // struct wildcard leaf in lhs
                        null
                    } else {
                        stepsSubsume(lhsStructWildcard.steps, newSubsteps)
                    }
                }
            }
            is Rel.Op.Exclude.Type.StructSymbol -> {
                val lhsStructWildcard = lhs[relOpExcludeTypeStructWildcard()]
                if (lhsStructWildcard != null) {
                    newSubsteps = if (lhsStructWildcard.steps.isEmpty()) {
                        // struct wildcard leaf in lhs
                        null
                    } else {
                        stepsSubsume(lhsStructWildcard.steps, newSubsteps)
                    }
                }
                val lhsStructSymbol = lhs[type]
                if (lhsStructSymbol != null) {
                    newSubsteps = if (lhsStructSymbol.steps.isEmpty()) {
                        // struct symbol leaf in lhs
                        null
                    } else {
                        stepsSubsume(lhsStructSymbol.steps, newSubsteps)
                    }
                }
            }
            is Rel.Op.Exclude.Type.StructKey -> {
                val lhsStructWildcard = lhs[relOpExcludeTypeStructWildcard()]
                if (lhsStructWildcard != null) {
                    newSubsteps = if (lhsStructWildcard.steps.isEmpty()) {
                        // struct wildcard leaf in lhs
                        null
                    } else {
                        stepsSubsume(lhsStructWildcard.steps, newSubsteps)
                    }
                }
                val keyAsSymbol = relOpExcludeTypeStructSymbol(type.key)
                val lhsStructSymbol = lhs[keyAsSymbol]
                if (lhsStructSymbol != null) {
                    newSubsteps = if (lhsStructSymbol.steps.isEmpty()) {
                        // struct symbol leaf in lhs
                        null
                    } else {
                        stepsSubsume(lhsStructSymbol.steps, newSubsteps)
                    }
                }
                val lhsStructKey = lhs[type]
                if (lhsStructKey != null) {
                    newSubsteps = if (lhsStructKey.steps.isEmpty()) {
                        // struct key leaf in lhs
                        null
                    } else {
                        stepsSubsume(lhsStructKey.steps, newSubsteps)
                    }
                }
            }
        }
        when (newSubsteps) {
            null -> null
            else -> type to ExcludeRepr(newSubsteps)
        }
    }.toMap()
    return when (result.isEmpty()) {
        true -> null
        else -> result
    }
}
