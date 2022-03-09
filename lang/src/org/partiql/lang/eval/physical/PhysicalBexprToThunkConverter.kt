package org.partiql.lang.eval.physical

import com.amazon.ionelement.api.BoolElement
import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.ThunkEnv
import org.partiql.lang.eval.address
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.name
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationScope
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.eval.sourceLocationMeta
import org.partiql.lang.eval.unnamedValue
import org.partiql.lang.util.toIntExact

private val DEFAULT_IMPL = PartiqlPhysical.build { impl("default") }

internal class PhysicalBexprToThunkConverter(
    private val exprConverter: PhysicalExprToThunkConverter,
    private val valueFactory: ExprValueFactory,
) : PartiqlPhysical.Bexpr.Converter<RelationThunkEnv> {

    private fun blockNonDefaultImpl(i: PartiqlPhysical.Impl) {
        if(i != DEFAULT_IMPL) {
            TODO("Support non-default operator implementations")
        }
    }

    override fun convertProject(node: PartiqlPhysical.Bexpr.Project): RelationThunkEnv {
        TODO("not implemented")
    }

    override fun convertScan(node: PartiqlPhysical.Bexpr.Scan): RelationThunkEnv {
        blockNonDefaultImpl(node.i)

        val exprThunk = exprConverter.convert(node.expr)
        val asIndex = node.asDecl.index.value.toIntExact()
        val atIndex = node.atDecl?.index?.value?.toIntExact() ?: -1
        val byIndex = node.byDecl?.index?.value?.toIntExact() ?: -1

        return relationThunk(node.metas) { env ->
            val valueToScan = exprThunk.invoke(env)

            // coerces non-collection types to a singleton Sequence<>.
            val rows: Sequence<ExprValue> = when(valueToScan.type) {
                ExprValueType.LIST, ExprValueType.BAG -> valueToScan.asSequence()
                else -> sequenceOf(valueToScan)
            }

            relation(RelationType.BAG) {
                var rowsIter: Iterator<ExprValue> = rows.iterator()
                while(rowsIter.hasNext()) {
                    val item = rowsIter.next()
                    env.registers[asIndex] = item.unnamedValue() // Remove any ordinal (output is a bag)

                    if (atIndex >= 0) {
                        env.registers[atIndex] = item.name ?: valueFactory.missingValue
                    }

                    if (byIndex >= 0) {
                        env.registers[byIndex] = item.address ?: valueFactory.missingValue
                    }
                    yield()
                }
            }
        }
    }

    override fun convertFilter(node: PartiqlPhysical.Bexpr.Filter): RelationThunkEnv {
        blockNonDefaultImpl(node.i)

        val predicateThunk = exprConverter.convert(node.predicate)
        val sourceThunk = this.convert(node.source)

        return relationThunk(node.metas) { env ->
            val sourceToFilter = sourceThunk(env)
            createFilterRelItr(sourceToFilter, predicateThunk, env)
        }
    }

    override fun convertJoin(node: PartiqlPhysical.Bexpr.Join): RelationThunkEnv {
        blockNonDefaultImpl(node.i)

        val leftThunk = this.convert(node.left)
        val rightThunk = this.convert(node.right)
        val predicateThunk = exprConverter.convert(node.predicate).takeIf { !node.predicate.isLitTrue() }

        return when (node.joinType) {
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
                    predicateThunk = predicateThunk
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
                    predicateThunk = predicateThunk
                )
            }
            is PartiqlPhysical.JoinType.Full -> TODO("Full join")
        }
    }

    private fun createInnerJoinThunk(
        joinMetas: MetaContainer,
        leftThunk: RelationThunkEnv,
        rightThunk: RelationThunkEnv,
        predicateThunk: ThunkEnv?
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
        env: Environment
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
        predicateThunk: ThunkEnv?
    ) =
        relationThunk(joinMetas) { env ->
            createLeftJoinRelItr(leftThunk, rightThunk, rightVariableIndexes, predicateThunk, env)
        }

    /**
     * Like [createCrossJoinRelItr], but the right-hand relation is padded with unknown values in the event
     * that it is empty or that the predicate does not match.
     */
    private fun createLeftJoinRelItr(
        leftThunk: RelationThunkEnv,
        rightThunk: RelationThunkEnv,
        rightVariableIndexes: List<Int>,
        predicateThunk: ThunkEnv?,
        env: Environment
    ): RelationIterator {
        return if(predicateThunk == null) {
            relation(RelationType.BAG) {
                val leftItr = leftThunk(env)
                while (leftItr.nextRow()) {
                    val rightItr = rightThunk(env)
                    // if the rightItr does has a row...
                    if (rightItr.nextRow()) {
                        yield() // yield current row
                        yieldAll(rightItr) // yield remaining rows
                    } else {
                        // no row--yield padded row
                        yieldPaddedUnknowns(rightVariableIndexes, env)
                    }
                }
            }
        }
        else {
            relation(RelationType.BAG) {
                val leftItr = leftThunk(env)
                while (leftItr.nextRow()) {
                    val rightItr = rightThunk(env)
                    var yieldedSomething = false
                    while (rightItr.nextRow()) {
                        if (coercePredicateResult(predicateThunk(env))) {
                            yield()
                            yieldedSomething = true
                        }
                    }
                    // If we still haven't yielded anything, we still need to emit a row with right-hand side variables
                    // padded with unknowns.
                    if (!yieldedSomething) {
                        yieldPaddedUnknowns(rightVariableIndexes, env)
                    }
                }
            }
        }
    }

    private suspend fun RelationScope.yieldPaddedUnknowns(
        rightVariableIndexes: List<Int>,
        env: Environment
    ) {
        rightVariableIndexes.forEach { env.registers[it] = valueFactory.nullValue }
        yield()
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


    private fun createFilterRelItr(
        relItr: RelationIterator,
        predicateThunk: ThunkEnv,
        env: Environment
    ) = relation(RelationType.BAG) {
        while(true) {
            if (!relItr.nextRow()) {
                break
            } else {
                val matches = predicateThunk(env)
                if(coercePredicateResult(matches)) {
                    yield()
                }
            }
        }
    }

    private fun coercePredicateResult(value: ExprValue): Boolean =
        when {
            value.isUnknown() -> false
            else -> value.booleanValue() //<-- throws if [value] is not a boolean.
        }

    override fun convertOffset(node: PartiqlPhysical.Bexpr.Offset): RelationThunkEnv {
        val rowCountThunk = exprConverter.convert(node.rowCount)
        val sourceThunk = this.convert(node.source)
        val rowCountLocation = node.rowCount.metas.sourceLocationMeta
        return relationThunk(node.metas) { env ->
            val skipCount: Long = evalOffsetRowCount(rowCountThunk, env, rowCountLocation)
            relation(RelationType.BAG) {
                val sourceRel = sourceThunk(env)
                var rowCount = 0L
                while(rowCount++ < skipCount) {
                    // stop iterating if we finish run out of rows before we hit the offset.
                    if(!sourceRel.nextRow()) {
                        return@relation
                    }
                }

                yieldAll(sourceRel)
            }
        }
    }

    override fun convertLimit(node: PartiqlPhysical.Bexpr.Limit): RelationThunkEnv {
        val rowCountThunk = exprConverter.convert(node.rowCount)
        val sourceThunk = this.convert(node.source)
        val rowCountLocation = node.rowCount.metas.sourceLocationMeta
        return relationThunk(node.metas) { env ->
            val limitCount = evalLimitRowCount(rowCountThunk, env, rowCountLocation)
            val rowIter = sourceThunk(env)
            relation(RelationType.BAG) {
                var rowCount = 0L
                while(rowCount++ < limitCount && rowIter.nextRow()) {
                    yield()
                }
            }
        }
    }
}

private fun PartiqlPhysical.Expr.isLitTrue() =
    this is PartiqlPhysical.Expr.Lit && this.value is BoolElement && this.value.booleanValue


