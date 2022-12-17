/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.physical.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.address
import org.partiql.lang.eval.name
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.eval.syntheticColumnName
import org.partiql.lang.eval.unnamedValue
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

internal object UnpivotOperatorFactoryDefault : UnpivotOperatorFactory(DEFAULT_IMPL_NAME) {
    override fun create(
        expr: ValueExpression,
        setAsVar: SetVariableFunc,
        setAtVar: SetVariableFunc?,
        setByVar: SetVariableFunc?
    ): RelationExpression = UnpivotOperatorDefault(expr, setAsVar, setAtVar, setByVar)
}

internal class UnpivotOperatorDefault(
    private val expr: ValueExpression,
    private val setAsVar: SetVariableFunc,
    private val setAtVar: SetVariableFunc?,
    private val setByVar: SetVariableFunc?
) : RelationExpression {
    override fun evaluate(state: EvaluatorState): RelationIterator {
        val originalValue = expr(state)
        val unpivot = originalValue.unpivot(state)

        return relation(RelationType.BAG) {
            val iter = unpivot.iterator()
            while (iter.hasNext()) {
                val item = iter.next()
                setAsVar(state, item.unnamedValue())
                setAtVar?.let { it(state, item.name ?: ExprValue.missingValue) }
                setByVar?.let { it(state, item.address ?: ExprValue.missingValue) }
                yield()
            }
        }
    }

    private fun ExprValue.unpivot(state: EvaluatorState): ExprValue = when (type) {
        ExprValueType.STRUCT, ExprValueType.MISSING -> this
        else -> ExprValue.newBag(
            listOf(
                this.namedValue(ExprValue.newString(syntheticColumnName(0)))
            )
        )
    }
}
