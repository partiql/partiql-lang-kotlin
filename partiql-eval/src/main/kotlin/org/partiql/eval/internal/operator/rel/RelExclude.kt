package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.exclude.CompiledExcludeItem
import org.partiql.eval.internal.exclude.ExcludeFieldCase
import org.partiql.eval.internal.exclude.ExcludeNode
import org.partiql.eval.internal.exclude.ExcludeStep
import org.partiql.eval.internal.operator.Operator
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
    private val exclusions: List<CompiledExcludeItem>
) : Operator.Relation {

    override fun open() {
        input.open()
    }

    override fun next(): Record? {
        while (true) {
            val row = input.next() ?: return null
            val newRecord = exclusions.fold(row) { curRecord, expr ->
                excludeOnRecord(curRecord, expr)
            }
            return newRecord
        }
    }

    override fun close() {
        input.close()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun excludeOnRecord(
        record: Record,
        exclusions: CompiledExcludeItem
    ): Record {
        val values = record.values
        val value = values.getOrNull(exclusions.root)
        val newValues = if (value != null) {
            values[exclusions.root] = excludeOnPartiQLValue(value, exclusions)
            values
        } else {
            values
        }
        return Record(newValues)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun excludeOnStructValue(
        structValue: StructValue<*>,
        exclusions: ExcludeNode
    ): PartiQLValue {
        val attrsToRemove = exclusions.leaves.mapNotNull { leaf ->
            when (val leafStep = leaf.step) {
                is ExcludeStep.StructWildcard -> {
                    // tuple wildcard at current level. return empty struct
                    return structValue<PartiQLValue>()
                }
                is ExcludeStep.StructField -> leafStep.attr
                is ExcludeStep.CollIndex, is ExcludeStep.CollWildcard -> null
            }
        }.toSet()
        val branches = exclusions.branches
        val finalStruct = structValue.entries.mapNotNull { structField ->
            if (attrsToRemove.contains(structField.first)) {
                // struct attr is to be removed at current level
                null
            } else {
                // deeper level exclusions
                val name = structField.first
                var expr = structField.second
                // apply case-sensitive tuple attr exclusions at deeper levels
                val structFieldCaseSensitiveKey = ExcludeStep.StructField(name, ExcludeFieldCase.SENSITIVE)
                branches.find {
                    it.step == structFieldCaseSensitiveKey
                }?.let {
                    expr = excludeOnPartiQLValue(expr, it)
                }
                // apply case-insensitive tuple attr exclusions at deeper levels
                val structFieldCaseInsensitiveKey = ExcludeStep.StructField(name, ExcludeFieldCase.INSENSITIVE)
                branches.find {
                    it.step == structFieldCaseInsensitiveKey
                }?.let {
                    expr = excludeOnPartiQLValue(expr, it)
                }
                // apply tuple wildcard exclusions at deeper levels
                val tupleWildcardKey = ExcludeStep.StructWildcard
                branches.find {
                    it.step == tupleWildcardKey
                }?.let {
                    expr = excludeOnPartiQLValue(expr, it)
                }
                Pair(name, expr)
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
    private fun excludeOnCollValue(
        coll: CollectionValue<*>,
        type: PartiQLValueType,
        exclusions: ExcludeNode
    ): PartiQLValue {
        val indexesToRemove = exclusions.leaves.mapNotNull { leaf ->
            when (val leafStep = leaf.step) {
                is ExcludeStep.CollWildcard -> {
                    // collection wildcard at current level. return empty collection
                    return newCollValue(type, emptyList())
                }
                is ExcludeStep.CollIndex -> leafStep.index
                is ExcludeStep.StructField, is ExcludeStep.StructWildcard -> null
            }
        }.toSet()
        val branches = exclusions.branches
        val finalColl = coll.mapIndexedNotNull { index, element ->
            if (indexesToRemove.contains(index)) {
                // coll index is to be removed at current level
                null
            } else {
                // deeper level exclusions
                var expr = element
                if (coll is ListValue || coll is SexpValue) {
                    // apply collection index exclusions at deeper levels for lists and sexps
                    val elementKey = ExcludeStep.CollIndex(index)
                    branches.find {
                        it.step == elementKey
                    }?.let {
                        expr = excludeOnPartiQLValue(element, it)
                    }
                }
                // apply collection wildcard exclusions at deeper levels for lists, bags, and sexps
                val collectionWildcardKey = ExcludeStep.CollWildcard
                branches.find {
                    it.step == collectionWildcardKey
                }?.let {
                    expr = excludeOnPartiQLValue(expr, it)
                }
                expr
            }
        }
        return newCollValue(type, finalColl)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun excludeOnPartiQLValue(initialPartiQLValue: PartiQLValue, exclusions: ExcludeNode): PartiQLValue {
        return when (initialPartiQLValue) {
            is StructValue<*> -> excludeOnStructValue(initialPartiQLValue, exclusions)
            is BagValue<*> -> excludeOnCollValue(initialPartiQLValue, PartiQLValueType.BAG, exclusions)
            is ListValue<*> -> excludeOnCollValue(initialPartiQLValue, PartiQLValueType.LIST, exclusions)
            is SexpValue<*> -> excludeOnCollValue(initialPartiQLValue, PartiQLValueType.SEXP, exclusions)
            else -> {
                initialPartiQLValue
            }
        }
    }
}
