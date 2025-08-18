package examples

import org.junit.jupiter.api.assertThrows
import org.partiql.lang.eval.EvaluationSession
import org.partiql.parser.PartiQLParser
import org.partiql.parser.PartiQLParserException
import org.partiql.parser.SourceLocation
import org.partiql.planner.PartiQLPlanner
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorHandlingChanges {

    @Test
    fun `Parser syntax error handling`() {
        val parser = PartiQLParser.default()

        val exception = assertThrows<PartiQLParserException> {
            parser.parse("SELECT * FRM abd")
        }

        assertTrue { exception.message.contains("mismatched input 'FRM'") }
        assertEquals(SourceLocation(1, 10, 52, 3), exception.location)
    }

    @Test
    fun `Planner function type mismatch error`() {
        val parser = PartiQLParser.default()
        val planner = PartiQLPlanner.default()
        val sess = PartiQLPlanner.Session(
            queryId = UUID.randomUUID().toString(),
            userId = "debug"
        )

        val parseResult = parser.parse("SELECT UPPER(123)")
        val exception = assertThrows<PartiQLParserException> {
            val result = planner.plan(parseResult.root, sess).plan
        }

        assertTrue { exception.message.contains("extraneous input '''") }
        assertEquals(SourceLocation(1, 14, 62, 1), exception.location)
    }

    @Test
    fun `Expression evaluation error`() {

        val parser = PartiQLParser.default()
        val planner = PartiQLPlanner.default()
        val sess = PartiQLPlanner.Session(
            queryId = UUID.randomUUID().toString(),
            userId = "debug"
        )

        val session = EvaluationSession.builder().build()

        val parseResult = parser.parse("SELECT UPPER(123)")
        val exception = assertThrows<PartiQLParserException> {
            val result = planner.plan(parseResult.root, sess).plan
            result
        }

        assertTrue { exception.message.contains("extraneous input '''") }
        assertEquals(SourceLocation(1, 14, 62, 1), exception.location)
    }
}
