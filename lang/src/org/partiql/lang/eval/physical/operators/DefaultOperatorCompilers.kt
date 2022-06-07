package org.partiql.lang.eval.physical.operators

import coercePredicateResult
import com.amazon.ionelement.api.BoolElement
import com.amazon.ionelement.api.MetaContainer
import createFilterRelItr
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.address
import org.partiql.lang.eval.name
import org.partiql.lang.eval.physical.DEFAULT_IMPL_NAME
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.ExprThunkEnv
import org.partiql.lang.eval.physical.RelationThunkEnv
import org.partiql.lang.eval.physical.evalLimitRowCount
import org.partiql.lang.eval.physical.evalOffsetRowCount
import org.partiql.lang.eval.physical.relationThunk
import org.partiql.lang.eval.physical.toSetVariableFunc
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationScope
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.eval.sourceLocationMeta
import org.partiql.lang.eval.unnamedValue
import org.partiql.lang.util.toIntExact

// DL TODO: terminology everywhere.  For instance, I don't know if I like the term operator "compiler" applied here

val DEFAULT_OPERATOR_COMPILERS = OperatorCompilerCatalog(
    listOf(
        createPhysicalOperatorCompiler<PartiqlPhysical.Bexpr.Scan>(DEFAULT_IMPL_NAME) { node, compiler, valueFactory ->
            val exprThunk = compiler.compile(node.expr)

            val asIndexSetter = node.asDecl.toSetVariableFunc()
            val atIndexSetter = node.atDecl?.toSetVariableFunc()
            val byIndexSetter = node.byDecl?.toSetVariableFunc()

            relationThunk(node.metas) { state ->
                val valueToScan = exprThunk.invoke(state)

                // coerces non-collection types to a singleton Sequence<>.
                val rows: Sequence<ExprValue> = when (valueToScan.type) {
                    ExprValueType.LIST, ExprValueType.BAG -> valueToScan.asSequence()
                    else -> sequenceOf(valueToScan)
                }

                relation(RelationType.BAG) {
                    val rowsIter: Iterator<ExprValue> = rows.iterator()
                    while (rowsIter.hasNext()) {
                        val item = rowsIter.next()

                        // Note that although we could access [EvaluatorState.registers] directly within relationThunk,
                        // we don't as a means to demonstrate the proper way that custom operator implementations should
                        // set the values of variables.  See [toSetFunc] and [SetVarFunc] for more info.
                        asIndexSetter(state, item.unnamedValue()) // Remove any ordinal (output is a bag)

                        if (atIndexSetter != null) {
                            atIndexSetter(state, item.name ?: valueFactory.missingValue)
                        }

                        if (byIndexSetter != null) {
                            byIndexSetter(state, item.address ?: valueFactory.missingValue)
                        }
                        yield()
                    }
                }
            }
        },
        createPhysicalOperatorCompiler<PartiqlPhysical.Bexpr.Filter>(DEFAULT_IMPL_NAME) { node, compiler, _ ->
            val predicateThunk = compiler.compile(node.predicate)
            val sourceThunk = compiler.compile(node.source)

            relationThunk(node.metas) { env ->
                val sourceToFilter = sourceThunk(env)
                createFilterRelItr(sourceToFilter, predicateThunk, env)
            }
        },
        createPhysicalOperatorCompiler<PartiqlPhysical.Bexpr.Join>(DEFAULT_IMPL_NAME) { node, compiler, valueFactory ->
            val leftThunk = compiler.compile(node.left)
            val rightThunk = compiler.compile(node.right)
            val predicateThunk = node.predicate?.let { compiler.compile(it).takeIf { !node.predicate.isLitTrue() } }

            when (node.joinType) {
                is PartiqlPhysical.JoinType.Inner -> {
                    createInnerJoinThunk(node.metas, leftThunk, rightThunk, predicateThunk)
                }
                is PartiqlPhysical.JoinType.Left -> {
                    val rightVariableIndexes = node.right.extractAccessibleVarDecls().map { it.index.value.toIntExact() }
                    createLeftJoinThunk(
                        joinMetas = node.metas,
                        leftThunk = leftThunk,
                        rightThunk = rightThunk,
                        rightVariableIndexes = rightVariableIndexes,
                        predicateThunk = predicateThunk,
                        valueFactory = valueFactory
                    )
                }
                is PartiqlPhysical.JoinType.Right -> {
                    // Note that this is the same as the left join but the right and left sides are swapped.
                    val leftVariableIndexes = node.left.extractAccessibleVarDecls().map { it.index.value.toIntExact() }
                    createLeftJoinThunk(
                        joinMetas = node.metas,
                        leftThunk = rightThunk,
                        rightThunk = leftThunk,
                        rightVariableIndexes = leftVariableIndexes,
                        predicateThunk = predicateThunk,
                        valueFactory = valueFactory
                    )
                }
                is PartiqlPhysical.JoinType.Full -> TODO("Full join")
            }
        },
        createPhysicalOperatorCompiler<PartiqlPhysical.Bexpr.Offset>(DEFAULT_IMPL_NAME) { node, compiler, _ ->
            val rowCountThunk = compiler.compile(node.rowCount)
            val sourceThunk = compiler.compile(node.source)
            val rowCountLocation = node.rowCount.metas.sourceLocationMeta
            relationThunk(node.metas) { env ->
                val skipCount: Long = evalOffsetRowCount(rowCountThunk, env, rowCountLocation)
                relation(RelationType.BAG) {
                    val sourceRel = sourceThunk(env)
                    var rowCount = 0L
                    while (rowCount++ < skipCount) {
                        // stop iterating if we finish run out of rows before we hit the offset.
                        if (!sourceRel.nextRow()) {
                            return@relation
                        }
                    }
                    yieldAll(sourceRel)
                }
            }
        },
        createPhysicalOperatorCompiler<PartiqlPhysical.Bexpr.Limit>(DEFAULT_IMPL_NAME) { node, compiler, _ ->
            val rowCountThunk = compiler.compile(node.rowCount)
            val sourceThunk = compiler.compile(node.source)
            val rowCountLocation = node.rowCount.metas.sourceLocationMeta
            relationThunk(node.metas) { env ->
                val limitCount = evalLimitRowCount(rowCountThunk, env, rowCountLocation)
                val rowIter = sourceThunk(env)
                relation(RelationType.BAG) {
                    var rowCount = 0L
                    while (rowCount++ < limitCount && rowIter.nextRow()) {
                        yield()
                    }
                }
            }
        },
        createPhysicalOperatorCompiler<PartiqlPhysical.Bexpr.Let>(DEFAULT_IMPL_NAME) { node, compiler, _ ->
            val sourceThunk = compiler.compile(node.source)
            class CompiledBinding(val index: Int, val valueThunk: ExprThunkEnv)
            val compiledBindings = node.bindings.map {
                CompiledBinding(
                    it.decl.index.value.toIntExact(),
                    compiler.compile(it.value)
                )
            }
            relationThunk(node.metas) { env ->
                val sourceItr = sourceThunk(env)

                relation(sourceItr.relType) {
                    while (sourceItr.nextRow()) {
                        compiledBindings.forEach {
                            env.registers[it.index] = it.valueThunk(env)
                        }
                        yield()
                    }
                }
            }
        }
    )
) // end of default operator implementations

