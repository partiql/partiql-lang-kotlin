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
import org.partiql.lang.mockdb.MockDb
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.assertIonEquals

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
     * The expected result, specified in Ion.
     *
     * If null, no assertion on the expected result will be performed, however [extraAssertions] will
     * still be called (if it is not null).
     */
    val expectedIonResult: String?,

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
    val compileOptions: CompileOptions = CompileOptions.standard(),

    /** An optional block in which to execute additional assertions. */
    val extraAssertions: ((ExprValue, CompileOptions) -> Unit)? = null
) {
    private val cleanedSqlUnderTest =
        sqlUnderTest.replace("\n", "")

    override fun toString(): String = listOfNotNull(group, name, note, cleanedSqlUnderTest).joinToString(" - ")

    fun toExprNodeTestCase(): ExprNodeTestCase =
        assertDoesNotThrow("IonResultTestCase ${toString()} should not throw when parsing") {
            ExprNodeTestCase(name, SqlParser(ION).parseExprNode(sqlUnderTest))
        }

}

internal fun IonResultTestCase.runTestCase(
    valueFactory: ExprValueFactory,
    db: MockDb,
    compileOptionsBlock: (CompileOptions.Builder.() -> Unit)? = null,
    pipelineBlock: (CompilerPipeline.Builder.() -> Unit)? = null

) {
    fun runTheTest() {
        val parser = SqlParser(ION)

        val astStatement = assertDoesNotThrow("Parsing the sql under test should not throw for test \"${this.name}\"") {
            parser.parseAstStatement(sqlUnderTest)
        }

        val expectedResult = assertDoesNotThrow(
            "Parsing the expected ion result should not throw for test \"${this.name}\""
        ) {
            expectedIonResult?.let { ION.singleValue(it) }
        }

        val modifiedCompileOptions = when(compileOptionsBlock) {
            null -> compileOptions
            else -> CompileOptions.build { compileOptionsBlock() }
        }


        val pipeline = CompilerPipeline.build(ION) pipelineBlock@{
            compileOptions(modifiedCompileOptions)
            pipelineBlock?.invoke(this)
        }

        val expression = assertDoesNotThrow("Compiling the query should not throw for test \"${this.name}\"") {
            pipeline.compile(astStatement)
        }

        val session = EvaluationSession.build {
            globals(db.valueBindings)
            parameters(EVALUATOR_TEST_SUITE.createParameters(valueFactory))
        }

        val (exprValueResult, ionValueResult) = assertDoesNotThrow(
            "evaluating the expression should not throw for test \"${this.name}\""
        ) {
            val result = expression.eval(session)
            result to result.ionValue
        }

        expectedResult?.let { assertIonEquals(it, ionValueResult, "for test \"${this.name}\", ") }

        assertDoesNotThrow("extraAssertions should not throw for test \"${this.name}\"") {
            extraAssertions?.invoke(exprValueResult, compileOptions)
        }
    }

    when {
        !expectFailure -> runTheTest()
        else -> {
            val message = "We expect test \"${this.name}\" to fail, but it did not. This check exists to ensure the " +
                "failing list is up to date."

            assertThrows<Throwable>(message) {
                runTheTest()
            }
        }
    }
}