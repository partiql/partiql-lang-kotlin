package org.partiql.lang.util.testdsl

import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.partiql.lang.ION
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EVALUATOR_TEST_SUITE
import org.partiql.lang.eval.EvaluatingCompiler
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.builtins.createBuiltinFunctions
import org.partiql.lang.mockdb.MockDb
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.assertIonEquals
import org.partiql.planner.GlobalBindings
import org.partiql.planner.PlanningResult
import org.partiql.planner.ResolutionResult
import org.partiql.planner.createQueryPlanner

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
    compileOptionsBlock: (CompileOptions.Builder.() -> Unit)? = null
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

        val globalBindings = GlobalBindings { bindingName ->
            val result = db.globals.entries.firstOrNull { bindingName.isEquivalentTo(it.key) }
            if(result != null) {
                // Note that the unique id is set to result.key (which is the actual name of the variable)
                // which *might* have different letter case than the [bindingName].
                ResolutionResult.GlobalVariable(ionSymbol(result.key))
            } else {
                ResolutionResult.Undefined
            }
        }

        val plannerResult = assertDoesNotThrow("Planning the query should not throw for test \"${this.name}\"") {
            val qp = createQueryPlanner(ION, globalBindings)
            qp.plan(astStatement)
        }

        // TODO:  should we be doing any assertions on the planner warnings? (currently we don't issue any)

        val plannedQuery = when(plannerResult) {
            is PlanningResult.Success -> plannerResult.physicalPlan
            is PlanningResult.Error -> fail("Failed to plan query for tests \"${this.name}\"")
        }

        val modifiedCompileOptions = when(compileOptionsBlock) {
            null -> compileOptions
            else -> CompileOptions.build { compileOptionsBlock() }
        }

        val expression = assertDoesNotThrow("Compiling the query should not throw for test \"${this.name}\"") {
            EvaluatingCompiler(
                valueFactory = valueFactory,
                functions = createBuiltinFunctions(valueFactory).associateBy { it.signature.name },
                customTypedOpParameters = emptyMap(),
                procedures = emptyMap(),
                compileOptions = modifiedCompileOptions
            ).compile(plannedQuery)
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
