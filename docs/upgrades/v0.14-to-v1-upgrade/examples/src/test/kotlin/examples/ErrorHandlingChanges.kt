package examples

import org.junit.jupiter.api.assertThrows
import org.partiql.errors.ErrorCode
import org.partiql.errors.Problem
import org.partiql.errors.ProblemSeverity
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluationSession
import org.partiql.parser.PartiQLParser
import org.partiql.parser.PartiQLParserException
import org.partiql.parser.SourceLocation
import org.partiql.planner.PartiQLPlanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorHandlingChanges {
    /**
     * The example shows error handling when using [PartiQLParser] to parse a string statement,
     * When an error occurs, the parser will stop the parsing process by throwing [PartiQLParserException].
     */
    @Test
    fun `PartiQLParser default error handling`() {
        val parser = PartiQLParser.default()

        val exception = assertThrows<PartiQLParserException> {
            try {
                // Extra single quote is added
                val parseResult = parser.parse("SELECT * FROM 'mytable")
            } catch (e: PartiQLParserException) {
                // Rethrow for validating the exception in the test
                throw e
            }
        }

        // This is a pain point when using the v0.14.9 parser. There's no proper way to extract error information
        // other than looking at the error message string.
        assertTrue { exception.message.contains("extraneous input") }
        assertEquals("UNRECOGNIZED", exception.tokenType)
        assertEquals(SourceLocation(1, 15, 666, 1), exception.location)
    }

    /**
     *  [PartiQLPlanner] transforms an Abstract Syntax Tree (AST) from the parser into a logical query plan.
     *  The planner does not throw for most errors. If errors occur, the planner will collect multiple errors during the
     * planning phase. The planner provides an optional callback for you to handle those errors.
     *
     * However, the planner may still throw exception in certain circumstances.
     * This example shows the error handling behavior when making a plan from a valid AST using [PartiQLPlanner]
     */
    @Test
    fun `PartiQLPlanner default error handling`() {
        val parser = PartiQLParser.default()
        val planner = PartiQLPlanner.default()
        val session = PartiQLPlanner.Session(
            queryId = "testId",
            userId = "test-user",
            currentCatalog = "default",
            catalogs = mapOf()
        )

        // The planner does not support CREATE TABLE currently
        val parseResult = parser.parse("CREATE TABLE mytable")
        val exception = assertThrows<Exception> {
            try {
                val planResult = planner.plan(parseResult.root, session)
            } catch (e: Exception) {
                // Rethrow for validating the exception in the test
                throw e
            }
        }

        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception.message!!.contains("Unsupported statement"))
    }

    /**
     * This example shows the planner will not throw for most of the errors and an optional problem callback can be specified to
     * handle errors from the planner.
     */
    @Test
    fun `PartiQLPlanner default error handling with problem callback`() {
        val parser = PartiQLParser.default()
        val planner = PartiQLPlanner.default()
        val session = PartiQLPlanner.Session(
            queryId = "testId",
            userId = "test-user",
            currentCatalog = "default",
            catalogs = mapOf()
        )

        val problemList = mutableListOf<Problem>()

        fun callback(p: Problem) {
            problemList.add(p)
        }

        val parseResult = parser.parse("UPPER(123)")
        val planResult = planner.plan(parseResult.root, session, ::callback)

        assertEquals(1, problemList.size)
        assertEquals(ProblemSeverity.ERROR, problemList[0].details.severity)
        assertEquals("Unknown function `upper(<int4>)", problemList[0].details.message)
    }

    /**
     * This example shows the error handling behavior of the PartiQL evaluation phase.
     */
    @Test
    fun `PartiQL evaluation phase default error handling`() {
        val pipeline = CompilerPipeline.standard()
        val session = EvaluationSession.build { }

        // Divide by zero error
        val expression = pipeline.compile("1 / 0")

        val exception = assertThrows<EvaluationException> {
            try {
                val result = expression.evaluate(session)
            } catch (e: EvaluationException) {
                // Rethrow for validating the exception in test
                throw e
            }
        }

        assertEquals(ErrorCode.EVALUATOR_DIVIDE_BY_ZERO, exception.errorCode)
        assertEquals("/ by zero", exception.message)
    }
}
