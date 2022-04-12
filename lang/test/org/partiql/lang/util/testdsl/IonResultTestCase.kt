package org.partiql.lang.util.testdsl

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ION
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EVALUATOR_TEST_SUITE
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.test.AstEvaluatorTestAdapter
import org.partiql.lang.eval.test.EvaluatorTestAdapter
import org.partiql.lang.eval.test.EvaluatorTestCase
import org.partiql.lang.eval.test.ExpectedResultFormat
import org.partiql.lang.mockdb.MockDb
import org.partiql.lang.syntax.SqlParser

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

    fun toExprNodeTestCase(): ExprNodeTestCase =
        assertDoesNotThrow("IonResultTestCase ${toString()} should not throw when parsing") {
            @Suppress("DEPRECATION")
            ExprNodeTestCase(name, SqlParser(ION).parseExprNode(sqlUnderTest))
        }
}

internal fun IonResultTestCase.runTestCase(
    valueFactory: ExprValueFactory,
    db: MockDb,
    pipelineBlock: CompilerPipeline.Builder.() -> Unit = { }
) {
    val harness: EvaluatorTestAdapter = AstEvaluatorTestAdapter()

    val session = EvaluationSession.build {
        globals(db.valueBindings)
        parameters(EVALUATOR_TEST_SUITE.createParameters(valueFactory))
    }

    val tc = EvaluatorTestCase(
        groupName = "${this.group}:${this.name}",
        query = this.sqlUnderTest,
        expectedResult = this.expectedLegacyModeIonResult,
        expectedPermissiveModeResult = this.expectedPermissiveModeIonResult,
        expectedResultFormat = ExpectedResultFormat.ION,
        excludeLegacySerializerAssertions = true,
        implicitPermissiveModeTest = false,
        compileOptionsBuilderBlock = this.compileOptionsBuilderBlock,
        compilerPipelineBuilderBlock = pipelineBlock,
        extraResultAssertions = extraAssertions
    )

    if (!this.expectFailure) {
        harness.runEvaluatorTestCase(tc, session)
    } else {
        val message = "We expect test \"${this.name}\" to fail, but it did not. This check exists to ensure the " +
            "failing list is up to date."

        assertThrows<Throwable>(message) {
            harness.runEvaluatorTestCase(tc, session)
        }
    }
}
