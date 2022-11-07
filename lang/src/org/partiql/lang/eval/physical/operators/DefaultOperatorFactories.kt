package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.address
import org.partiql.lang.eval.name
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.evalLimitRowCount
import org.partiql.lang.eval.physical.evalOffsetRowCount
import org.partiql.lang.eval.physical.window.ExperimentalWindowFunc
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.eval.unnamedValue
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

/**
 * A collection of all the default relational operator implementations provided by PartiQL.
 *
 * By default, the query planner will select these as the implementations for all relational operators, but
 * alternate implementations may be provided and chosen by physical plan passes.
 *
 * @see [org.partiql.lang.planner.PlannerPipeline.Builder.addPhysicalPlanPass]
 * @see [org.partiql.lang.planner.PlannerPipeline.Builder.addRelationalOperatorFactory]
 */
internal val DEFAULT_RELATIONAL_OPERATOR_FACTORIES = listOf(

    AggregateOperatorFactoryDefault,
    SortOperatorFactoryDefault,
    UnpivotOperatorFactoryDefault,
    @OptIn(ExperimentalWindowFunc::class)
    WindowOperatorFactoryDefault,

    object : ScanRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(
            impl: PartiqlPhysical.Impl,
            expr: ValueExpression,
            setAsVar: SetVariableFunc,
            setAtVar: SetVariableFunc?,
            setByVar: SetVariableFunc?
        ) = RelationExpression { state ->
            val valueToScan = expr(state)

            // coerces non-collection types to a singleton Sequence<>.
            val rows: Sequence<ExprValue> = when (valueToScan.type) {
                ExprValueType.LIST, ExprValueType.BAG -> valueToScan.asSequence()
                else -> sequenceOf(valueToScan)
            }

            relation(RelationType.BAG) {
                val rowsIter: Iterator<ExprValue> = rows.iterator()
                while (rowsIter.hasNext()) {
                    val item = rowsIter.next()

                    // .unnamedValue() removes any ordinal that might exist on item
                    setAsVar(state, item.unnamedValue())

                    if (setAtVar != null) {
                        setAtVar(state, item.name ?: state.valueFactory.missingValue)
                    }

                    if (setByVar != null) {
                        setByVar(state, item.address ?: state.valueFactory.missingValue)
                    }
                    yield()
                }
            }
        }
    },
    object : FilterRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(impl: PartiqlPhysical.Impl, predicate: ValueExpression, sourceBexpr: RelationExpression) =
            RelationExpression { state ->
                val sourceToFilter = sourceBexpr.evaluate(state)
                createFilterRelItr(sourceToFilter, predicate, state)
            }
    },
    object : JoinRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(
            impl: PartiqlPhysical.Impl,
            joinType: PartiqlPhysical.JoinType,
            leftBexpr: RelationExpression,
            rightBexpr: RelationExpression,
            predicateExpr: ValueExpression?,
            setLeftSideVariablesToNull: (EvaluatorState) -> Unit,
            setRightSideVariablesToNull: (EvaluatorState) -> Unit
        ): RelationExpression =
            when (joinType) {
                is PartiqlPhysical.JoinType.Inner -> {
                    if (predicateExpr == null) {
                        RelationExpression { state ->
                            createCrossJoinRelItr(leftBexpr, rightBexpr, state)
                        }
                    } else {
                        RelationExpression { state ->
                            val crossJoinRelItr = createCrossJoinRelItr(leftBexpr, rightBexpr, state)
                            createFilterRelItr(crossJoinRelItr, predicateExpr, state)
                        }
                    }
                }
                is PartiqlPhysical.JoinType.Left -> {
                    RelationExpression { state ->
                        createLeftJoinRelItr(
                            leftRel = leftBexpr,
                            rightRel = rightBexpr,
                            predicateExpr = predicateExpr,
                            setRightSideVariablesToNull = setRightSideVariablesToNull,
                            state = state
                        )
                    }
                }
                is PartiqlPhysical.JoinType.Right -> {
                    // Note that this is the same as the left join but the right and left sides are swapped.
                    RelationExpression { state ->
                        createLeftJoinRelItr(
                            leftRel = rightBexpr,
                            rightRel = leftBexpr,
                            predicateExpr = predicateExpr,
                            setRightSideVariablesToNull = setLeftSideVariablesToNull,
                            state = state
                        )
                    }
                }
                is PartiqlPhysical.JoinType.Full -> TODO("Full join")
            }
    },
    object : OffsetRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(
            impl: PartiqlPhysical.Impl,
            rowCountExpr: ValueExpression,
            sourceBexpr: RelationExpression
        ): RelationExpression =
            RelationExpression { state ->
                val skipCount: Long = evalOffsetRowCount(rowCountExpr, state)
                val sourceRel = sourceBexpr.evaluate(state)
                relation(sourceRel.relType) {
                    var rowCount = 0L
                    while (rowCount++ < skipCount) {
                        // stop iterating if we run out of rows before we hit the offset.
                        if (!sourceRel.nextRow()) {
                            return@relation
                        }
                    }
                    yieldAll(sourceRel)
                }
            }
    },
    object : LimitRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(
            impl: PartiqlPhysical.Impl,
            rowCountExpr: ValueExpression,
            sourceBexpr: RelationExpression
        ): RelationExpression =
            RelationExpression { state ->
                val limitCount = evalLimitRowCount(rowCountExpr, state)
                val rowIter = sourceBexpr.evaluate(state)
                relation(rowIter.relType) {
                    var rowCount = 0L
                    while (rowCount++ < limitCount && rowIter.nextRow()) {
                        yield()
                    }
                }
            }
    },
    object : LetRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(
            impl: PartiqlPhysical.Impl,
            sourceBexpr: RelationExpression,
            bindings: List<VariableBinding>
        ) = RelationExpression { state ->
            val sourceItr = sourceBexpr.evaluate(state)

            relation(sourceItr.relType) {
                while (sourceItr.nextRow()) {
                    bindings.forEach {
                        it.setFunc(state, it.expr(state))
                    }
                    yield()
                }
            }
        }
    }
)