private fun createInnerJoinThunk(
    joinMetas: MetaContainer,
    leftThunk: RelationThunkEnv,
    rightThunk: RelationThunkEnv,
    predicateThunk: ExprThunkEnv?
) = if (predicateThunk == null) {
    relationThunk(joinMetas) { env ->
        createCrossJoinRelItr(leftThunk, rightThunk, env)
    }
} else {
    relationThunk(joinMetas) { env ->
        val crossJoinRelItr = createCrossJoinRelItr(leftThunk, rightThunk, env)
        createFilterRelItr(crossJoinRelItr, predicateThunk, env)
    }
}

private fun createCrossJoinRelItr(
    leftThunk: RelationThunkEnv,
    rightThunk: RelationThunkEnv,
    env: EvaluatorState
): RelationIterator {
    return relation(RelationType.BAG) {
        val leftItr = leftThunk(env)
        while (leftItr.nextRow()) {
            val rightItr = rightThunk(env)
            while (rightItr.nextRow()) {
                yield()
            }
        }
    }
}

private fun createLeftJoinThunk(
    joinMetas: MetaContainer,
    leftThunk: RelationThunkEnv,
    rightThunk: RelationThunkEnv,
    rightVariableIndexes: List<Int>,
    predicateThunk: ExprThunkEnv?,
    valueFactory: ExprValueFactory
) =
    relationThunk(joinMetas) { env ->
        createLeftJoinRelItr(leftThunk, rightThunk, rightVariableIndexes, predicateThunk, env, valueFactory)
    }

