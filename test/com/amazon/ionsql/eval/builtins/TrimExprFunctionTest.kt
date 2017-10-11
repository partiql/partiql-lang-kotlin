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
    private val env = Environment(Bindings.empty(), Bindings.empty(), Bindings.empty(), RegisterBank(0))

    private val subject = TrimExprFunction(ion)

    private fun callTrim(vararg args: Any) = subject.call(env, args.map { it.exprValue() }.toList()).stringValue()

    @Test
    fun oneArgument() = assertEquals("string", callTrim("   string   "))

    @Test
    fun twoArguments() = assertEquals("string   ", callTrim("leading", "   string   "))

    @Test
    fun threeArguments() = assertEquals("string", callTrim("both", "a", "aaaaaaaaaastringaaaaaaa"))

    @Test
    fun zeroArguments() {
        assertThatThrownBy { callTrim() }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("Trim takes between 1 and 3 arguments, received: 0")
    }

    @Test
    fun moreThanThreeArguments() {
        assertThatThrownBy { callTrim("both", "a", "aaaaaaaaaastringaaaaaaa", "a") }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("Trim takes between 1 and 3 arguments, received: 4")
    }

    @Test
    fun invalidSpecification() {
        assertThatThrownBy { callTrim("invalid", "string") }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasMessageContaining("'invalid' is an unknown trim specification, valid vales: both, leading, trailing")
    }

    @Test
    fun wrongSpecificationType() {
        assertThatThrownBy { assertEquals("string", callTrim(1, "string")) }
            .isExactlyInstanceOf(EvaluationException::class.java)
            .hasRootCauseExactlyInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Expected text: 1")
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