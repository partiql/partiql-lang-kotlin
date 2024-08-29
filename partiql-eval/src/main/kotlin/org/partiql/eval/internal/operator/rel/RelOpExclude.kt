package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.IteratorSupplier
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.eval.value.Field
import org.partiql.plan.v1.operator.rel.RelExcludeCollectionWildcard
import org.partiql.plan.v1.operator.rel.RelExcludeIndex
import org.partiql.plan.v1.operator.rel.RelExcludeKey
import org.partiql.plan.v1.operator.rel.RelExcludePath
import org.partiql.plan.v1.operator.rel.RelExcludeStep
import org.partiql.plan.v1.operator.rel.RelExcludeStructWildcard
import org.partiql.plan.v1.operator.rel.RelExcludeSymbol
import org.partiql.types.PType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueType

internal class RelOpExclude(
    private val input: Operator.Relation,
    private val exclusions: List<RelExcludePath>,
) : Operator.Relation {

    override fun open(env: Environment) {
        input.open(env)
    }

    override fun hasNext(): Boolean {
        return input.hasNext()
    }

    override fun next(): Record {
        val record = input.next()
        exclusions.forEach { path ->
            val o = path.getRoot().getOffset()
            val value = record.values[o]
            record.values[o] = excludeValue(value, path.getSteps().toList())
        }
        return record
    }

    override fun close() {
        input.close()
    }

    private fun excludeFields(
        structValue: Datum,
        exclusions: List<RelExcludeStep>,
    ): Datum {
        val structSymbolsToRemove = mutableSetOf<String>()
        val structKeysToRemove = mutableSetOf<String>() // keys stored as lowercase strings
        val branches = mutableMapOf<RelExcludeStep, List<RelExcludeStep>>()
        exclusions.forEach { exclusion ->
            val substeps = exclusion.getSubsteps()
            when (substeps.isEmpty()) {
                true -> {
                    when (exclusion) {
                        is RelExcludeStructWildcard -> {
                            // struct wildcard at current level. return empty struct
                            return Datum.struct(emptyList())
                        }
                        is RelExcludeSymbol -> structSymbolsToRemove.add(exclusion.getSymbol())
                        is RelExcludeKey -> structKeysToRemove.add(exclusion.getKey().lowercase())
                        else -> { /* coll step; do nothing */
                        }
                    }
                }
                false -> {
                    when (exclusion) {
                        is RelExcludeStructWildcard,
                        is RelExcludeSymbol,
                        is RelExcludeKey,
                        -> {
                            branches[exclusion] = exclusion.getSubsteps()
                        }
                        else -> { /* coll step; do nothing */
                        }
                    }
                }
            }
        }
        val structSupplier = IteratorSupplier { structValue.fields }
        val finalStruct = structSupplier.mapNotNull { structField ->
            if (structSymbolsToRemove.contains(structField.name) || structKeysToRemove.contains(structField.name.lowercase())) {
                // struct attr is to be removed at current level
                null
            } else {
                // deeper level exclusions
                val name = structField.name
                var value = structField.value
                // apply struct key exclusions at deeper levels
                val structKey = RelExcludeStep.key(name)
                branches[structKey]?.let {
                    value = excludeValue(value, it)
                }
                // apply struct symbol exclusions at deeper levels
                val structSymbol = RelExcludeStep.symbol(name)
                branches[structSymbol]?.let {
                    value = excludeValue(value, it)
                }
                // apply struct wildcard exclusions at deeper levels
                val structWildcard = RelExcludeStep.struct()
                branches[structWildcard]?.let {
                    value = excludeValue(value, it)
                }
                Pair(name, value)
            }
        }.map { Field.of(it.first, it.second) }
        return Datum.struct(finalStruct)
    }

    /**
     * Returns a [PartiQLValue] created from an iterable of [coll]. Requires [type] to be a collection type
     * (i.e. [PartiQLValueType.LIST], [PartiQLValueType.BAG], or [PartiQLValueType.SEXP]).
     */
    private fun newCollValue(type: PType, coll: Iterable<Datum>): Datum {
        return when (type.kind) {
            PType.Kind.ARRAY -> Datum.list(coll)
            PType.Kind.BAG -> Datum.bag(coll)
            PType.Kind.SEXP -> Datum.sexp(coll)
            else -> error("Collection type required")
        }
    }

    private fun excludeCollection(
        coll: Iterable<Datum>,
        type: PType,
        exclusions: List<RelExcludeStep>,
    ): Datum {
        val indexesToRemove = mutableSetOf<Int>()
        val branches = mutableMapOf<RelExcludeStep, List<RelExcludeStep>>()
        exclusions.forEach { exclusion ->
            val substeps = exclusion.getSubsteps()
            when (substeps.isEmpty()) {
                true -> {
                    when (exclusion) {
                        is RelExcludeCollectionWildcard -> {
                            // collection wildcard at current level. return empty collection
                            return newCollValue(type, emptyList())
                        }
                        is RelExcludeIndex -> {
                            indexesToRemove.add(exclusion.getIndex())
                        }
                        else -> { /* struct step; do nothing */
                        }
                    }
                }
                false -> {
                    when (exclusion) {
                        is RelExcludeCollectionWildcard,
                        is RelExcludeIndex,
                        -> {
                            branches[exclusion] = exclusion.getSubsteps()
                        }
                        else -> { /* struct step; do nothing */
                        }
                    }
                }
            }
        }
        val finalColl = coll.mapIndexedNotNull { index, element ->
            if (indexesToRemove.contains(index)) {
                // coll index is to be removed at current level
                null
            } else {
                // deeper level exclusions
                var value = element
                if (type.kind == PType.Kind.ARRAY || type.kind == PType.Kind.SEXP) {
                    // apply collection index exclusions at deeper levels for lists and sexps
                    val collIndex = RelExcludeStep.index(index)
                    branches[collIndex]?.let {
                        value = excludeValue(element, it)
                    }
                }
                // apply collection wildcard exclusions at deeper levels for lists, bags, and sexps
                val collWildcard = RelExcludeStep.collection()
                branches[collWildcard]?.let {
                    value = excludeValue(value, it)
                }
                value
            }
        }
        return newCollValue(type, finalColl)
    }

    private fun excludeValue(initialPartiQLValue: Datum, exclusions: List<RelExcludeStep>): Datum {
        return when (initialPartiQLValue.type.kind) {
            PType.Kind.ROW, PType.Kind.STRUCT -> excludeFields(initialPartiQLValue, exclusions)
            PType.Kind.BAG, PType.Kind.ARRAY, PType.Kind.SEXP -> excludeCollection(
                initialPartiQLValue,
                initialPartiQLValue.type,
                exclusions
            )
            else -> {
                initialPartiQLValue
            }
        }
    }
}
