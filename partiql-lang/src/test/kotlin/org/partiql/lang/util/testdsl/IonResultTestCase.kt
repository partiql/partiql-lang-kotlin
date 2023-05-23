package org.partiql.lang.util.testdsl

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EVALUATOR_TEST_SUITE
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.evaluatortestframework.CompilerPipelineFactory
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.eval.evaluatortestframework.PartiQLCompilerPipelineFactory
import org.partiql.lang.eval.evaluatortestframework.PipelineEvaluatorTestAdapter
import org.partiql.lang.mockdb.MockDb
import org.partiql.lang.syntax.impl.PartiQLPigParser

/** Defines a test case for query evaluation. */
data class IonResultTestCase(
    /** Name of the test.  Shows in IDE's test runner.  Not required to be unique. */
    val name: String,

    /** The test's group name, if any. */
    val group: String? = null,

    /** Useful additional details about the test. */
    val note: String? = null,

    /** The query to be evaluated. */
    val sqlUnderTest: String,

    /**
     * The expected result when run in [org.partiql.lang.eval.TypingMode.LEGACY], formatted in Ion text.
     */
    val expectedLegacyModeIonResult: String,

    /**
     * The expected result when run in [org.partiql.lang.eval.TypingMode.PERMISSIVE], formatted in Ion text.
     */
    val expectedPermissiveModeIonResult: String,

    /**
     * If the test unexpectedly succeeds, cause the unit test to fail.
     *
     * This should be set to true for all tests which are on a "fail list".
     *
     * When a failing test is fixed, it should be removed from all fail lists.  This ensures that all passing tests
     * are removed from all fail lists.  Without this, our fail lists will likely include passing tests.
     */
    val expectFailure: Boolean = false,

    /** The compile options to use. */
    val compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },

    /** An optional block in which to execute additional assertions. */
    val extraAssertions: (ExprValue) -> Unit
) {
    private val cleanedSqlUnderTest =
        sqlUnderTest.replace("\n", "")

    override fun toString(): String = listOfNotNull(group, name, note, cleanedSqlUnderTest).joinToString(" - ")

    fun toStatementTestCase(): StatementTestCase =
        assertDoesNotThrow("IonResultTestCase ${toString()} should not throw when parsing") {
            StatementTestCase(name, PartiQLPigParser().parseAstStatement(sqlUnderTest))
        }
}

internal fun IonResultTestCase.runTestCase(
    db: MockDb,
    target: EvaluatorTestTarget,
    compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
) {
    val adapter = PipelineEvaluatorTestAdapter(
        when (target) {
            EvaluatorTestTarget.COMPILER_PIPELINE -> CompilerPipelineFactory()
            EvaluatorTestTarget.PARTIQL_PIPELINE -> PartiQLCompilerPipelineFactory()
            // We don't support ALL_PIPELINES here because each pipeline needs a separate skip list, which
            // is decided by the caller of this function.
            EvaluatorTestTarget.ALL_PIPELINES -> error("May only test one pipeline at a time with IonResultTestCase")
        }
    )

    val session = EvaluationSession.build {
        globals(db.valueBindings)
        parameters(EVALUATOR_TEST_SUITE.createParameters())
    }

    val tc = EvaluatorTestCase(
        groupName = "${this.group}:${this.name}",
        query = this.sqlUnderTest,
        expectedResult = this.expectedLegacyModeIonResult,
        expectedPermissiveModeResult = this.expectedPermissiveModeIonResult,
        expectedResultFormat = ExpectedResultFormat.ION,
        implicitPermissiveModeTest = false,
        compileOptionsBuilderBlock = this.compileOptionsBuilderBlock,
        compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
        extraResultAssertions = extraAssertions
    )

    if (!this.expectFailure) {
        adapter.runEvaluatorTestCase(tc, session)
    } else {
        val message = "We expect test \"${this.name}\" to fail, but it did not. This check exists to ensure the " +
            "failing list is up to date."

        assertThrows<Throwable>(message) {
            adapter.runEvaluatorTestCase(tc, session)
        }
    }
}
