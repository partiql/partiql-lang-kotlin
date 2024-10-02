package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.IteratorSupplier
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.ExcludeCollectionWildcard
import org.partiql.plan.ExcludeIndex
import org.partiql.plan.ExcludeKey
import org.partiql.plan.ExcludePath
import org.partiql.plan.ExcludeStep
import org.partiql.plan.ExcludeStructWildcard
import org.partiql.plan.ExcludeSymbol
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.types.PType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueType

/**
 * TODO THERE ARE BUGS IN THIS IMPLEMENTATION POSSIBLY DUE TO HASHCODE/EQUALS OF [ExcludePath].
 */
internal class RelOpExclude(
    private val input: Operator.Relation,
    private val exclusions: List<ExcludePath>,
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
        exclusions: List<ExcludeStep>,
    ): Datum {
        val structSymbolsToRemove = mutableSetOf<String>()
        val structKeysToRemove = mutableSetOf<String>() // keys stored as lowercase strings
        val branches = mutableMapOf<ExcludeStep, List<ExcludeStep>>()
        exclusions.forEach { exclusion ->
            val substeps = exclusion.getSubsteps()
            when (substeps.isEmpty()) {
                true -> {
                    when (exclusion) {
                        is ExcludeStructWildcard -> {
                            // struct wildcard at current level. return empty struct
                            return Datum.struct(emptyList())
                        }
                        is ExcludeSymbol -> structSymbolsToRemove.add(exclusion.getSymbol())
                        is ExcludeKey -> structKeysToRemove.add(exclusion.getKey().lowercase())
                        else -> { /* coll step; do nothing */
                        }
                    }
                }
                false -> {
                    when (exclusion) {
                        is ExcludeStructWildcard,
                        is ExcludeSymbol,
                        is ExcludeKey,
                        -> {
                            branches[exclusion] = exclusion.getSubsteps().toList()
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
                val structKey = ExcludeStep.key(name)
                branches[structKey]?.let {
                    value = excludeValue(value, it)
                }
                // apply struct symbol exclusions at deeper levels
                val structSymbol = ExcludeStep.symbol(name)
                branches[structSymbol]?.let {
                    value = excludeValue(value, it)
                }
                // apply struct wildcard exclusions at deeper levels
                val structWildcard = ExcludeStep.struct()
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
        exclusions: List<ExcludeStep>,
    ): Datum {
        val indexesToRemove = mutableSetOf<Int>()
        val branches = mutableMapOf<ExcludeStep, List<ExcludeStep>>()
        exclusions.forEach { exclusion ->
            val substeps = exclusion.getSubsteps()
            when (substeps.isEmpty()) {
                true -> {
                    when (exclusion) {
                        is ExcludeCollectionWildcard -> {
                            // collection wildcard at current level. return empty collection
                            return newCollValue(type, emptyList())
                        }
                        is ExcludeIndex -> {
                            indexesToRemove.add(exclusion.getIndex())
                        }
                        else -> { /* struct step; do nothing */
                        }
                    }
                }
                false -> {
                    when (exclusion) {
                        is ExcludeCollectionWildcard,
                        is ExcludeIndex,
                        -> {
                            branches[exclusion] = exclusion.getSubsteps().toList()
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
                    val collIndex = ExcludeStep.index(index)
                    branches[collIndex]?.let {
                        value = excludeValue(element, it)
                    }
                }
                // apply collection wildcard exclusions at deeper levels for lists, bags, and sexps
                val collWildcard = ExcludeStep.collection()
                branches[collWildcard]?.let {
                    value = excludeValue(value, it)
                }
                value
            }
        }
        return newCollValue(type, finalColl)
    }

    private fun excludeValue(initialPartiQLValue: Datum, exclusions: List<ExcludeStep>): Datum {
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
