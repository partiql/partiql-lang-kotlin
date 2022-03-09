package org.partiql.lang.eval.physical

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

    override fun convertJoin(node: PartiqlPhysical.Bexpr.Join): RelationThunkEnv {
        blockNonDefaultImpl(node.i)

        val leftThunk = this.convert(node.left)
        val rightThunk = this.convert(node.right)
        val predicateThunk = exprConverter.convert(node.predicate)

        return when (node.joinType) {
            is PartiqlPhysical.JoinType.Inner -> relationThunk(node.metas) { env ->
                val crossJoinRelItr = createCrossJoinRelItr(leftThunk, rightThunk, env)
                createFilterRelItr(crossJoinRelItr, predicateThunk, env)
            }
            is PartiqlPhysical.JoinType.Full -> TODO("Full join")
            is PartiqlPhysical.JoinType.Left -> TODO("left join")
            is PartiqlPhysical.JoinType.Right -> TODO("right join")
        }
    }

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
                when {
                    matches.isUnknown() -> { }
                    else -> {
                        if (matches.booleanValue()) {
                            yield()
                        }
                    }
                }
            }
        }
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


