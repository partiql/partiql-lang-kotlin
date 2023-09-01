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

import org.partiql.errors.ErrorCode
import org.partiql.lang.Ident
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprAggregator
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.createUniqueExprValueFilter
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.numberValue
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.div
import org.partiql.lang.util.exprValue
import org.partiql.lang.util.plus

internal sealed class Accumulator(
    internal open val filter: (ExprValue) -> Boolean
) : ExprAggregator {
    companion object {
        internal fun create(funcName: String, quantifier: PartiqlPhysical.SetQuantifier): Accumulator {
            val filter = when (quantifier) {
                is PartiqlPhysical.SetQuantifier.Distinct -> createUniqueExprValueFilter()
                is PartiqlPhysical.SetQuantifier.All -> { _: ExprValue -> true }
            }
            return when (Ident.normalizeRegular(funcName.trim())) {
                "min" -> AccumulatorMin(filter)
                "max" -> AccumulatorMax(filter)
                "avg" -> AccumulatorAvg(filter)
                "count" -> AccumulatorCount(filter)
                "sum" -> AccumulatorSum(filter)
                "group_as" -> AccumulatorGroupAs(filter)
                "every" -> AccumulatorEvery(filter)
                "any" -> AccumulatorAnySome(filter)
                "some" -> AccumulatorAnySome(filter)
                else -> throw IllegalArgumentException("Unsupported aggregation function: $funcName")
            }
        }
    }

    override fun next(value: ExprValue) {
        if (value.isUnknown() || filter.invoke(value).not()) return
        nextValue(value)
    }

    abstract fun nextValue(value: ExprValue)
}

internal class AccumulatorSum(
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    var sum: Number? = null

    override fun nextValue(value: ExprValue) {
        checkIsNumberType(funcName = "SUM", value = value)
        if (sum == null) sum = 0L
        this.sum = value.numberValue() + this.sum!!
    }

    override fun compute(): ExprValue {
        return sum?.exprValue() ?: ExprValue.nullValue
    }
}

internal class AccumulatorAvg(
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    var sum: Number = 0.0
    var count: Long = 0L

    override fun nextValue(value: ExprValue) {
        checkIsNumberType(funcName = "AVG", value = value)
        this.sum += value.numberValue()
        this.count += 1L
    }

    override fun compute(): ExprValue = when (count) {
        0L -> ExprValue.nullValue
        else -> (sum / bigDecimalOf(count)).exprValue()
    }
}

internal class AccumulatorMax(
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    var max: ExprValue = ExprValue.nullValue

    override fun nextValue(value: ExprValue) {
        max = comparisonAccumulator(NaturalExprValueComparators.NULLS_LAST_DESC)(max, value)
    }

    override fun compute(): ExprValue = max
}

internal class AccumulatorMin(
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    var min: ExprValue = ExprValue.nullValue

    override fun nextValue(value: ExprValue) {
        min = comparisonAccumulator(NaturalExprValueComparators.NULLS_LAST_ASC)(min, value)
    }

    override fun compute(): ExprValue = min
}

internal class AccumulatorCount(
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    var count: Long = 0L

    override fun nextValue(value: ExprValue) {
        this.count += 1L
    }

    override fun compute(): ExprValue = count.exprValue()
}

internal class AccumulatorEvery(
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    private var res: ExprValue? = null
    override fun nextValue(value: ExprValue) {
        checkIsBooleanType("EVERY", value)
        res = res?.let { ExprValue.newBoolean(it.booleanValue() && value.booleanValue()) } ?: value
    }

    override fun compute(): ExprValue = res ?: ExprValue.nullValue
}

internal class AccumulatorAnySome(
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    private var res: ExprValue? = null
    override fun nextValue(value: ExprValue) {
        checkIsBooleanType("ANY/SOME", value)
        res = res?.let { ExprValue.newBoolean(it.booleanValue() || value.booleanValue()) } ?: value
    }

    override fun compute(): ExprValue = res ?: ExprValue.nullValue
}

internal class AccumulatorGroupAs(
    internal override val filter: (ExprValue) -> Boolean
) : Accumulator(filter = filter) {

    val exprValues = mutableListOf<ExprValue>()

    override fun nextValue(value: ExprValue) {
        exprValues.add(value)
    }

    override fun compute(): ExprValue = ExprValue.newBag(exprValues)
}

private fun comparisonAccumulator(comparator: NaturalExprValueComparators): (ExprValue?, ExprValue) -> ExprValue =
    { left, right ->
        when {
            left == null || comparator.compare(left, right) > 0 -> right
            else -> left
        }
    }

internal fun checkIsNumberType(funcName: String, value: ExprValue) {
    if (!value.type.isNumber) {
        errNoContext(
            message = "Aggregate function $funcName expects arguments of NUMBER type but the following value was provided: $value, with type of ${value.type}",
            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
            internal = false
        )
    }
}

internal fun checkIsBooleanType(funcName: String, value: ExprValue) {
    if (value.type != ExprValueType.BOOL) {
        errNoContext(
            message = "Aggregate function $funcName expects arguments of BOOL type but the following value was provided: $value, with type of ${value.type}",
            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
            internal = false
        )
    }
}
