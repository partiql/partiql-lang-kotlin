package org.partiql.eval.internal.operator.rel

import org.partiql.eval.PQLValue
import org.partiql.eval.StructField
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.IteratorSupplier
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Rel
import org.partiql.plan.relOpExcludeTypeCollIndex
import org.partiql.plan.relOpExcludeTypeCollWildcard
import org.partiql.plan.relOpExcludeTypeStructKey
import org.partiql.plan.relOpExcludeTypeStructSymbol
import org.partiql.plan.relOpExcludeTypeStructWildcard
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal class RelExclude(
    private val input: Operator.Relation,
    private val exclusions: List<Rel.Op.Exclude.Path>
) : Operator.Relation {

    override fun open(env: Environment) {
        input.open(env)
    }

    override fun hasNext(): Boolean {
        return input.hasNext()
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun next(): Record {
        val record = input.next()
        exclusions.forEach { path ->
            val root = path.root.ref
            val value = record.values[root]
            record.values[root] = excludeValue(value, path.steps)
        }
        return record
    }

    override fun close() {
        input.close()
    }

    private fun excludeStruct(
        structValue: PQLValue,
        exclusions: List<Rel.Op.Exclude.Step>
    ): PQLValue {
        val structSymbolsToRemove = mutableSetOf<String>()
        val structKeysToRemove = mutableSetOf<String>() // keys stored as lowercase strings
        val branches = mutableMapOf<Rel.Op.Exclude.Type, List<Rel.Op.Exclude.Step>>()
        exclusions.forEach { exclusion ->
            when (exclusion.substeps.isEmpty()) {
                true -> {
                    when (val leafType = exclusion.type) {
                        is Rel.Op.Exclude.Type.StructWildcard -> {
                            // struct wildcard at current level. return empty struct
                            return PQLValue.structValue(emptyList())
                        }
                        is Rel.Op.Exclude.Type.StructSymbol -> structSymbolsToRemove.add(leafType.symbol)
                        is Rel.Op.Exclude.Type.StructKey -> structKeysToRemove.add(leafType.key.lowercase())
                        else -> { /* coll step; do nothing */ }
                    }
                }
                false -> {
                    when (exclusion.type) {
                        is Rel.Op.Exclude.Type.StructWildcard, is Rel.Op.Exclude.Type.StructSymbol, is Rel.Op.Exclude.Type.StructKey -> branches[exclusion.type] =
                            exclusion.substeps
                        else -> { /* coll step; do nothing */ }
                    }
                }
            }
        }
        val structSupplier = IteratorSupplier { structValue.structFields }
        val finalStruct = structSupplier.mapNotNull { structField ->
            if (structSymbolsToRemove.contains(structField.name) || structKeysToRemove.contains(structField.name.lowercase())) {
                // struct attr is to be removed at current level
                null
            } else {
                // deeper level exclusions
                val name = structField.name
                var value = structField.value
                // apply struct key exclusions at deeper levels
                val structKey = relOpExcludeTypeStructKey(name)
                branches[structKey]?.let {
                    value = excludeValue(value, it)
                }
                // apply struct symbol exclusions at deeper levels
                val structSymbol = relOpExcludeTypeStructSymbol(name)
                branches[structSymbol]?.let {
                    value = excludeValue(value, it)
                }
                // apply struct wildcard exclusions at deeper levels
                val structWildcard = relOpExcludeTypeStructWildcard()
                branches[structWildcard]?.let {
                    value = excludeValue(value, it)
                }
                Pair(name, value)
            }
        }.map { StructField.of(it.first, it.second) }
        return PQLValue.structValue(finalStruct)
    }

    /**
     * Returns a [PartiQLValue] created from an iterable of [coll]. Requires [type] to be a collection type
     * (i.e. [PartiQLValueType.LIST], [PartiQLValueType.BAG], or [PartiQLValueType.SEXP]).
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun newCollValue(type: PartiQLValueType, coll: Iterable<PQLValue>): PQLValue {
        return when (type) {
            PartiQLValueType.LIST -> PQLValue.listValue(coll)
            PartiQLValueType.BAG -> PQLValue.bagValue(coll)
            PartiQLValueType.SEXP -> PQLValue.sexpValue(coll)
            else -> error("Collection type required")
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun excludeCollection(
        coll: Iterable<PQLValue>,
        type: PartiQLValueType,
        exclusions: List<Rel.Op.Exclude.Step>
    ): PQLValue {
        val indexesToRemove = mutableSetOf<Int>()
        val branches = mutableMapOf<Rel.Op.Exclude.Type, List<Rel.Op.Exclude.Step>>()
        exclusions.forEach { exclusion ->
            when (exclusion.substeps.isEmpty()) {
                true -> {
                    when (val leafType = exclusion.type) {
                        is Rel.Op.Exclude.Type.CollWildcard -> {
                            // collection wildcard at current level. return empty collection
                            return newCollValue(type, emptyList())
                        }
                        is Rel.Op.Exclude.Type.CollIndex -> {
                            indexesToRemove.add(leafType.index)
                        }
                        else -> { /* struct step; do nothing */ }
                    }
                }
                false -> {
                    when (exclusion.type) {
                        is Rel.Op.Exclude.Type.CollWildcard, is Rel.Op.Exclude.Type.CollIndex -> branches[exclusion.type] =
                            exclusion.substeps
                        else -> { /* struct step; do nothing */ }
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
                if (type == PartiQLValueType.LIST || type == PartiQLValueType.SEXP) {
                    // apply collection index exclusions at deeper levels for lists and sexps
                    val collIndex = relOpExcludeTypeCollIndex(index)
                    branches[collIndex]?.let {
                        value = excludeValue(element, it)
                    }
                }
                // apply collection wildcard exclusions at deeper levels for lists, bags, and sexps
                val collWildcard = relOpExcludeTypeCollWildcard()
                branches[collWildcard]?.let {
                    value = excludeValue(value, it)
                }
                value
            }
        }
        return newCollValue(type, finalColl)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun excludeValue(initialPartiQLValue: PQLValue, exclusions: List<Rel.Op.Exclude.Step>): PQLValue {
        return when (initialPartiQLValue.type) {
            PartiQLValueType.STRUCT -> excludeStruct(initialPartiQLValue, exclusions)
            PartiQLValueType.BAG -> excludeCollection(IteratorSupplier { initialPartiQLValue.bagValues }, initialPartiQLValue.type, exclusions)
            PartiQLValueType.LIST -> excludeCollection(IteratorSupplier { initialPartiQLValue.listValues }, initialPartiQLValue.type, exclusions)
            PartiQLValueType.SEXP -> excludeCollection(IteratorSupplier { initialPartiQLValue.sexpValues }, initialPartiQLValue.type, exclusions)
            else -> {
                initialPartiQLValue
            }
        }
    }
}
