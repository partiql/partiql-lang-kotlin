package examples

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.Context
import org.partiql.spi.SourceLocation
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.types.PType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A unified error handling is introduced PartiQL system. [PRuntimeException] will be thrown from all components in PartiQL system
 * to trap any errors or warnings. [PRuntimeException] warps a [PError] Java class which exposes information for users to write
 * high quality error messages.
 *
 * Also [PErrorListener] is introduced to allow customized error handling. You can register a [PErrorListener] in context of
 * major PartiQL components [PartiQLParser], [PartiQLPlanner] and [PartiQLCompiler]. It allows you to inspect each PError
 * the component emitted and decide action on error. You may treat all warnings as errors, suppress all warnings, delay the error handling
 * or throw custom exception with an error message.
 *
 * Note for the output when you choose to delay the error handling. The component will not stop and still return an output object
 * In case the severity is [Severity.ERROR], the output may be incorrect or incomplete. You should discard the result.
 * You may proceed with [Severity.WARNING], but be aware of the warning condition such as date accuracy loss.
 */
class ErrorHandlingChanges {
    class CustomPErrorListener : PErrorListener {
        val errorCollection: MutableList<PError> = mutableListOf()

        override fun report(error: PError) {
            when (error.severity.code()) {
                // You may choose to customize the error message and then halt the flow by throwing the error, or you can record the error and then throw when it reaches maximum count
                // In this case, we record only without throwing
                Severity.ERROR -> errorCollection.add(error)
                // You may choose to customize the error message, treat warning as an error and then halt the flow by throwing, or you can suppress all warnings.
                // In this case, we record only without throwing
                Severity.WARNING -> errorCollection.add(error)
                else -> error("This shouldn't have occurred.")
            }
        }
    }

    /**
     * The example shows the error handling when using [PartiQLParser] to parse a string statement,
     * When an error occurs, the parser will stop the parsing process by throwing the [PRuntimeException].
     */
    @Test
    fun `PartiQLParser default error handling`() {
        val parser = PartiQLParser.standard()

        // The PRuntimeException is expected to throw when .parse called on a PartiQL query with invalid syntax.
        val exception = assertThrows<PRuntimeException> {
            try {
                // Extra single quote is added
                parser.parse("SELECT * FROM 'mytable;")
            } catch (e: PRuntimeException) {
                // Rethrow for validating the exception in test
                throw e
            }
        }

        assertEquals(exception.error.code(), PError.UNEXPECTED_TOKEN)
        assertEquals(PErrorKind.SYNTAX, exception.error.kind.code())
        assertEquals(Severity.ERROR, exception.error.severity.code())
        assertEquals(SourceLocation(1, 15, 1), exception.error.location)
        // Inspect properties from exception
        assertEquals("UNRECOGNIZED", exception.error.get("TOKEN_NAME", String::class.java))
    }

    /**
     * The example shows the error handling with custom [PErrorListener], which delays the error handling
     * when using [PartiQLParser] to parse multiple string statements.
     */
    @Test
    fun `PartiQLParser error handling with custom listener`() {
        // Register Error Listener
        val listener = CustomPErrorListener()
        val context = Context.of(listener)

        val parser = PartiQLParser.standard()

        // 1. Good statement. This one should be parsed.
        // 2. Extra single quote. The parsing should stop here.
        // 3. Incorrect operator. The parsing will not reach here to report error.
        val query = """
            SELECT * FROM mytable1;
            SELECT * FROM 'mytable2;
            SELECT 1 ++ 2 FROM mytable2;
        """.trimIndent()

        lateinit var result: PartiQLParser.Result

        assertDoesNotThrow {
            try {
                // Invalid single quote is added before `mytable`
                result = parser.parse(query, context)
            } catch (e: PRuntimeException) {
                // It is unlikely reach here due to parsing if you use custom listener to suppress or delay error handling.
                // However, PartiQL system still may throw the exception. You should be able to unwrap these exceptions and provide quality
                // error messages to your users.
                // So in this case, rethrowing should not fail the test as exception is trapped in listener.
                throw e
            }
        }

        // In this case, the first statement should be parsed and then the parsing process may not move forward.
        // However, the exception is not thrown immediately. However, the result is still considered as incomplete.
        assertEquals(1, result.statements.size)

        val exception = listener.errorCollection[0]
        assertEquals(exception.code(), PError.UNEXPECTED_TOKEN)
        assertEquals(PErrorKind.SYNTAX, exception.kind.code())
        assertEquals(Severity.ERROR, exception.severity.code())
        assertEquals(SourceLocation(2, 15, 1), exception.location)
        // Inspect properties from exception
        assertEquals("UNRECOGNIZED", exception.get("TOKEN_NAME", String::class.java))
    }

