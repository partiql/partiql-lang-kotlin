package com.amazon.ionsql.eval.builtins

import com.amazon.ion.system.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*
import org.assertj.core.api.Assertions.*
import org.junit.*
import org.junit.Assert.*

/**
 * Tests for [TrimExprFunction], most tests are done e2e through the evaluator, see [BuiltinFunctionsTest]
 */
class TrimExprFunctionTest {
    private fun Any.exprValue() = when (this) {
        is String -> this.exprValue(ion)
        is Int    -> this.exprValue(ion)
        else      -> throw RuntimeException()
    }

    private val ion = IonSystemBuilder.standard().build()
    private val env = Environment(locals = Bindings.empty(),
                                  session = EvaluationSession.standard(),
                                  registers =  RegisterBank(0))

    private val subject = TrimExprFunction(ion)

    private fun callTrim(vararg args: Any) = subject.call(env, args.map { it.exprValue() }.toList()).stringValue()

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
    fun zeroArguments() {
        assertThatThrownBy { callTrim() }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("trim takes between 1 and 3 arguments, received: 0")
    }

    @Test
    fun moreThanThreeArguments() {
        assertThatThrownBy { callTrim("both", "a", "aaaaaaaaaastringaaaaaaa", "a") }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("trim takes between 1 and 3 arguments, received: 4")
    }

    @Test
    fun wrongSpecificationType() {
        assertThatThrownBy { assertEquals("string", callTrim(1, "string")) }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("with two arguments trim's first argument must be either the specification or a 'to remove' string")
    }

    @Test
    fun wrongArgumentType() {
        assertThatThrownBy { assertEquals("string", callTrim(1)) }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasRootCauseExactlyInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Expected text: 1")
    }

    @Test
    fun wrongToRemoveType() {
        assertThatThrownBy { assertEquals("string", callTrim("both", 1, "string")) }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasRootCauseExactlyInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Expected text: 1")
    }
}