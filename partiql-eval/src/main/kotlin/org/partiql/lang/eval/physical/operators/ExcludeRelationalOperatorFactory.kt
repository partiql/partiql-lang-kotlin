package org.partiql.lang.eval.physical.operators

import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.internal.BagExprValue
import org.partiql.lang.eval.internal.ListExprValue
import org.partiql.lang.eval.internal.NamedExprValue
import org.partiql.lang.eval.internal.SexpExprValue
import org.partiql.lang.eval.internal.StructExprValue
import org.partiql.lang.eval.internal.ext.namedValue
import org.partiql.lang.eval.internal.newSequenceExprValue
import org.partiql.lang.eval.longValue
import org.partiql.lang.eval.name
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.eval.stringValue
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

/**
 * Provides an implementation of the [PartiqlPhysical.Bexpr.ExcludeClause] operator.
 *
 * @constructor
 *
 * @param name
 */
abstract class ExcludeRelationalOperatorFactory(name: String) : RelationalOperatorFactory {
    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.EXCLUDE, name)

    /**
     * Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.ExcludeClause].
     *
     * @param impl
     * @param sourceBexpr
     * @param compiledExcludeExprs
     * @return
     */
    abstract fun create(
        impl: PartiqlPhysical.Impl,
        sourceBexpr: RelationExpression,
        compiledExcludeExprs: List<CompiledExcludeExpr>
    ): RelationExpression
}

internal object ExcludeRelationalOperatorFactoryDefault : ExcludeRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        sourceBexpr: RelationExpression,
        compiledExcludeExprs: List<CompiledExcludeExpr>
    ): RelationExpression = ExcludeOperator(
        input = sourceBexpr,
        compiledExcludeExprs = compiledExcludeExprs
    )
}

internal class ExcludeOperator(
    val input: RelationExpression,
    val compiledExcludeExprs: List<CompiledExcludeExpr>
) : RelationExpression {
    private fun excludeBindings(
        curRegisters: Array<ExprValue>,
        root: Int,
        exclusions: RemoveAndOtherSteps
    ): Array<ExprValue> {
        val curExprValue = curRegisters.getOrNull(root)
        return if (curExprValue != null) {
            val newExprValue = excludeExprValue(curExprValue, exclusions)
            curRegisters[root] = newExprValue
            curRegisters
        } else {
            curRegisters
        }
    }

    private fun excludeStructExprValue(
        structExprValue: StructExprValue,
        exclusions: RemoveAndOtherSteps
    ): ExprValue {
        val toRemove = exclusions.remove
        val otherSteps = exclusions.steps
        if (toRemove.any { it is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard }) {
            // tuple wildcard at current level. return empty struct
            return StructExprValue(
                sequence = emptySequence(),
                ordering = structExprValue.ordering
            )
        }
        val attrsToRemove = toRemove.filterIsInstance<PartiqlPhysical.ExcludeStep.ExcludeTupleAttr>()
            .map { it.attr.name.text }
            .toSet()
        val sequenceWithRemoved = structExprValue.mapNotNull { structField ->
            if (attrsToRemove.contains(structField.name?.stringValue())) {
                null
            } else {
                structField as NamedExprValue
            }
        }
        val finalSequence = sequenceWithRemoved.map { structField ->
            var expr = structField.value
            val name = structField.name
            // apply case-sensitive tuple attr exclusions
            val structFieldCaseSensitiveKey = PartiqlPhysical.build {
                excludeTupleAttr(
                    identifier(
                        name.stringValue(),
                        caseSensitive()
                    )
                )
            }
            otherSteps[structFieldCaseSensitiveKey]?.let {
                expr = excludeExprValue(expr, it)
            }
            // apply case-insensitive tuple attr exclusions
            val structFieldCaseInsensitiveKey = PartiqlPhysical.build {
                excludeTupleAttr(
                    identifier(
                        name.stringValue(),
                        caseInsensitive()
                    )
                )
            }
            otherSteps[structFieldCaseInsensitiveKey]?.let {
                expr = excludeExprValue(expr, it)
            }
            // apply tuple wildcard exclusions
            val tupleWildcardKey = PartiqlPhysical.build { excludeTupleWildcard(emptyMetaContainer()) }
            otherSteps[tupleWildcardKey]?.let {
                expr = excludeExprValue(expr, it)
            }
            expr.namedValue(name)
        }.asSequence()
        return StructExprValue(sequence = finalSequence, ordering = structExprValue.ordering)
    }

    private fun excludeCollectionExprValue(
        initialExprValue: ExprValue,
        exprValueType: ExprValueType,
        exclusions: RemoveAndOtherSteps
    ): ExprValue {
        val toRemove = exclusions.remove
        val otherSteps = exclusions.steps
        if (toRemove.any { it is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard }) {
            // collection wildcard at current level. return empty collection
            return newSequenceExprValue(exprValueType, emptySequence())
        } else {
            val indexesToRemove = toRemove.filterIsInstance<PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex>()
                .map { it.index.value }
                .toSet()
            val sequenceWithRemoved = initialExprValue.mapNotNull { element ->
                if (indexesToRemove.contains(element.name?.longValue())) {
                    null
                } else {
                    element
                }
            }.asSequence()
            val finalSequence = sequenceWithRemoved.map { element ->
                var expr = element
                if (initialExprValue is ListExprValue || initialExprValue is SexpExprValue) {
                    element as NamedExprValue
                    val index = element.name.longValue()
                    // apply collection index exclusions for lists and sexps
                    val elementKey = PartiqlPhysical.build {
                        excludeCollectionIndex(
                            index
                        )
                    }
                    otherSteps[elementKey]?.let {
                        expr = excludeExprValue(element.value, it)
                    }
                }
                // apply collection wildcard exclusions for lists, bags, and sexps
                val collectionWildcardKey = PartiqlPhysical.build { excludeCollectionWildcard(emptyMetaContainer()) }
                otherSteps[collectionWildcardKey]?.let {
                    expr = excludeExprValue(expr, it)
                }
                expr
            }
            return newSequenceExprValue(exprValueType, finalSequence)
        }
    }

    private fun excludeExprValue(initialExprValue: ExprValue, exclusions: RemoveAndOtherSteps): ExprValue {
        return when (initialExprValue) {
            is NamedExprValue -> excludeExprValue(initialExprValue.value, exclusions)
            is StructExprValue -> excludeStructExprValue(initialExprValue, exclusions)
            is ListExprValue -> excludeCollectionExprValue(initialExprValue, ExprValueType.LIST, exclusions)
            is BagExprValue -> excludeCollectionExprValue(initialExprValue, ExprValueType.BAG, exclusions)
            is SexpExprValue -> excludeCollectionExprValue(initialExprValue, ExprValueType.SEXP, exclusions)
            else -> {
                initialExprValue
            }
        }
    }

    override fun evaluate(state: EvaluatorState): RelationIterator {
        val rows = input.evaluate(state)

        return relation(rows.relType) {
            while (rows.nextRow()) {
                val newRegisters = compiledExcludeExprs.fold(state.registers) { curRegisters, expr ->
                    excludeBindings(curRegisters, expr.root, expr.exclusions)
                }
                state.load(newRegisters)
                yield()
            }
        }
    }
}

