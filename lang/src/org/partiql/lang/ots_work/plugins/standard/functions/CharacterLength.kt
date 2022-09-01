package org.partiql.lang.ots_work.plugins.standard.functions

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.valueFactory

object CharacterLength : ScalarFunction {
    override val signature: FunctionSignature
        get() {
            val element = listOf(StringType, SymbolType)
            return FunctionSignature(
                "character_length",
                listOf(element),
                returnType = listOf(IntType)
            )
        }

    override fun callWithRequired(required: List<ExprValue>): ExprValue {
        val s = required.first().stringValue()
        return valueFactory.newInt(s.codePointCount(0, s.length))
    }
}
