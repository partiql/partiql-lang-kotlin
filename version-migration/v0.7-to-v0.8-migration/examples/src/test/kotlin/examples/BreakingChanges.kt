package examples

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.planner.PassResult
import org.partiql.lang.syntax.Lexer
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.SqlLexer
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.types.CustomType
import kotlin.test.Test

class BreakingChanges {

    /**
     * Replaced: [PartiqlAst.Expr.Except], [PartiqlAst.Expr.Intersect], and [PartiqlAst.Expr.Union]
     */
    @Test
    fun `Bag Operators (Replaced)`() {
        fun `Bag Operators Listed`(bagOp: PartiqlAst.Expr): Nothing = when (bagOp) {
            is PartiqlAst.Expr.Except -> TODO("TO BE REPLACED")
            is PartiqlAst.Expr.Intersect -> TODO("TO BE REPLACED")
            is PartiqlAst.Expr.Union -> TODO("TO BE REPLACED")
            else -> TODO("")
        }

        fun `Example Usage`(): PartiqlAst.Expr.Intersect = PartiqlAst.build {
            val setQuantifier: PartiqlAst.SetQuantifier = all()
            val leftBag: PartiqlAst.Expr = bag()
            val rightBag: PartiqlAst.Expr = bag()
            intersect(
                setq = setQuantifier,
                operands = listOf(leftBag, rightBag),
                metas = emptyMetaContainer()
            )
        }
    }

    /**
     * Added: [PassResult]
     */
    @Test
    fun `PassResult (Renamed)`() {
        fun `Example Usage`(): PassResult<ExprValue> {
            val ion: IonSystem = IonSystemBuilder.standard().build()
            val valueFactory: ExprValueFactory = ExprValueFactory.standard(ion)
            val exprValue: ExprValue = valueFactory.newInt(1)
            return PassResult.Success(
                exprValue,
                emptyList()
            )
        }
    }

    /**
     * Removed: [ExprValueType.typeNames]
     */
    @Test
    fun `ExprValueType - TypeNames (Removed)`() {
        fun `Example Usage`() {
            val nullTypeNames: List<String> = ExprValueType.NULL.typeNames
            val intTypeNames: List<String> = ExprValueType.INT.typeNames
        }
    }

    /**
     * Changed: Parsing of ORDER BY
     * Deprecated: [SqlParser]
     */
    @Test
    fun `ORDER BY Parsing (Changed) and Parser (Deprecated)`() {

        /**
         * The v0.7.* implementation automatically adds a default [PartiqlAst.SortSpec] and a default [PartiqlAst.NullsSpec]
         * to the parsed [PartiqlAst.OrderBy]. This is removed in v0.8.0.
         */
        fun `Example Usage`() {
            val ion: IonSystem = IonSystemBuilder.standard().build()
            val customTypes: List<CustomType> = emptyList()
            val parser: Parser = SqlParser(ion, customTypes)
            val query = "SELECT * FROM t ORDER BY t.a"
            val ast: PartiqlAst.Statement.Query = parser.parseAstStatement(query) as PartiqlAst.Statement.Query
            val exprSelect: PartiqlAst.Expr.Select = ast.expr as PartiqlAst.Expr.Select
            assert(exprSelect.order?.sortSpecs?.get(0)?.orderingSpec != null)
            assert(exprSelect.order?.sortSpecs?.get(0)?.nullsSpec != null)
        }
    }

    /**
     * Deprecated: [SqlLexer]
     *  - Not to be replaced. Unsupported generated files using ANTLR may be used (not as a 1-for-1 replacement) to
     *    mimic the same functionality -- but PartiQL will not be supporting this use-case. See the `migrated-examples`
     *    for more information.
     */
    @Test
    fun `SqlLexer (Deprecated)`() {
        fun `Example Usage`() {
            val ion: IonSystem = IonSystemBuilder.standard().build()
            val lexer: Lexer = SqlLexer(ion)
            lexer.tokenize("SELECT * FROM t")
        }
    }

    /**
     * Deprecated: [TypedOpBehavior.LEGACY]
     */
    @Test
    fun `TypedOpParameter LEGACY (Deprecated)`() {
        fun `Example Usage`() {
            val deprecated: TypedOpBehavior = TypedOpBehavior.LEGACY
            val supported: TypedOpBehavior = TypedOpBehavior.HONOR_PARAMETERS
        }
    }
}
