package examples

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.ast.Assignment
import org.partiql.lang.ast.AssignmentOp
import org.partiql.lang.ast.CaseSensitivity
import org.partiql.lang.ast.DataManipulation
import org.partiql.lang.ast.Literal
import org.partiql.lang.ast.VariableReference
import org.partiql.lang.ast.emptyMetaContainer
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.syntax.SourcePosition
import org.partiql.lang.syntax.SqlLexer
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.syntax.Token
import org.partiql.lang.syntax.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals

class BreakingChanges {
    @Test
    fun `api change - Token's SourcePosition changed with SourceSpan`() {
        val ion = IonSystemBuilder.standard().build()

        // SqlLexer provides us with `Token`s
        val tokens = SqlLexer(ion).tokenize("42")
        val firstToken = tokens.first()

        // `Token`s defined in v0.2.* and before used `SourcePosition` specifying the `line` and `column` of a `Token`
        val tokenFromConstructor = Token(
            type = TokenType.LITERAL,
            value = ion.singleValue("42"),
            position = SourcePosition(
                line = 1,
                column = 1
            )
        )
        assertEquals(tokenFromConstructor, firstToken)
    }

    @Test
    fun `api change - PARSE_EXEC_AT_UNEXPECTED_LOCATION removed from ErrorCode`() {
        val ion = IonSystemBuilder.standard().build()
        val parser = SqlParser(ion)

        // bad query containing a stored procedure call, `EXEC`, at an unexpected location
        val badQuery = "SELECT * FROM (EXEC undrop 'foo')"

        try {
            parser.parseExprNode(badQuery)
        } catch (pe: ParserException) {
            // the parser gives a `ParserException` with error code `PARSE_EXEC_AT_UNEXPECTED_LOCATION`, which is
            // removed starting in v0.3.* onwards
            assertEquals(ErrorCode.PARSE_EXEC_AT_UNEXPECTED_LOCATION, pe.errorCode)
        }
    }

    @Test
    fun `api change - refactored DML-related AST nodes`() {
        val ion = IonSystemBuilder.standard().build()
        // In v0.2.* and before,
        // - `DataManipulation` node can only specify a single `dmlOperation`
        // - `AssignmentOp`, a type of `DataManipulationOperation` contains a list of `Assignments`
        // The following AST represents the DML query (without source location metas): SET k = 5, l = 6
        DataManipulation(
            dmlOperation = AssignmentOp(
                assignments = listOf(
                    Assignment(
                        lvalue = VariableReference(
                            id = "k",
                            case = CaseSensitivity.INSENSITIVE,
                            metas = emptyMetaContainer
                        ),
                        rvalue = Literal(
                            ionValue = ion.singleValue("5"),
                            metas = emptyMetaContainer
                        )
                    ),
                    Assignment(
                        lvalue = VariableReference(
                            id = "l",
                            case = CaseSensitivity.INSENSITIVE,
                            metas = emptyMetaContainer
                        ),
                        rvalue = Literal(
                            ionValue = ion.singleValue("6"),
                            metas = emptyMetaContainer
                        )
                    )
                )
            ),
            metas = emptyMetaContainer
        )
    }
}
