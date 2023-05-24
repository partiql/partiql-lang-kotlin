package examples

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.emptyMetaContainer
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.planner.PlannerPassResult
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.parser.antlr.PartiQLTokens
import org.partiql.lang.types.CustomType
import java.nio.charset.StandardCharsets
import kotlin.test.Test

class BreakingChanges {

    /**
     * Introduced: [PartiqlAst.BagOpType] and [PartiqlAst.Expr.BagOp]
     */
    @Test
    fun `Bag Operators (Replacement)`() {
        fun `Added Bag Operator Types`(bagOp: PartiqlAst.BagOpType): Nothing = when (bagOp) {
            is PartiqlAst.BagOpType.Except -> TODO("EXAMPLE REPLACEMENT")
            is PartiqlAst.BagOpType.OuterExcept -> TODO("EXAMPLE REPLACEMENT")
            is PartiqlAst.BagOpType.Intersect -> TODO("EXAMPLE REPLACEMENT")
            is PartiqlAst.BagOpType.OuterIntersect -> TODO("EXAMPLE REPLACEMENT")
            is PartiqlAst.BagOpType.Union -> TODO("EXAMPLE REPLACEMENT")
            is PartiqlAst.BagOpType.OuterUnion -> TODO("EXAMPLE REPLACEMENT")
            else -> TODO("")
        }

        fun `New Usage`(): PartiqlAst.Expr.BagOp = PartiqlAst.build {
            val operation: PartiqlAst.BagOpType = intersect()
            val setQuantifier: PartiqlAst.SetQuantifier = all()
            val leftBag: PartiqlAst.Expr = bag()
            val rightBag: PartiqlAst.Expr = bag()
            bagOp(
                op = operation,
                quantifier = setQuantifier,
                operands = listOf(leftBag, rightBag),
                metas = emptyMetaContainer()
            )
        }
    }

    /**
     * Added: [PlannerPassResult]
     * - PassResult gets renamed to [PlannerPassResult]. This is part of the experimental planner API.
     */
    @Test
    fun `PassResult (Renamed)`() {
        fun `Example Usage`(): PlannerPassResult<ExprValue> {
            val ion: IonSystem = IonSystemBuilder.standard().build()
            val valueFactory: ExprValueFactory = ExprValueFactory.standard(ion)
            val exprValue: ExprValue = valueFactory.newInt(1)
            return PlannerPassResult.Success(
                exprValue,
                emptyList()
            )
        }
    }

    /**
     * Changed: Parsing of ORDER BY
     * Added: [PartiQLParserBuilder]
     */
    @Test
    fun `ORDER BY Parsing (Changed) and Parser (Introduced)`() {

        /**
         * The v0.7.* implementation automatically adds a default [PartiqlAst.SortSpec] and a default [PartiqlAst.NullsSpec]
         * to the parsed [PartiqlAst.OrderBy]. This is removed in v0.8.0. The v0.8.0 keeps them null.
         */
        fun `Example Usage`() {
            val ion: IonSystem = IonSystemBuilder.standard().build()
            val customTypes: List<CustomType> = emptyList()
            val parser: Parser = PartiQLParserBuilder().ionSystem(ion).customTypes(customTypes).build()
            val query = "SELECT * FROM t ORDER BY t.a"
            val ast: PartiqlAst.Statement.Query = parser.parseAstStatement(query) as PartiqlAst.Statement.Query
            val exprSelect: PartiqlAst.Expr.Select = ast.expr as PartiqlAst.Expr.Select
            assert(exprSelect.order?.sortSpecs?.get(0)?.orderingSpec == null)
            assert(exprSelect.order?.sortSpecs?.get(0)?.nullsSpec == null)
        }
    }

    /**
     * While these APIs are not supported by PartiQL -- they are APIs exposed by ANTLR.
     */
    @Test
    fun `SqlLexer (Deprecated)`() {
        fun `Example Usage`(): List<Token> {
            // Create Lexer (not officially supported by PartiQL)
            val query: String = "SELECT * FROM t"
            val inputStream: CharStream = CharStreams.fromStream(
                query.byteInputStream(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
            )
            val lexer: org.antlr.v4.runtime.Lexer = PartiQLTokens(inputStream)
            lexer.removeErrorListeners()

            // Get Token Stream & "Fill" Tokens
            val tokenStream: CommonTokenStream = CommonTokenStream(lexer).also { stream -> stream.fill() }
            val tokens: List<Token> = tokenStream.tokens

            return tokens
        }
    }
}
