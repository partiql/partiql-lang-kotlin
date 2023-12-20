package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.exclude.CompiledExcludeItem
import org.partiql.eval.internal.exclude.ExcludeFieldCase
import org.partiql.eval.internal.exclude.ExcludeNode
import org.partiql.eval.internal.exclude.ExcludeStep
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Identifier
import org.partiql.plan.Rel
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
    val input: Operator.Relation,
    private val compiledExcludeItems: List<CompiledExcludeItem>
) : Operator.Relation {

    override fun open() {
        input.open()
    }

    override fun next(): Record? {
        while (true) {
            val row = input.next() ?: return null
            val newRecord = compiledExcludeItems.fold(row) { curRecord, expr ->
                excludeOnRecord(curRecord, expr)
            }
            return newRecord
        }
    }

    override fun close() {
        input.close()
    }
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
    val leavesSteps = exclusions.leaves.map { leaf -> leaf.step }
    val branches = exclusions.branches
    if (leavesSteps.any { it is ExcludeStep.StructWildcard }) {
        // tuple wildcard at current level. return empty struct
        return structValue<PartiQLValue>()
    }
    val attrsToRemove = leavesSteps.filterIsInstance<ExcludeStep.StructField>()
        .map { it.attr }
        .toSet()
    val entriesWithRemoved = structValue.entries.filter { structField ->
        !attrsToRemove.contains(structField.first)
    }
    val finalStruct = entriesWithRemoved.map { structField ->
        val name = structField.first
        var expr = structField.second
        // apply case-sensitive tuple attr exclusions
        val structFieldCaseSensitiveKey = ExcludeStep.StructField(name, ExcludeFieldCase.SENSITIVE)
        branches.find {
            it.step == structFieldCaseSensitiveKey
        }?.let {
            expr = excludeOnPartiQLValue(expr, it)
        }
        // apply case-insensitive tuple attr exclusions
        val structFieldCaseInsensitiveKey = ExcludeStep.StructField(name, ExcludeFieldCase.INSENSITIVE)
        branches.find {
            it.step == structFieldCaseInsensitiveKey
        }?.let {
            expr = excludeOnPartiQLValue(expr, it)
        }
        // apply tuple wildcard exclusions
        val tupleWildcardKey = ExcludeStep.StructWildcard
        branches.find {
            it.step == tupleWildcardKey
        }?.let {
            expr = excludeOnPartiQLValue(expr, it)
        }
        Pair(name, expr)
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
    val leavesSteps = exclusions.leaves.map { leaf -> leaf.step }
    val branches = exclusions.branches
    if (leavesSteps.any { it is ExcludeStep.CollWildcard }) {
        // collection wildcard at current level. return empty collection
        return newCollValue(type, emptyList())
    } else {
        val indexesToRemove = leavesSteps.filterIsInstance<ExcludeStep.CollIndex>()
            .map { it.index }
            .toSet()
        val collWithRemoved = when (coll) {
            is BagValue -> coll
            is ListValue, is SexpValue -> coll.filterIndexed { index, _ ->
                !indexesToRemove.contains(index)
            }
        }
        val finalColl = collWithRemoved.mapIndexed { index, element ->
            var expr = element
            if (coll is ListValue || coll is SexpValue) {
                // apply collection index exclusions for lists and sexps
                val elementKey = ExcludeStep.CollIndex(index)
                branches.find {
                    it.step == elementKey
                }?.let {
                    expr = excludeOnPartiQLValue(element, it)
                }
            }
            // apply collection wildcard exclusions for lists, bags, and sexps
            val collectionWildcardKey = ExcludeStep.CollWildcard
            branches.find {
                it.step == collectionWildcardKey
            }?.let {
                expr = excludeOnPartiQLValue(expr, it)
            }
            expr
        }
        return newCollValue(type, finalColl)
    }
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

/**
 * Creates a list of [CompiledExcludeItem] with each index of the resulting list corresponding to a different
 * exclude path root.
 */
internal fun compileExcludeItems(excludeExprs: List<Rel.Op.Exclude.Item>): List<CompiledExcludeItem> {
    val compiledExcludeItems = excludeExprs
        .groupBy { it.root }
        .map { (root, exclusions) ->
            exclusions.fold(CompiledExcludeItem.empty(root.ref)) { acc, exclusion ->
                acc.addNode(exclusion.steps.map { it.toCompiledExcludeStep() })
                acc
            }
        }
    return compiledExcludeItems
}

private fun Rel.Op.Exclude.Step.toCompiledExcludeStep(): ExcludeStep {
    return when (this) {
        is Rel.Op.Exclude.Step.StructField -> ExcludeStep.StructField(this.symbol.symbol, this.symbol.caseSensitivity.toCompiledExcludeStepCase())
        is Rel.Op.Exclude.Step.StructWildcard -> ExcludeStep.StructWildcard
        is Rel.Op.Exclude.Step.CollIndex -> ExcludeStep.CollIndex(this.index)
        is Rel.Op.Exclude.Step.CollWildcard -> ExcludeStep.CollWildcard
    }
}

private fun Identifier.CaseSensitivity.toCompiledExcludeStepCase(): ExcludeFieldCase {
    return when (this) {
        Identifier.CaseSensitivity.SENSITIVE -> ExcludeFieldCase.SENSITIVE
        Identifier.CaseSensitivity.INSENSITIVE -> ExcludeFieldCase.INSENSITIVE
    }
}