/**
 * TODO ALAN: after rebase to include `EvaluatingCompiler` implementation of `EXCLUDE`, need to refactor for better
 *  code reuse here and other parts.
 * Creates a list of compiled exclude expressions with each index of the resulting list corresponding to a different
 * exclude path root.
 */
internal fun compileExcludeClause(excludeClause: PartiqlPhysical.Bexpr.ExcludeClause): List<CompiledExcludeExpr> {
    val excludeExprs = excludeClause.exprs
    fun addToCompiledExcludeExprs(curCompiledExpr: RemoveAndOtherSteps, steps: List<PartiqlPhysical.ExcludeStep>): RemoveAndOtherSteps {
        // subsumption cases
        // when steps.size == 1: possibly add to remove set
        // when steps.size > 1: possibly add to steps map
        val first = steps.first()
        var entryRemove = curCompiledExpr.remove.toMutableSet()
        var entrySteps = curCompiledExpr.steps.toMutableMap()
        if (steps.size == 1) {
            when (first) {
                is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr -> {
                    if (entryRemove.contains(PartiqlPhysical.build { excludeTupleWildcard() })) {
                        // contains wildcard; do not add; e.g. a.* and a.b -> keep a.*
                    } else {
                        // add to entries to remove
                        entryRemove.add(first)
                        // remove from other steps; e.g. a.b.c and a.b -> keep a.b
                        entrySteps.remove(first)
                    }
                }
                is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard -> {
                    entryRemove.add(first)
                    // remove all tuple attribute exclude steps
                    entryRemove = entryRemove.filterNot {
                        it is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr
                    }.toMutableSet()
                    // remove all tuple attribute/wildcard exclude steps from deeper levels
                    entrySteps = entrySteps.filterNot {
                        it.key is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr || it.key is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard
                    }.toMutableMap()
                }
                is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex -> {
                    if (entryRemove.contains(PartiqlPhysical.build { excludeCollectionWildcard() })) {
                        // contains wildcard; do not add; e.g a[*] and a[1] -> keep a[*]
                    } else {
                        // add to entries to remove
                        entryRemove.add(first)
                        // remove from other steps; e.g. a.b[2].c and a.b[2] -> keep a.b[2]
                        entrySteps.remove(first)
                    }
                }
                is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard -> {
                    entryRemove.add(first)
                    // remove all collection index exclude steps
                    entryRemove = entryRemove.filterNot {
                        it is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex
                    }.toMutableSet()
                    // remove all collection index/wildcard exclude steps from deeper levels
                    entrySteps = entrySteps.filterNot {
                        it.key is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex || it.key is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard
                    }.toMutableMap()
                }
            }
        } else {
            // remove at deeper level; need to check if first step is already removed in current step
            when (first) {
                is PartiqlPhysical.ExcludeStep.ExcludeTupleAttr -> {
                    if (entryRemove.contains(PartiqlPhysical.build { excludeTupleWildcard() }) || entryRemove.contains(first)) {
                        // remove set contains tuple wildcard or attr; do not add to other steps;
                        // e.g. a.* and a.b.c -> a.*
                    } else {
                        val existingEntry = entrySteps.getOrDefault(first, RemoveAndOtherSteps.empty())
                        val newEntry = addToCompiledExcludeExprs(existingEntry, steps.drop(1))
                        entrySteps[first] = newEntry
                    }
                }
                is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard -> {
                    if (entryRemove.any { it is PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard }) {
                        // tuple wildcard at current level; do nothing
                    } else {
                        val existingEntry = entrySteps.getOrDefault(first, RemoveAndOtherSteps.empty())
                        val newEntry = addToCompiledExcludeExprs(existingEntry, steps.drop(1))
                        entrySteps[first] = newEntry
                    }
                }
                is PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex -> {
                    if (entryRemove.contains(PartiqlPhysical.build { excludeCollectionWildcard() }) || entryRemove.contains(first)) {
                        // remove set contains collection wildcard or index; do not add to other steps;
                        // e.g. a[*] and a[*][1] -> a[*]
                    } else {
                        val existingEntry = entrySteps.getOrDefault(first, RemoveAndOtherSteps.empty())
                        val newEntry = addToCompiledExcludeExprs(existingEntry, steps.drop(1))
                        entrySteps[first] = newEntry
                    }
                }
                is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard -> {
                    if (entryRemove.any { it is PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard }) {
                        // collection wildcard at current level; do nothing
                    } else {
                        val existingEntry = entrySteps.getOrDefault(first, RemoveAndOtherSteps.empty())
                        val newEntry = addToCompiledExcludeExprs(existingEntry, steps.drop(1))
                        entrySteps[first] = newEntry
                    }
                }
            }
        }
        return RemoveAndOtherSteps(entryRemove, entrySteps)
    }
    val compiledExcludeExprs = excludeExprs
        .groupBy { it.root }
        .map { (root, exclusions) ->
            val compiledExclusions = exclusions.fold(RemoveAndOtherSteps.empty()) { acc, exclusion ->
                addToCompiledExcludeExprs(acc, exclusion.steps)
            }
            CompiledExcludeExpr(root.value.toInt(), compiledExclusions)
        }
    return compiledExcludeExprs
}

