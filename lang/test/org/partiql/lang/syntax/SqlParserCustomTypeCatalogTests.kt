package org.partiql.lang.syntax

import com.amazon.ion.IonSexp
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.toAstExpr
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.visitors.CustomTypeVisitorTransform
import org.partiql.lang.util.asIonSexp

/**
 * Parser tests covering the (de)serialization of the old ASTs to custom types
 * TODO: Remove these tests https://github.com/partiql/partiql-lang-kotlin/issues/510
 */
class SqlParserCustomTypeCatalogTests : SqlParserTestBase() {

    private val customTypeVisitorTransform = CustomTypeVisitorTransform()

    @ParameterizedTest
    @MethodSource("parametersForCatalogTests")
    fun catalogTests(tc: TestCase) = runTest(tc)

    data class TestCase(
        val name: String,
        val sql: String,
        val oldSerializedPigAst: String,
        val newSerializedPigAst: String
    )

    private fun deserialize(serializedSexp: String): ExprNode {
        val sexp = ion.singleValue(serializedSexp).asIonSexp()
        val astExpr = PartiqlAst.transform(sexp.toIonElement()) as PartiqlAst.Expr
        val astStatement = PartiqlAst.build {
            query(astExpr)
        }
        val transformedAstStatement = customTypeVisitorTransform.transformStatement(astStatement)
        return transformedAstStatement.toExprNode(ion)
    }

    private fun runTest(tc: TestCase) {
        // Deserialize old sexp
        val exprNodeFromOldSexp = deserialize(tc.oldSerializedPigAst)

        // Deserialize new sexp
        val newPigSexp = ion.singleValue(tc.newSerializedPigAst).asIonSexp()
        val exprNodeFromNewSexp = deserialize(tc.newSerializedPigAst)

        // Assert that both the expr nodes are same after they are serialized using the new PIG AST serialization
        val oldRoundTrippedAst = exprNodeFromOldSexp.toAstExpr().toIonElement().asAnyElement().toIonValue(ion) as IonSexp
        val newRoundTrippedAst = exprNodeFromNewSexp.toAstExpr().toIonElement().asAnyElement().toIonValue(ion) as IonSexp

        assertEquals("Old and new generated sexps do not match for \"${tc.name}\"", oldRoundTrippedAst, newRoundTrippedAst)

        // Assert that the provided new pig sexp is the same as the new generated pig sexp
        assertEquals("Provided new sexp and new generated sexp do not match for \"${tc.name}\"", newPigSexp, oldRoundTrippedAst)

        // Assert that the expr node generated from the query is the same as the old deserialized one
        val exprNodeFromQuery = parse(tc.sql).toExprNode(ion)
        val sexpFromQueryExprNode = exprNodeFromQuery.toAstExpr().toIonElement().asAnyElement().toIonValue(ion) as IonSexp

        assertEquals(
            "ExprNode generated from the query is not the same as the old deserialized one",
            sexpFromQueryExprNode,
            oldRoundTrippedAst
        )
    }

    companion object {
        private fun castToCustomTypeTests() = listOf(
            "es_boolean", "es_any", "es_integer", "es_float", "es_text",
            "spark_short", "spark_integer", "spark_long", "spark_double", "spark_boolean", "spark_float",
            "rs_varchar_max", "rs_integer", "rs_bigint", "rs_boolean", "rs_real", "rs_double_precision"
        ).map { customType ->
            TestCase(
                name = "cast to custom type $customType",
                sql = "CAST(a AS ${customType.toUpperCase()})",
                oldSerializedPigAst = "(cast (id a (case_insensitive) (unqualified)) ($customType))",
                newSerializedPigAst = "(cast (id a (case_insensitive) (unqualified)) (custom_type $customType))"
            )
        }

        @JvmStatic
        @Suppress("UNUSED")
        fun parametersForCatalogTests() = listOf(
            TestCase(
                name = "cast to custom type es_boolean",
                sql = "CAST(TRUE AS ES_BOOLEAN)",
                oldSerializedPigAst = "(cast (lit true) (es_boolean))",
                newSerializedPigAst = "(cast (lit true) (custom_type es_boolean))"
            ),
            TestCase(
                name = "select query",
                sql = """SELECT name, CAST(colour AS ES_TEXT) as colour, CAST(age AS ES_INTEGER) as years FROM SOURCE_VIEW_DELTA_FULL_TRANSACTIONS""",
                oldSerializedPigAst = """
                    (select 
                        (project (project_list 
                            (project_expr (id name (case_insensitive) (unqualified)) null) 
                            (project_expr (cast (id colour (case_insensitive) (unqualified)) (es_text)) colour) 
                            (project_expr (cast (id age (case_insensitive) (unqualified)) (es_integer)) years))) 
                                (from (scan (id SOURCE_VIEW_DELTA_FULL_TRANSACTIONS (case_insensitive) (unqualified)) null null null)))
                """.trimIndent(),
                newSerializedPigAst = """
                    (select 
                        (project (project_list 
                            (project_expr (id name (case_insensitive) (unqualified)) null) 
                            (project_expr (cast (id colour (case_insensitive) (unqualified)) (custom_type es_text)) colour) 
                            (project_expr (cast (id age (case_insensitive) (unqualified)) (custom_type es_integer)) years))) 
                                (from (scan (id SOURCE_VIEW_DELTA_FULL_TRANSACTIONS (case_insensitive) (unqualified)) null null null)))
                """.trimIndent()
            )
        ) + castToCustomTypeTests()
    }
}
