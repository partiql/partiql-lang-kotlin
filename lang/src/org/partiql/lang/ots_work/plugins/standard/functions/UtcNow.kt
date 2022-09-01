package org.partiql.lang.ots_work.plugins.standard.functions

import com.amazon.ion.Timestamp
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.types.TimeStampType
import org.partiql.lang.ots_work.plugins.standard.valueFactory

data class UtcNow(
    val now: Timestamp
) : ScalarFunction {
    override val signature = FunctionSignature(
        "utcnow",
        emptyList(),
        returnType = listOf(TimeStampType)
    )

    override fun callWithRequired(required: List<ExprValue>): ExprValue =
        valueFactory.newTimestamp(now)
}