/**
 * Represents an instance of a compiled `EXCLUDE` expression. Notably, this expr will have redundant steps removed.
 */
data class CompiledExcludeExpr(val root: Int, val exclusions: RemoveAndOtherSteps)

/**
 * Represents all the exclusions at the current level and other nested levels.
 *
 * The idea behind this data structure is that at a current level (i.e. path step index), we keep track of the
 * - Exclude paths that have a final exclude step at the current level. This set of tuple attributes and collection
 * indexes to remove at the current level is modeled as a set of exclude steps (i.e. [RemoveAndOtherSteps.remove]).
 * - Exclude paths that have additional steps (their final step is at a deeper level). This is modeled as a mapping
 * of exclude steps to other [RemoveAndOtherSteps] to group all exclude paths that share the same current step.
 *
 * For example, let's say we have exclude paths (ignoring the exclude path root) of
 *       a.b,
 *       x.y.z1,
 *       x.y.z2
 *       ^ ^ ^
 * Level 1 2 3
 *
 * These exclude paths would be converted to the following in [RemoveAndOtherSteps].
 * ```
 * // For demonstration purposes, the syntax '<string>' corresponds to the exclude tuple attribute step of <string>
 * RemoveAndOtherSteps(                   // Level 1 (no exclusions at level 1)
 *     remove = emptySet(),
 *     steps = mapOf(
 *         'a' to RemoveAndOtherSteps(    // Level 2 for paths that have `'a'` in Level 1
 *             remove = setOf('b'),       // path `a.b` has final step at level 2
 *             steps = emptyMap()
 *         ),
 *         'x' to RemoveAndOtherSteps(    // Level 2 for paths that have `'x'` in Level 1
 *             remove = emptySet(),
 *             steps = mapOf(
 *                 'y' to RemoveAndOtherSteps(     // Level 3 for paths that have `'y'` in Level 2 and `'x'` in Level 1
 *                     remove = setOf('z1', 'z2'), // paths `x.y.z1` and `x.y.z2` have final step at level 3
 *                     steps = emptyMap()
 *                 )
 *             )
 *         ),
 *     )
 * )
 * ```
 */
data class RemoveAndOtherSteps(val remove: Set<PartiqlPhysical.ExcludeStep>, val steps: Map<PartiqlPhysical.ExcludeStep, RemoveAndOtherSteps>) {
    companion object {
        fun empty(): RemoveAndOtherSteps {
            return RemoveAndOtherSteps(emptySet(), emptyMap())
        }
    }
}
