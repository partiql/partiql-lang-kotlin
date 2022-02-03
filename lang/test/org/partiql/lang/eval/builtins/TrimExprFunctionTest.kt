package org.partiql.lang.eval.builtins

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.partiql.lang.TestBase
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.RequiredArgs
import org.partiql.lang.eval.RequiredWithVariadic
import org.partiql.lang.eval.call
import org.partiql.lang.eval.stringValue

/**
 * Tests for [TrimExprFunction], most tests are done e2e through the evaluator, see [TrimEvaluationTest]
 */
class TrimExprFunctionTest : TestBase() {
    private val env = Environment.standard()

    private val subject = TrimExprFunction(valueFactory)

    private fun callTrim(vararg args: Any): String {
        val args = args.map { anyToExprValue(it) }.toList()
        val required = args.take(1)
        val rest = args.drop(1)
        return when (rest.size) {
            0 -> subject.call(env, RequiredArgs(required))
            else -> subject.call(env, RequiredWithVariadic(required, rest))
        }.stringValue()
    }

    @Test
    fun oneArgument() = assertEquals("string", callTrim("   string   "))

    @Test
    fun twoArguments() = assertEquals("string   ", callTrim("leading", "   string   "))

    @Test
    fun twoArguments2() = assertEquals("string", callTrim("12", "1212string1212"))

    @Test
    fun twoArgumentsBoth() = assertEquals("", callTrim("both", "      "))

    @Test
    fun twoArgumentsLeading() = assertEquals("", callTrim("leading", "      "))

    @Test
    fun twoArgumentsTrailing() = assertEquals("", callTrim("trailing", "      "))

    @Test
    fun threeArguments() = assertEquals("string", callTrim("both", "a", "aaaaaaaaaastringaaaaaaa"))

    @Test
    fun wrongSpecificationType() {
        assertThatThrownBy { assertEquals("string", callTrim(1, "string")) }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("with two arguments trim's first argument must be either the specification or a 'to remove' string")
    }

    @Test
    fun wrongToRemoveType() {
        assertThatThrownBy { assertEquals("string", callTrim("both", 1, "string")) }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("Expected text: 1")
    }
}