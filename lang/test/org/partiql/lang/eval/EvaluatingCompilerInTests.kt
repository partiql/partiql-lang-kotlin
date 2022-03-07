package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.util.ArgumentsProviderBase

class EvaluatingCompilerInTests : EvaluatorTestBase() {

    // Basic tests for behavior that is unchanged all [TypingMode]s, not including unknown propagation
    // as that is already well tested in [EvaluatingCompilerUnknownValuesTest].
    @ParameterizedTest
    @ArgumentsSource(BasicInOperatorTestCases::class)
    fun basicInOperatorTests(tc: EvaluatorTestCase) = runTestCaseInLegacyAndPermissiveModes(tc, EvaluationSession.standard())
    class BasicInOperatorTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // These cases get the optimized thunk since the right-operand consists solely of known literal values
            EvaluatorTestCase("0 IN (1, 2, 3)", "FALSE"),
            EvaluatorTestCase("1 IN (1, 2, 3)", "TRUE"),
            EvaluatorTestCase("2 IN (1, 2, 3)", "TRUE"),
            EvaluatorTestCase("3 IN (1, 2, 3)", "TRUE"),
            EvaluatorTestCase("4 IN (1, 2, 3)", "FALSE"),

            // These cases get the un-optimized thunk since the right-operand does not consist solely of known literal values
            EvaluatorTestCase("0 IN (1, 1+1, 3)", "FALSE"),
            EvaluatorTestCase("1 IN (1, 1+1, 3)", "TRUE"),
            EvaluatorTestCase("2 IN (1, 1+1, 3)", "TRUE"),
            EvaluatorTestCase("3 IN (1, 1+1, 3)", "TRUE"),
            EvaluatorTestCase("4 IN (1, 1+1, 3)", "FALSE")
            // Note: if we start doing compile-time reduction of literal values, we'll need to change `1+1` above to
            // something else
        )
    }

    // Tests the differences between [TypingMode.LEGACY] and [TypingMode.PERMISSIVE] for the IN operator.
    @ParameterizedTest
    @ArgumentsSource(InRightOpNotASequenceCases::class)
    fun inRightOpNotASequence(tc: EvaluatorTestCase) = runTestCase(tc, EvaluationSession.standard())
    class InRightOpNotASequenceCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // TypingMode.LEGACY returns `false` when the right-hand operand is not a sequence
            // TypingMode.PERMISSIVE the same returns `MISSING`
            EvaluatorTestCase(
                groupName = "IN--right operand not a sequence (TypingMode.LEGACY)",
                sqlUnderTest = "1 IN 'so long'",
                expectedSql = "false",
                compOptions = CompOptions.STANDARD
            ),
            EvaluatorTestCase(
                groupName = "IN--right operand not a sequence (TypingMode.PERMISSIVE)",
                sqlUnderTest = "1 IN 'thanks for all the fish'",
                expectedSql = "MISSING",
                compOptions = CompOptions.PERMISSIVE
            )
        )
    }
}