/**
 * Like [createCrossJoinRelItr], but the right-hand relation is padded with unknown values in the event
 * that it is empty or that the predicate does not match.
 */
private fun createLeftJoinRelItr(
    leftThunk: RelationThunkEnv,
    rightThunk: RelationThunkEnv,
    rightVariableIndexes: List<Int>,
    predicateThunk: ExprThunkEnv?,
    state: EvaluatorState,
    valueFactory: ExprValueFactory
): RelationIterator {
    return if (predicateThunk == null) {
        relation(RelationType.BAG) {
            val leftItr = leftThunk(state)
            while (leftItr.nextRow()) {
                val rightItr = rightThunk(state)
                // if the rightItr does has a row...
                if (rightItr.nextRow()) {
                    yield() // yield current row
                    yieldAll(rightItr) // yield remaining rows
                } else {
                    // no row--yield padded row
                    yieldPaddedUnknowns(rightVariableIndexes, state, valueFactory)
                }
            }
        }
    } else {
        relation(RelationType.BAG) {
            val leftItr = leftThunk(state)
            while (leftItr.nextRow()) {
                val rightItr = rightThunk(state)
                var yieldedSomething = false
                while (rightItr.nextRow()) {
                    if (coercePredicateResult(predicateThunk(state))) {
                        yield()
                        yieldedSomething = true
                    }
                }
                // If we still haven't yielded anything, we still need to emit a row with right-hand side variables
                // padded with unknowns.
                if (!yieldedSomething) {
                    yieldPaddedUnknowns(rightVariableIndexes, state, valueFactory)
                }
            }
        }
    }
}

private fun PartiqlPhysical.Bexpr.extractAccessibleVarDecls(): List<PartiqlPhysical.VarDecl> =
// This fold traverses a [PartiqlPhysical.Bexpr] node and extracts all variable declarations within
    // It avoids recursing into sub-queries.
    object : PartiqlPhysical.VisitorFold<List<PartiqlPhysical.VarDecl>>() {
        override fun visitVarDecl(
            node: PartiqlPhysical.VarDecl,
            accumulator: List<PartiqlPhysical.VarDecl>
        ): List<PartiqlPhysical.VarDecl> = accumulator + node

        /**
         * Avoids recursion into expressions, since these may contain sub-queries with other var-decls that we don't
         * care about here.
         */
        override fun walkExpr(
            node: PartiqlPhysical.Expr,
            accumulator: List<PartiqlPhysical.VarDecl>
        ): List<PartiqlPhysical.VarDecl> {
            return accumulator
        }
    }.walkBexpr(this, emptyList())

private suspend fun RelationScope.yieldPaddedUnknowns(
    rightVariableIndexes: List<Int>,
    state: EvaluatorState,
    valueFactory: ExprValueFactory
) {
    rightVariableIndexes.forEach { state.registers[it] = valueFactory.nullValue }
    yield()
}

private fun PartiqlPhysical.Expr.isLitTrue() =
    this is PartiqlPhysical.Expr.Lit && this.value is BoolElement && this.value.booleanValue