    /**
     * This example shows the error handling behavior that the [PartiQLPlanner] can proceed with most errors
     * or warnings. However, it may still throw [PRuntimeException] in some circumstances
     */
    @Test
    fun `PartiQLPlanner default error handling which throws exception`() {
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()

        // CREATE TABLE is not supported by planner currently
        val parseResult = parser.parse("CREATE TABLE mytable")
        val exception = assertThrows<PRuntimeException> {
            try {
                // Create are not supported in current Planner
                val plan = planner.plan(parseResult.statements[0], session).plan
            } catch (e: PRuntimeException) {
                // Rethrow for validating the exception in test
                throw e
            }
        }

        assertEquals(PError.INTERNAL_ERROR, exception.error.code())
        assertEquals(PErrorKind.SEMANTIC, exception.error.kind.code())
        assertEquals(Severity.ERROR, exception.error.severity.code())
    }

    /**
     * This example shows the error handling behavior that the [PartiQLPlanner] can proceed with most errors
     * or warnings and will not throw.
     */
    @Test
    fun `PartiQLPlanner default error handling which does not throw`() {
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()

        // Invalid expression adding number and string
        val parseResult = parser.parse("SELECT 1 + 'string' FROM mytable")
        assertDoesNotThrow {
            val plan = planner.plan(parseResult.statements[0], session).plan
        }
    }

    /**
     * This example shows the error handling behavior that the [PartiQLPlanner] can proceed with most errors
     * or warnings and will not throw. But we use error listener to trap errors or warnings from the planner.
     */
    @Test
    fun `PartiQLPlanner error handling with custom listener`() {
        // Register Error Listener
        val listener = CustomPErrorListener()
        val context = Context.of(listener)

        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()

        // Invalid expression adding number and string
        val parseResult = parser.parse("SELECT 1 + 'string' FROM mytable")
        assertDoesNotThrow {
            val plan = planner.plan(parseResult.statements[0], session, context).plan
        }
        assertEquals(2, listener.errorCollection.size)

        val exception1 = listener.errorCollection[0]
        assertEquals(PError.VAR_REF_NOT_FOUND, exception1.code())
        assertEquals(PErrorKind.SEMANTIC, exception1.kind.code())
        assertEquals(Severity.WARNING, exception1.severity.code())
        assertEquals("mytable", exception1.get("ID", Identifier::class.java).toString())

        val exception2 = listener.errorCollection[1]
        assertEquals(PError.FUNCTION_TYPE_MISMATCH, exception2.code())
        assertEquals(PErrorKind.SEMANTIC, exception2.kind.code())
        assertEquals(Severity.WARNING, exception2.severity.code())
        assertEquals("\"\uFDEFplus\"", exception2.get("FN_ID", Identifier::class.java).toString())
    }

    /**
     *  For the execution of compiled statements, the PartiQL system will immediately throw [PRuntimeException].
     */
    @Test
    fun `PartiQL evaluation phase default error handling`() {
        val compiler = PartiQLCompiler.standard()
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()

        // Divide by zero
        val parseResult = parser.parse("1 / 0")
        val plan = planner.plan(parseResult.statements[0], session).plan
        val statement = compiler.prepare(plan, Mode.STRICT())
        val exception = assertThrows<PRuntimeException> {
            try {
                statement.execute()
            } catch (e: PRuntimeException) {
                // A DIVISION_BY_ZERO will throw during the execution
                // Rethrow for validating the exception in test
                throw e
            }
        }

        assertEquals(exception.error.code(), PError.DIVISION_BY_ZERO)
        assertEquals(PErrorKind.EXECUTION, exception.error.kind.code())
        assertEquals(Severity.ERROR, exception.error.severity.code())
        assertEquals("1", exception.error.get("DIVIDEND", String::class.java))
        assertEquals(PType.integer(), exception.error.get("DIVIDEND_TYPE", PType::class.java))
    }
}
