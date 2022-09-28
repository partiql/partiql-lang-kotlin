package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.address
import org.partiql.lang.eval.name
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.eval.syntheticColumnName
import org.partiql.lang.eval.unnamedValue

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Scan] operator.*/
abstract class UnpivotRelationalOperatorFactory(name: String) : RelationalOperatorFactory {
    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.UNPIVOT, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Scan]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,
        /** Invoke to obtain the value to be iterated over.*/
        expr: ValueExpression,
        /** Invoke to set the `AS` variable binding. */
        setAsVar: SetVariableFunc,
        /** Invoke to set the `AT` variable binding, if non-null */
        setAtVar: SetVariableFunc?,
        /** Invoke to set the `BY` variable binding, if non-null. */
        setByVar: SetVariableFunc?
    ): RelationExpression
}

internal class UnpivotOperator(name: String) : UnpivotRelationalOperatorFactory(name) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        expr: ValueExpression,
        setAsVar: SetVariableFunc,
        setAtVar: SetVariableFunc?,
        setByVar: SetVariableFunc?
    ): RelationExpression =
        RelationExpression { state ->

            fun ExprValue.unpivot(state: EvaluatorState): ExprValue = when (type) {
                ExprValueType.STRUCT, ExprValueType.MISSING -> this

                else -> state.valueFactory.newBag(
                    listOf(
                        this.namedValue(state.valueFactory.newString(syntheticColumnName(0)))
                    )
                )
            }

            val originalValue = expr(state)

            val unpivot = originalValue.unpivot(state)

            relation(RelationType.BAG) {
                val iter = unpivot.iterator()
                while (iter.hasNext()) {
                    val item = iter.next()

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
}
