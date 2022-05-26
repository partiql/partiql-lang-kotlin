package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.util.ArgumentsProviderBase

class EvaluatingCompilerInTests : EvaluatorTestBase() {

    // Basic tests for behavior that is unchanged all [TypingMode]s, not including unknown propagation
    // as that is already well tested in [EvaluatingCompilerUnknownValuesTest].
    @ParameterizedTest
    @ArgumentsSource(BasicInOperatorTestCases::class)
    fun basicInOperatorTests(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())
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
    fun inRightOpNotASequence(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())
    class InRightOpNotASequenceCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // TypingMode.LEGACY returns `false` when the right-hand operand is not a sequence
            // TypingMode.PERMISSIVE the same returns `MISSING`
            EvaluatorTestCase(
                groupName = "IN--right operand not a sequence (TypingMode.LEGACY)",
                query = "1 IN 'so long'",
                expectedResult = "false",
                expectedPermissiveModeResult = "MISSING",
            ),
        )
    }

    /**
     * Note: if we start doing compile-time reduction of literal values, we'll need to change `1+1` in the unoptimized bags
     * to something else
     */
    @ParameterizedTest
    @ArgumentsSource(InStructOperatorTestCases::class)
    fun inStructOperatorTest(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())
    class InStructOperatorTestCases : ArgumentsProviderBase() {
        private val optimizedBag = "<< {'a': 1}, {'b':2}, {'c':3}>>"
        private val multiKeyBag = "<< {'a': 1, 'b': 1}, {'a':2, 'b': 2}>>"
        private val unoptimizedBag = "<< {'a': 1}, {'b': (1+1) }, {'c':3}>>"
        override fun getParameters(): List<Any> = listOf(
            // These cases get the optimized thunk since the right-operand consists solely of known literal values
            EvaluatorTestCase("0 IN $optimizedBag", "FALSE"),
            EvaluatorTestCase("1 IN $optimizedBag", "TRUE"),
            EvaluatorTestCase("2 IN $optimizedBag", "TRUE"),
            EvaluatorTestCase("3 IN $optimizedBag", "TRUE"),
            EvaluatorTestCase("4 IN $optimizedBag", "FALSE"),

            // These cases should all fail due to multi-key structs. We only compare single-key structs.
            EvaluatorTestCase("0 IN $multiKeyBag", "FALSE"),
            EvaluatorTestCase("1 IN $multiKeyBag", "FALSE"),
            EvaluatorTestCase("2 IN $multiKeyBag", "FALSE"),

            // These cases get the un-optimized thunk since the right-operand does not consist solely of known literal values
            EvaluatorTestCase("0 IN $unoptimizedBag", "FALSE"),
            EvaluatorTestCase("1 IN $unoptimizedBag", "TRUE"),
            EvaluatorTestCase("2 IN $unoptimizedBag", "TRUE"),
            EvaluatorTestCase("3 IN $unoptimizedBag", "TRUE"),
            EvaluatorTestCase("4 IN $unoptimizedBag", "FALSE"),

        )
    }

    /**
     * Note: if we start doing compile-time reduction of literal values, we'll need to change `1+1` in the unoptimized bags
     * to something else
     */
    @ParameterizedTest
    @ArgumentsSource(InSelectOperatorTestCases::class)
    fun inSelectOperatorTest(tc: EvaluatorTestCase) =
        runEvaluatorTestCase(tc, EvaluationSession.standard())
    class InSelectOperatorTestCases : ArgumentsProviderBase() {
        private val optimizedQuery: String = "SELECT a FROM << {'a': 1}, {'a':2}, {'a':3} >>"
        private val unoptimizedQuery: String = "SELECT a FROM << {'a': 1}, {'a':(1+1)}, {'a':3} >>"
        private val multiPairStructQuery: String = "SELECT a, b FROM << {'a': 1, 'b': 1}, {'a':2, 'b': 2 } >>"
        override fun getParameters(): List<Any> = listOf(
            // These cases get the optimized thunk since the right-operand consists solely of known literal values
            EvaluatorTestCase("0 IN ($optimizedQuery)", "FALSE"),
            EvaluatorTestCase("1 IN ($optimizedQuery)", "TRUE"),
            EvaluatorTestCase("2 IN ($optimizedQuery)", "TRUE"),
            EvaluatorTestCase("3 IN ($optimizedQuery)", "TRUE"),
            EvaluatorTestCase("4 IN ($optimizedQuery)", "FALSE"),

            // These cases should all fail due to multi-key structs. We only compare single-key structs.
            EvaluatorTestCase("0 IN ($multiPairStructQuery)", "FALSE"),
            EvaluatorTestCase("1 IN ($multiPairStructQuery)", "FALSE"),
            EvaluatorTestCase("2 IN ($multiPairStructQuery)", "FALSE"),

            // These cases get the un-optimized thunk since the right-operand does not consist solely of known literal values
            EvaluatorTestCase("0 IN ($unoptimizedQuery)", "FALSE"),
            EvaluatorTestCase("1 IN ($unoptimizedQuery)", "TRUE"),
            EvaluatorTestCase("2 IN ($unoptimizedQuery)", "TRUE"),
            EvaluatorTestCase("3 IN ($unoptimizedQuery)", "TRUE"),
            EvaluatorTestCase("4 IN ($unoptimizedQuery)", "FALSE"),
        )
    }
}
