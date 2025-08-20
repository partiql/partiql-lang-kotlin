package examples

import org.junit.jupiter.api.assertThrows
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.Context
import org.partiql.spi.SourceLocation
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.types.PType
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorHandlingChanges {

    /**
     * The example shows the default behavior of error handling in major PartiQL components {@link PartiQLParser},
     * {@link PartiQLPlanner} and {@link PartiQLCompiler}. {@link PRuntimeException} will be thrown from the PartiQL system
     * to trap any parsing errors or warnings. {@link PRuntimeException} wraps a {@link PError} which includes {@link PError#code()},
     * {@link PError#kind}, {@link PError#severity}, {@link PError#location} and {@link PError#properties}.
     * {@link PRuntimeException} with some enhanced information allows us for more robust and customized error control.
     */
    @Test
    fun `Default PartiQL error handling`() {
        val compiler = PartiQLCompiler.standard()
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()

        val query = """
            CREATE TABLE 'mytable;
        """.trimIndent()

        val exception = assertThrows<PRuntimeException> {
            try {
                val parseResult = parser.parse(query)
                val plan = planner.plan(parseResult.statements[0], session).plan
                val result = compiler.prepare(plan, Mode.STRICT())
            } catch (e: PRuntimeException) {
                // The exception should have been thrown from PartiQL components. You can add your handling here.
                // Rethrow for validating the exception in test
                throw e
            }
        }

        assertEquals(exception.error.code(), PError.UNEXPECTED_TOKEN)
        assertEquals(PErrorKind.SYNTAX, exception.error.kind.code())
        assertEquals(Severity.ERROR, exception.error.severity.code())
        assertEquals(SourceLocation(1, 14, 1), exception.error.location)
        // Inspect properties from exception
        assertEquals("UNRECOGNIZED", exception.error.get("TOKEN_NAME", String::class.java))
    }

    /**
     * The example shows we registered a custom error handling in the context of major PartiQL components {@link PartiQLParser},
     * {@link PartiQLPlanner} and {@link PartiQLCompiler} which records the errors or warning instead of throwing for deferred error processing.
     * You can choose to treat warning as errors, or inhibit certain warnings or even ignore warnings.
     * It might be a good idea for you to consolidate your logic of converting PErrors to user-presented messages and use that here.
     */
    @Test
    fun `PartiQLParser with custom error handler`() {
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

        // Register Error Listener
        val listener = CustomPErrorListener()
        val context = Context.of(listener)

        val compiler = PartiQLCompiler.standard()
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()

        val query = """
            CREATE TABLE mytable;
            INSERT INTO 'mytable VALUES (2,3);
        """.trimIndent()

        // The example shows you how to implement custom error handling for PartiQL. In the example, we will record the
        // exception for delayed processing to override the default behavior.
        try {
            // Parser with custom context
            val parseResult = parser.parse(query, context)
            // Planner with custom context
            val plan = planner.plan(parseResult.statements[0], session, context).plan
            // Complier with custom context
            val result = compiler.prepare(plan, Mode.STRICT(), context)
        } catch (e: PRuntimeException) {
            // This will not throw as we register custom error handling, However, it is still good practice to catch this exception.
            // PartiQL system may still throw PRuntimeException, you may want to print user friendly message.
            throw e
        }

        // Process your listener now as the errors are not thrown in this example.
        assertEquals(2, listener.errorCollection.size)

        val exception1 = listener.errorCollection[0]

        assertEquals(PError.UNEXPECTED_TOKEN, exception1.code())
        assertEquals(PErrorKind.SYNTAX, exception1.kind.code())
        assertEquals(Severity.ERROR, exception1.severity.code())
        assertEquals(SourceLocation(2, 13, 1), exception1.location)
        // Inspect properties from exception
        assertEquals("UNRECOGNIZED", exception1.get("TOKEN_NAME", String::class.java))

        val exception2 = listener.errorCollection[1]

        assertEquals(PError.INTERNAL_ERROR, exception2.code())
        assertEquals(PErrorKind.SEMANTIC, exception2.kind.code())
        assertEquals(Severity.ERROR, exception2.severity.code())
        assertEquals("CREATE TABLE has not been supported yet in AstRewriter", exception2.get("CAUSE", Throwable::class.java)!!.message)
    }

    /**
     *  Error listeners are specifically meant to provide control over the reporting of errors for PartiQL’s
     *  major components (parser, planner, and compiler). However, for the execution of compiled statements,
     *  Instead, it immediately throws PRuntimeException.
     */
    @Test
    fun `PartiQL statement execution error handling`() {
        val compiler = PartiQLCompiler.standard()
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()

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
