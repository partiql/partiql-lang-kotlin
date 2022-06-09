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
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.eval.unnamedValue
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

internal val DEFAULT_OPERATOR_FACTORIES = listOf(
    object : ScanPhysicalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(
            impl: PartiqlPhysical.Impl,
            expr: ValueExpr,
            setAsVar: SetVariableFunc,
            setAtVar: SetVariableFunc?,
            setByVar: SetVariableFunc?
        ): BindingsExpr =
            BindingsExpr { state ->
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

                        // Note that although we could access [EvaluatorState.registers] directly within relationThunk,
                        // we don't as a means to demonstrate the proper way that custom operator implementations should
                        // set the values of variables.  See [toSetFunc] and [SetVarFunc] for more info.
                        setAsVar(state, item.unnamedValue()) // Remove any ordinal (output is a bag)

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
    object : FilterPhysicalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(impl: PartiqlPhysical.Impl, predicate: ValueExpr, sourceBexpr: BindingsExpr) =
            BindingsExpr { state ->
                val sourceToFilter = sourceBexpr(state)
                createFilterRelItr(sourceToFilter, predicate, state)
            }
    },
    object : JoinPhysicalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(
            impl: PartiqlPhysical.Impl,
            joinType: PartiqlPhysical.JoinType,
            leftBexpr: BindingsExpr,
            rightBexpr: BindingsExpr,
            predicateExpr: ValueExpr?,
            setLeftSideVariablesToNull: (EvaluatorState) -> Unit,
            setRightSideVariablesToNull: (EvaluatorState) -> Unit
        ): BindingsExpr =
            when (joinType) {
                is PartiqlPhysical.JoinType.Inner -> {
                    if (predicateExpr == null) {
                        BindingsExpr { state ->
                            createCrossJoinRelItr(leftBexpr, rightBexpr, state)
                        }
                    } else {
                        BindingsExpr { state ->
                            val crossJoinRelItr = createCrossJoinRelItr(leftBexpr, rightBexpr, state)
                            createFilterRelItr(crossJoinRelItr, predicateExpr, state)
                        }
                    }
                }
                is PartiqlPhysical.JoinType.Left -> {
                    BindingsExpr { state ->
                        createLeftJoinRelItr(
                            leftBexpr = leftBexpr,
                            rightBexpr = rightBexpr,
                            predicateExpr = predicateExpr,
                            setRightSideVariablesToNull = setRightSideVariablesToNull,
                            state = state
                        )
                    }
                }
                is PartiqlPhysical.JoinType.Right -> {
                    // Note that this is the same as the left join but the right and left sides are swapped.
                    BindingsExpr { state ->
                        createLeftJoinRelItr(
                            leftBexpr = rightBexpr,
                            rightBexpr = leftBexpr,
                            predicateExpr = predicateExpr,
                            setRightSideVariablesToNull = setLeftSideVariablesToNull,
                            state = state
                        )
                    }
                }
                is PartiqlPhysical.JoinType.Full -> TODO("Full join")
            }
    },
    object : OffsetPhysicalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(
            impl: PartiqlPhysical.Impl,
            rowCountExpr: ValueExpr,
            sourceBexpr: BindingsExpr
        ): BindingsExpr =
            BindingsExpr { state ->
                val skipCount: Long = evalOffsetRowCount(rowCountExpr, state)
                relation(RelationType.BAG) {
                    val sourceRel = sourceBexpr(state)
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
    object : LimitPhysicalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(
            impl: PartiqlPhysical.Impl,
            rowCountExpr: ValueExpr,
            sourceBexpr: BindingsExpr
        ): BindingsExpr =
            BindingsExpr { state ->
                val limitCount = evalLimitRowCount(rowCountExpr, state)
                val rowIter = sourceBexpr(state)
                relation(RelationType.BAG) {
                    var rowCount = 0L
                    while (rowCount++ < limitCount && rowIter.nextRow()) {
                        yield()
                    }
                }
            }
    },
    object : LetPhysicalOperatorFactory(DEFAULT_IMPL_NAME) {
        override fun create(
            impl: PartiqlPhysical.Impl,
            sourceBexpr: BindingsExpr,
            bindings: List<VariableBinding>
        ) = BindingsExpr { state ->
            val sourceItr = sourceBexpr(state)

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
