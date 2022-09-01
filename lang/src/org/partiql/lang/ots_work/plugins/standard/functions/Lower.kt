package org.partiql.lang.ots_work.plugins.standard.functions

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.valueFactory

object Lower : ScalarFunction {
    override val signature: FunctionSignature =
        FunctionSignature(
            "lower",
            requiredParameters = listOf(listOf(StringType, SymbolType)),
            returnType = listOf(StringType)
        )

    override fun callWithRequired(required: List<ExprValue>): ExprValue =
        valueFactory.newString(required.first().stringValue().toLowerCase())
}
