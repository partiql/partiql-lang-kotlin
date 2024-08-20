package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.IteratorSupplier
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.eval.value.Field
import org.partiql.plan.Rel
import org.partiql.plan.relOpExcludeTypeCollIndex
import org.partiql.plan.relOpExcludeTypeCollWildcard
import org.partiql.plan.relOpExcludeTypeStructKey
import org.partiql.plan.relOpExcludeTypeStructSymbol
import org.partiql.plan.relOpExcludeTypeStructWildcard
import org.partiql.types.PType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueType

internal class RelOpExclude(
    private val input: Operator.Relation,
    private val exclusions: List<Rel.Op.Exclude.Path>
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
            val root = path.root.ref
            val value = record.values[root]
            record.values[root] = excludeValue(value, path.steps)
        }
        return record
    }

    override fun close() {
        input.close()
    }

    private fun excludeFields(
        structValue: Datum,
        exclusions: List<Rel.Op.Exclude.Step>
    ): Datum {
        val structSymbolsToRemove = mutableSetOf<String>()
        val structKeysToRemove = mutableSetOf<String>() // keys stored as lowercase strings
        val branches = mutableMapOf<Rel.Op.Exclude.Type, List<Rel.Op.Exclude.Step>>()
        exclusions.forEach { exclusion ->
            when (exclusion.substeps.isEmpty()) {
                true -> {
                    when (val leafType = exclusion.type) {
                        is Rel.Op.Exclude.Type.StructWildcard -> {
                            // struct wildcard at current level. return empty struct
                            return Datum.struct(emptyList())
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
        exclusions: List<Rel.Op.Exclude.Step>
    ): Datum {
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
                if (type.kind == PType.Kind.ARRAY || type.kind == PType.Kind.SEXP) {
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

    private fun excludeValue(initialPartiQLValue: Datum, exclusions: List<Rel.Op.Exclude.Step>): Datum {
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
