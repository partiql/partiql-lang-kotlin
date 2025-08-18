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
import kotlin.test.Test
import kotlin.test.assertEquals

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

    private lateinit var context: Context
    private lateinit var listener: CustomPErrorListener

    @Test
    fun `PartiQLParser error handling`() {
        val parser = PartiQLParser.standard()

        // The PRuntimeException is expected to throw when .parse called on a PartiQL query with invalid syntax.
        val exception = assertThrows<PRuntimeException> {
            // An invalid single quote is added before Table name foo
            parser.parse("CREATE TABLE 'foo")
        }

        assertEquals(exception.error.code(), PError.UNEXPECTED_TOKEN)
        assertEquals(PErrorKind.SYNTAX, exception.error.kind.code())
        assertEquals(Severity.ERROR, exception.error.severity.code())
        assertEquals(SourceLocation(1, 14, 1), exception.error.location)
    }

    @Test
    fun `PartiQLParser with custom error handler`() {
        // Register Error Listener
        listener = CustomPErrorListener()
        context = Context.of(listener)

        val parser = PartiQLParser.standard()
        val parseResult: PartiQLParser.Result
        try {
            // An invalid single quote is added before Table name foo
            parseResult = parser.parse("CREATE TABLE 'mytable", context)
        } catch (ex: PRuntimeException) {
            // Since we register custom error handler to record all errors instead of throwing, these exceptions were likely unexpected.
            // However, the PartiQL library may throw PRuntimeExceptions as well. You should be able to unwrap these exceptions and provide quality
            // error messages to your users. In this case, we throw
            throw ex
        }

        // Process your listener now as the errors are not thrown in this example.
        val exception = listener.errorCollection[0]

        assertEquals(PError.UNEXPECTED_TOKEN, exception.code())
        assertEquals(PErrorKind.SYNTAX, exception.kind.code())
        assertEquals(Severity.ERROR, exception.severity.code())
        assertEquals(SourceLocation(1, 14, 1), exception.location)
    }

    @Test
    fun `PartiQLPlanner error handling`() {
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()

        val parseResult = parser.parse("CREATE TABLE mytable")
        val exception = assertThrows<PRuntimeException> {
            planner.plan(parseResult.statements[0], session)
        }

        assertEquals(PError.INTERNAL_ERROR, exception.error.code())
        assertEquals(PErrorKind.SEMANTIC, exception.error.kind.code())
        assertEquals(Severity.ERROR, exception.error.severity.code())
    }

    @Test
    fun `PartiQLPlanner with custom error handler`() {
        // Register Error Listener
        listener = CustomPErrorListener()
        context = Context.of(listener)

        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()

        val parseResult = parser.parse("CREATE TABLE mytable")

        try {
            planner.plan(parseResult.statements[0], session, context)
        } catch (ex: PRuntimeException) {
            throw ex
        }

        val exception = listener.errorCollection[0]

        assertEquals(PError.INTERNAL_ERROR, exception.code())
        assertEquals(PErrorKind.SEMANTIC, exception.kind.code())
        assertEquals(Severity.ERROR, exception.severity.code())
    }

    @Test
    fun `PartiQLCompiler error handling`() {
        val compiler = PartiQLCompiler.standard()
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()

        val parseResult = parser.parse("1 / 0")

        val session = Session.empty()
        val plan = planner.plan(parseResult.statements[0], session).plan
        val exception = assertThrows<PRuntimeException> {
            val result = compiler.prepare(plan, Mode.STRICT()).execute()
        }

        assertEquals(PError.DIVISION_BY_ZERO, exception.error.code())
        assertEquals(PErrorKind.EXECUTION, exception.error.kind.code())
        assertEquals(Severity.ERROR, exception.error.severity.code())
    }

    @Test
    fun `PartiQLCompiler with custom error handler`() {
        // Register Error Listener
        listener = CustomPErrorListener()
        context = Context.of(listener)

        val compiler = PartiQLCompiler.standard()
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()

        val parseResult = parser.parse("1 / 0")

        val session = Session.empty()
        val plan = planner.plan(parseResult.statements[0], session).plan
        try {
            val result = compiler.prepare(plan, Mode.STRICT(), context)
        } catch (ex: PRuntimeException) {
            throw ex
        }

        val exception = listener.errorCollection[0]

        assertEquals(PError.DIVISION_BY_ZERO, exception.code())
        assertEquals(PErrorKind.EXECUTION, exception.kind.code())
        assertEquals(Severity.ERROR, exception.severity.code())
    }


}
