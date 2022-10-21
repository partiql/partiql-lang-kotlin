package examples

import com.amazon.ion.IonInt
import com.amazon.ion.IonSexp
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ast.Bag
import org.partiql.lang.ast.CaseSensitivity
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.FromSourceExpr
import org.partiql.lang.ast.FromSourceUnpivot
import org.partiql.lang.ast.ListExprNode
import org.partiql.lang.ast.Literal
import org.partiql.lang.ast.SymbolicName
import org.partiql.lang.ast.VariableReference
import org.partiql.lang.ast.metaContainerOf
import org.partiql.lang.ast.passes.V0AstSerializer
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.filterMetaNodes
import kotlin.test.Test
import kotlin.test.assertEquals

class BreakingChanges {
    @Test
    fun `behavioral change - JOIN requires ON clause`() {
        // Initialization of components related to evaluating a query end-to-end
        val ion = IonSystemBuilder.standard().build()
        val parser = SqlParser(ion)
        val pipeline = CompilerPipeline.standard(ion)
        val evaluationSession = EvaluationSession.standard()

        // Query with a JOIN without an ON clause. In v0.1.*, the ON condition is optional.
        val query = "SELECT * FROM <<{'a': 1}>> INNER JOIN <<{'a': 2}>>"
        val parsedQuery = parser.parseExprNode(query)
        val compiledQuery = pipeline.compile(parsedQuery)
        val result = compiledQuery.eval(evaluationSession)

        // Query successfully evaluates. Here we compare the result to its string representation.
        assertEquals("<<{'a': 1, 'a': 2}>>", result.toString())

        // Query with an ON clause successfully parses and can be evaluated
        val resultWithOn = pipeline.compile("SELECT * FROM <<{'a': 1}>> INNER JOIN <<{'a': 2}>> ON true").eval(evaluationSession)
        assertEquals(result.toString(), resultWithOn.toString())
    }

    @Test
    fun `api change - different modeling of FROM source in AST`() {
        // FROM source and FROM source UNPIVOT are modeled differently between v0.1.* and v0.2.*
        // The following is an AstNode/ExprNode representation of '... FROM foo AS f AT g'
        val fromExpr = VariableReference(id = "foo", case = CaseSensitivity.INSENSITIVE, metas = metaContainerOf())
        FromSourceExpr(
            expr = fromExpr,
            asName = SymbolicName(name = "f", metas = metaContainerOf()),
            atName = SymbolicName(name = "g", metas = metaContainerOf())
        )

        // The following models '... FROM UNPIVOT foo AS f AT g'
        FromSourceUnpivot(
            expr = fromExpr,
            asName = SymbolicName(name = "f", metas = metaContainerOf()),
            atName = SymbolicName(name = "g", metas = metaContainerOf()),
            metas = metaContainerOf()
        )
    }

    @Test
    fun `api change - different modeling of LIST and BAG nodes in AST`() {
        val ion = IonSystemBuilder.standard().build()
        val elem1 = Literal(ionValue = ion.singleValue("1"), metas = metaContainerOf())
        val elem2 = Literal(ionValue = ion.singleValue("2"), metas = metaContainerOf())
        val elem3 = Literal(ionValue = ion.singleValue("3"), metas = metaContainerOf())

        // LIST and BAG are modeled differently between v0.1.* and v0.2.*
        // The following is an AstNode/ExprNode representation of [1, 2, 3]
        ListExprNode(
            values = listOf(
                elem1,
                elem2,
                elem3
            ),
            metas = metaContainerOf()
        )

        // The following is an AstNode/ExprNode representation of <<1, 2, 3>>
        Bag(
            bag = listOf(
                elem1,
                elem2,
                elem3
            ),
            metas = metaContainerOf()
        )
    }

    @Test
    fun `deprecated api - rewriting ASTs`() {
        val ion = IonSystemBuilder.standard().build()
        // v0.1.* parsed the PartiQL statement to an ExprNode that could be serialized to the V0Ast. ExprNode and V0Ast
        // have been deprecated in favor of PartiqlAst.
        val node: ExprNode = SqlParser(ion).parseExprNode("SELECT * FROM <<{'a': 1, 'b': {'c': 23}}>>")
        val ionSexp: IonSexp = V0AstSerializer.serialize(node, ion)

        // below is a way to recursively rewrite a V0Ast/IonSexp statement. This particular example function rewrites
        // any encountered int literal to 42.
        fun rewriteIntsTo42(sexp: IonSexp): IonSexp {
            val rewritten = sexp.map { child ->
                when (child) {
                    is IonSexp -> rewriteIntsTo42(child)
                    is IonInt -> ion.newInt(42)
                    else -> child.clone()
                }
            }
            return ion.newSexp(*rewritten.toTypedArray())
        }

        val rewrittenIonSexp = rewriteIntsTo42(ionSexp)
        // the rewritten statement will be equivalent to the following statement (excluding source location metas)
        val expectedIonSexp = SqlParser(ion).parse("SELECT * FROM <<{'a': 42, 'b': {'c': 42}}>>")
        assertEquals(expectedIonSexp.filterMetaNodes(), rewrittenIonSexp.filterMetaNodes())
    }
}
