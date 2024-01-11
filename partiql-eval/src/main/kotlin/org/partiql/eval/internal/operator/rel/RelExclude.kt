package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Rel
import org.partiql.plan.relOpExcludeTypeCollIndex
import org.partiql.plan.relOpExcludeTypeCollWildcard
import org.partiql.plan.relOpExcludeTypeStructKey
import org.partiql.plan.relOpExcludeTypeStructSymbol
import org.partiql.plan.relOpExcludeTypeStructWildcard
import org.partiql.value.BagValue
import org.partiql.value.CollectionValue
import org.partiql.value.ListValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.SexpValue
import org.partiql.value.StructValue
import org.partiql.value.bagValue
import org.partiql.value.listValue
import org.partiql.value.sexpValue
import org.partiql.value.structValue

internal class RelExclude(
    private val input: Operator.Relation,
    private val exclusions: List<Rel.Op.Exclude.Path>
) : Operator.Relation {

    override fun open() {
        input.open()
    }

    override fun next(): Record? {
        val record = input.next() ?: return null
        return exclusions.fold(record) { rec, path -> exclude(rec, path) }
    }

    override fun close() {
        input.close()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun exclude(
        record: Record,
        path: Rel.Op.Exclude.Path
    ): Record {
        val values = record.values
        val value = values.getOrNull(path.root.ref)
        val newValues = if (value != null) {
            values[path.root.ref] = exclude(value, path.steps)
            values
        } else {
            values
        }
        return Record(newValues)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun exclude(
        structValue: StructValue<*>,
        exclusions: List<Rel.Op.Exclude.Step>
    ): PartiQLValue {
        val structSymbolsToRemove = mutableSetOf<String>()
        val structKeysToRemove = mutableSetOf<String>() // keys stored as lowercase strings
        val branches = mutableMapOf<Rel.Op.Exclude.Type, List<Rel.Op.Exclude.Step>>()
        exclusions.forEach { exclusion ->
            when (exclusion.substeps.isEmpty()) {
                true -> {
                    when (val leafType = exclusion.type) {
                        is Rel.Op.Exclude.Type.StructWildcard -> {
                            // struct wildcard at current level. return empty struct
                            return structValue<PartiQLValue>()
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
        val finalStruct = structValue.entries.mapNotNull { structField ->
            if (structSymbolsToRemove.contains(structField.first) || structKeysToRemove.contains(structField.first.lowercase())) {
                // struct attr is to be removed at current level
                null
            } else {
                // deeper level exclusions
                val name = structField.first
                var value = structField.second
                // apply struct key exclusions at deeper levels
                val structKey = relOpExcludeTypeStructKey(name)
                branches[structKey]?.let {
                    value = exclude(value, it)
                }
                // apply struct symbol exclusions at deeper levels
                val structSymbol = relOpExcludeTypeStructSymbol(name)
                branches[structSymbol]?.let {
                    value = exclude(value, it)
                }
                // apply struct wildcard exclusions at deeper levels
                val structWildcard = relOpExcludeTypeStructWildcard()
                branches[structWildcard]?.let {
                    value = exclude(value, it)
                }
                Pair(name, value)
            }
        }
        return structValue(finalStruct)
    }

    /**
     * Returns a [PartiQLValue] created from an iterable of [coll]. Requires [type] to be a collection type
     * (i.e. [PartiQLValueType.LIST], [PartiQLValueType.BAG], or [PartiQLValueType.SEXP]).
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun newCollValue(type: PartiQLValueType, coll: Iterable<PartiQLValue>): PartiQLValue {
        return when (type) {
            PartiQLValueType.LIST -> listValue(coll)
            PartiQLValueType.BAG -> bagValue(coll)
            PartiQLValueType.SEXP -> sexpValue(coll)
            else -> error("Collection type required")
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun exclude(
        coll: CollectionValue<*>,
        type: PartiQLValueType,
        exclusions: List<Rel.Op.Exclude.Step>
    ): PartiQLValue {
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
                if (coll is ListValue || coll is SexpValue) {
                    // apply collection index exclusions at deeper levels for lists and sexps
                    val collIndex = relOpExcludeTypeCollIndex(index)
                    branches[collIndex]?.let {
                        value = exclude(element, it)
                    }
                }
                // apply collection wildcard exclusions at deeper levels for lists, bags, and sexps
                val collWildcard = relOpExcludeTypeCollWildcard()
                branches[collWildcard]?.let {
                    value = exclude(value, it)
                }
                value
            }
        }
        return newCollValue(type, finalColl)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun exclude(initialPartiQLValue: PartiQLValue, exclusions: List<Rel.Op.Exclude.Step>): PartiQLValue {
        return when (initialPartiQLValue) {
            is StructValue<*> -> exclude(initialPartiQLValue, exclusions)
            is BagValue<*> -> exclude(initialPartiQLValue, PartiQLValueType.BAG, exclusions)
            is ListValue<*> -> exclude(initialPartiQLValue, PartiQLValueType.LIST, exclusions)
            is SexpValue<*> -> exclude(initialPartiQLValue, PartiQLValueType.SEXP, exclusions)
            else -> {
                initialPartiQLValue
            }
        }
    }
}
