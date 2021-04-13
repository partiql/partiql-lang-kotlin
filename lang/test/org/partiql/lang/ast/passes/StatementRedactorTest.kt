package org.partiql.lang.ast.passes

import com.amazon.ionelement.api.StringElement
import junitparams.Parameters
import org.junit.Test
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.SqlParserTestBase

class StatementRedactorTest : SqlParserTestBase() {
    private val testSafeFieldNames = setOf("hk", "rk")

    data class RedactionTestCase(val originalStatement: String, val redactedStatement: String)

    /**
     * Here is an example of user defined functions (UDF) with following signature:
     *  begins_with(Path: ExprNode, Value: Literal)
     *  contains(Path: ExprNode, Value: Literal)
     *
     * Callers are responsible for determining which [args] are to be redacted.
     * There are two major components:
     *     1. Which arguments are needed for [SafeFieldName] validation
     *     2. Which arguments are returned for redaction
     */
    private fun validateFuncContainsAndBeginsWith(args : List<PartiqlAst.Expr> ) : List<PartiqlAst.Expr> {
        val path = args[0]
        val value = args[1]
        val argsToRedact = mutableListOf<PartiqlAst.Expr>()

        if (path !is PartiqlAst.Expr.Id && path !is PartiqlAst.Expr.Path) {
            throw IllegalArgumentException("Unexpected type of argument path")
        }
        if (value !is PartiqlAst.Expr.Lit || value.value !is StringElement) {
            throw IllegalArgumentException("Unexpected type of argument value")
        }
        if (!skipRedaction(path, testSafeFieldNames)) {
            argsToRedact.add(value)
        }

        return argsToRedact
    }

    /**
     * Return true if the parsed results of input [statement] is the same as input [ast]
     */
    private fun validateInputAstParsedFromInputStatement(statement: String, ast: ExprNode): Boolean {
        return parser.parseExprNode(statement) == ast
    }

    @Test
    @Parameters
    fun testRedactOnPositiveCases(tc: RedactionTestCase) {
        val testConfig = mapOf("contains" to ::validateFuncContainsAndBeginsWith, "begins_with" to ::validateFuncContainsAndBeginsWith)
        val redactedStatement = redact(tc.originalStatement, testSafeFieldNames, testConfig)

        assertEquals(tc.redactedStatement, redactedStatement)
    }

    fun parametersForTestRedactOnPositiveCases() = listOf(
            // Typed
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk IS MISSING AND attr IS MISSING",
                    "SELECT * FROM tb WHERE hk IS MISSING AND attr IS ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk IS NOT MISSING AND attr IS NOT MISSING",
                    "SELECT * FROM tb WHERE hk IS NOT MISSING AND attr IS NOT ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS MISSING",
                    "SELECT * FROM tb WHERE attr IS ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS NUMERIC",
                    "SELECT * FROM tb WHERE attr IS ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS NOT NUMERIC",
                    "SELECT * FROM tb WHERE attr IS NOT ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS STRING",
                    "SELECT * FROM tb WHERE attr IS ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS NOT STRING",
                    "SELECT * FROM tb WHERE attr IS NOT ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS TUPLE",
                    "SELECT * FROM tb WHERE attr IS ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS NOT TUPLE",
                    "SELECT * FROM tb WHERE attr IS NOT ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS STRUCT",
                    "SELECT * FROM tb WHERE attr IS ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS NOT STRUCT",
                    "SELECT * FROM tb WHERE attr IS NOT ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS BOOLEAN",
                    "SELECT * FROM tb WHERE attr IS ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS NOT BOOLEAN",
                    "SELECT * FROM tb WHERE attr IS NOT ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS LIST",
                    "SELECT * FROM tb WHERE attr IS ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS NOT LIST",
                    "SELECT * FROM tb WHERE attr IS NOT ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS BLOB",
                    "SELECT * FROM tb WHERE attr IS ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS NOT BLOB",
                    "SELECT * FROM tb WHERE attr IS NOT ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS NULL",
                    "SELECT * FROM tb WHERE attr IS ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE attr IS NOT NULL",
                    "SELECT * FROM tb WHERE attr IS NOT ***(Redacted)"),
            // Call and Nested Call
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 1 AND begins_with(Attr, 'foo') AND begins_with(hk, 'foo')",
                    "SELECT * FROM tb WHERE hk = 1 AND begins_with(Attr, ***(Redacted)) AND begins_with(hk, 'foo')"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 1 AND contains(Attr, 'foo') AND contains(hk, 'foo')",
                    "SELECT * FROM tb WHERE hk = 1 AND contains(Attr, ***(Redacted)) AND contains(hk, 'foo')"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 1 AND arbitrary_udf(Attr, 'foo', arbitrary_udf(hk, 'foo'))",
                    "SELECT * FROM tb WHERE hk = 1 AND arbitrary_udf(Attr, ***(Redacted), arbitrary_udf(hk, ***(Redacted)))"),
            // Unary operator
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk IS MISSING AND attr = - 'literal'",
                    "SELECT * FROM tb WHERE hk IS MISSING AND attr = - ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk IS MISSING AND attr = -+ -+ -+ non_literal",
                    "SELECT * FROM tb WHERE hk IS MISSING AND attr = -+ -+ -+ non_literal"),
            // Arithmetic operators are not redacted
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 012-34-5678 AND ssn = 012-34-5678",
                    "SELECT * FROM tb WHERE hk = 012-34-5678 AND ssn = ***(Redacted)-***(Redacted)-***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 012-34-5678 AND ssn = 012*34*5678",
                    "SELECT * FROM tb WHERE hk = 012-34-5678 AND ssn = ***(Redacted)****(Redacted)****(Redacted)"),
            // Concat operator
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 'abc' || '123' AND ssn = 'abc' || '123' || 'xyz'",
                    "SELECT * FROM tb WHERE hk = 'abc' || '123' AND ssn = ***(Redacted) || ***(Redacted) || ***(Redacted)"),
            // In operator
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk IN (1, 3, 5) AND attr IN (2, 4, 6)",
                    "SELECT * FROM tb WHERE hk IN (1, 3, 5) AND attr IN (***(Redacted), ***(Redacted), ***(Redacted))"),
            // Between operator
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk BETWEEN 1 AND 2 AND attr BETWEEN 1 AND 2",
                    "SELECT * FROM tb WHERE hk BETWEEN 1 AND 2 AND attr BETWEEN ***(Redacted) AND ***(Redacted)"),
            // Comparison operators
            RedactionTestCase(
                    "SELECT * FROM tb WHERE (hk <> 1 or hk < 1 or hk <= 1 or hk > 1 or hk >=1 )",
                    "SELECT * FROM tb WHERE (hk <> 1 or hk < 1 or hk <= 1 or hk > 1 or hk >=1 )"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 'a' and (attr1 <> 1 or attr2 < 1 or attr3 <= 1 or attr4 > 1 or attr5 >=1 )",
                    "SELECT * FROM tb WHERE hk = 'a' and (attr1 <> ***(Redacted) or attr2 < ***(Redacted) or attr3 <= ***(Redacted) or attr4 > ***(Redacted) or attr5 >=***(Redacted) )"),
            // String
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 'a' and attr = 'a'",
                    "SELECT * FROM tb WHERE hk = 'a' and attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = '2016-02-15' and attr = '2016-02-15'",
                    "SELECT * FROM tb WHERE hk = '2016-02-15' and attr = ***(Redacted)"),
            // TODO: Fix metadata length for unicode
//            RedactionTestCase(
//                    "SELECT * FROM tb WHERE hk = '\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE38\uD83D\uDE38' and attr = '\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE38\uD83D\uDE38'",
//                    "SELECT * FROM tb WHERE hk = '\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE38\uD83D\uDE38' and attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = '話家身圧費谷料村能計税金' and attr = '話家身圧費谷料村能計税金'",
                    "SELECT * FROM tb WHERE hk = '話家身圧費谷料村能計税金' and attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 'abcde\\u0832fgh' and attr = 'abcde\\u0832fgh'",
                    "SELECT * FROM tb WHERE hk = 'abcde\\u0832fgh' and attr = ***(Redacted)"),
            // Int
            RedactionTestCase(
                    "SELECT * FROM tb WHERE NOT hk = 1 AND NOT attr = 1",
                    "SELECT * FROM tb WHERE NOT hk = 1 AND NOT attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE NOT hk = 0 AND NOT attr = 0",
                    "SELECT * FROM tb WHERE NOT hk = 0 AND NOT attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = -0 AND attr = -0",
                    "SELECT * FROM tb WHERE hk = -0 AND attr = -***(Redacted)"),
            // Decimal
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 0.123 AND attr = 0.123",
                    "SELECT * FROM tb WHERE hk = 0.123 AND attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = -0.123 AND attr = -0.123",
                    "SELECT * FROM tb WHERE hk = -0.123 AND attr = -***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 0.000 AND attr = 0.000",
                    "SELECT * FROM tb WHERE hk = 0.000 AND attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = -0.000 AND attr = -0.000",
                    "SELECT * FROM tb WHERE hk = -0.000 AND attr = -***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 0.12e-4 AND attr = 0.12e-4",
                    "SELECT * FROM tb WHERE hk = 0.12e-4 AND attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = -0.01200e5 AND attr = -0.01200e5",
                    "SELECT * FROM tb WHERE hk = -0.01200e5 AND attr = -***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 0. AND attr = 0.",
                    "SELECT * FROM tb WHERE hk = 0. AND attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 0E0 AND attr = 0E0",
                    "SELECT * FROM tb WHERE hk = 0E0 AND attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 0E-0 AND attr = 0E-0",
                    "SELECT * FROM tb WHERE hk = 0E-0 AND attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 0.0E1 AND attr = 0.0E1",
                    "SELECT * FROM tb WHERE hk = 0.0E1 AND attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = -0E0 AND attr = -0E0",
                    "SELECT * FROM tb WHERE hk = -0E0 AND attr = -***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = -0. AND attr = -0.",
                    "SELECT * FROM tb WHERE hk = -0. AND attr = -***(Redacted)"),
            // Boolean on non-key attr
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = TRUE AND attr = TRUE",
                    "SELECT * FROM tb WHERE hk = TRUE AND attr = ***(Redacted)"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = FALSE AND attr = FALSE",
                    "SELECT * FROM tb WHERE hk = FALSE AND attr = ***(Redacted)"),
            // NULL on non-key attr
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = NULL AND attr = NULL",
                    "SELECT * FROM tb WHERE hk = NULL AND attr = ***(Redacted)"),
            // Map on non-key attr
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 'a' AND attr = { 'hk' : 'value' }",
                    "SELECT * FROM tb WHERE hk = 'a' AND attr = { ***(Redacted) : ***(Redacted) }"),
            // List
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 'a' AND attr = [ 'value1', 'value2' ]",
                    "SELECT * FROM tb WHERE hk = 'a' AND attr = [ ***(Redacted), ***(Redacted) ]"),
            // Set
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 'a' AND attr = << 'value1', 'value2' >>",
                    "SELECT * FROM tb WHERE hk = 'a' AND attr = << ***(Redacted), ***(Redacted) >>"),
            // Space and new line preserved
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk =1    \n    AND attr =1",
                    "SELECT * FROM tb WHERE hk =1    \n" +
                            "    AND attr =***(Redacted)"),
            RedactionTestCase(
                    "SELECT * \n FROM tb \n WHERE hk =1 \n\r AND attr = 'asdfas \n\r df \n\r sa' AND hk = 2",
                    "SELECT * \n FROM tb \n WHERE hk =1 \n\r AND attr = ***(Redacted) AND hk = 2"),
            // Multiple Args cases
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 1 and rk = '1'",
                    "SELECT * FROM tb WHERE hk = 1 and rk = '1'"),
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 'a' and (rk = 1 or (rk = 2 and attr = '1'))",
                    "SELECT * FROM tb WHERE hk = 'a' and (rk = 1 or (rk = 2 and attr = ***(Redacted)))"),
            // Nested Map
            RedactionTestCase(
                    "SELECT * FROM tb WHERE hk = 'a' AND attr = { 'name' : { 'name' : 'value' } }",
                    "SELECT * FROM tb WHERE hk = 'a' AND attr = { ***(Redacted) : { ***(Redacted) : ***(Redacted) } }"),
            // Insert Into
            RedactionTestCase(
                    "INSERT INTO tb VALUE { 'hk': 'a', 'rk': 1, 'attr': 'b' }",
                    "INSERT INTO tb VALUE { 'hk': 'a', 'rk': 1, 'attr': ***(Redacted) }"),
            RedactionTestCase(
                    "INSERT INTO tb VALUE { 'hk': 'a', 'rk': 1, 'attr': { 'hk': 'a' }}",
                    "INSERT INTO tb VALUE { 'hk': 'a', 'rk': 1, 'attr': { ***(Redacted): ***(Redacted) }}"),
            RedactionTestCase(
                    "INSERT INTO tb VALUE `{ 'hk': 'a', 'rk': 1, 'attr': { 'hk': 'a' }}`",
                    "INSERT INTO tb VALUE ***(Redacted)"),
            RedactionTestCase(
                    "INSERT INTO tb VALUE HK",
                    "INSERT INTO tb VALUE HK"),
            RedactionTestCase(
                    "INSERT INTO tb VALUE hk",
                    "INSERT INTO tb VALUE hk"),
            RedactionTestCase(
                    "INSERT INTO tb VALUE hk = 'a' and attr = 'a'",
                    "INSERT INTO tb VALUE hk = 'a' and attr = ***(Redacted)"),
            RedactionTestCase(
                    "INSERT INTO tb VALUE MISSING",
                    "INSERT INTO tb VALUE MISSING"),
            RedactionTestCase(
                    "INSERT INTO tb VALUE << 'value1', 'value2' >>",
                    "INSERT INTO tb VALUE << ***(Redacted), ***(Redacted) >>"),
            // Update Assignment
            RedactionTestCase(
                    "update nonExistentTable set foo = 'bar' where attr1='testValue'",
                    "update nonExistentTable set foo = ***(Redacted) where attr1=***(Redacted)"),
            RedactionTestCase(
                    "UPDATE tb SET hk = 'b', rk = 2, attr = 2 WHERE hk = 'a' and rk = 1 and attr = 1",
                    "UPDATE tb SET hk = 'b', rk = 2, attr = ***(Redacted) WHERE hk = 'a' and rk = 1 and attr = ***(Redacted)"),
            // Delete
            RedactionTestCase(
                    "DELETE FROM tb WHERE hk = 'a' AND attr = 'b'",
                    "DELETE FROM tb WHERE hk = 'a' AND attr = ***(Redacted)"),
            // Path
            RedactionTestCase(
                    "SELECT ?, tb.a from tb where tb.hk = ? and tb.rk = 'eggs' and tb[*].bar = ? or tb[*].rk = 'foo'",
                    "SELECT ?, tb.a from tb where tb.hk = ? and tb.rk = 'eggs' and tb[*].bar = ? or tb[*].rk = 'foo'"),
            // Parameter
            RedactionTestCase(
                    "SELECT ?, tb.a from tb where ? = 'foo' and ? = ? and ?.rk = 'eggs' and ?[*].bar = ? or ?[*].rk = 'foo'",
                    "SELECT ?, tb.a from tb where ? = 'foo' and ? = ? and ?.rk = 'eggs' and ?[*].bar = ? or ?[*].rk = 'foo'")
    )

    @Test
    fun testDefaultArguments() {
        val originalStatement = "SELECT * FROM tb WHERE hk = 1 AND begins_with(Attr, 'foo', bar)"
        val expectedRedactedStatement = "SELECT * FROM tb WHERE hk = ***(Redacted) AND begins_with(Attr, ***(Redacted), bar)"
        val redactedStatement1 = redact(originalStatement, super.parser.parseExprNode(originalStatement))
        assertEquals(expectedRedactedStatement, redactedStatement1)

        val redactedStatement2 = redact(originalStatement, super.parser.parseExprNode(originalStatement), providedSafeFieldNames = emptySet())
        assertEquals(expectedRedactedStatement, redactedStatement2)

        val redactedStatement3 = redact(originalStatement, super.parser.parseExprNode(originalStatement), userDefinedFunctionRedactionConfig = emptyMap())
        assertEquals(expectedRedactedStatement, redactedStatement3)
    }

    @Test
    fun testInputStatementAstMismatch() {
        val inputStatement = "SELECT * FROM tb WHERE nonKey = 'a'"
        val inputAst = super.parser.parseExprNode("SELECT * FROM tb WHERE hk = 1 AND nonKey = 'a'")
        assertFalse(validateInputAstParsedFromInputStatement(inputStatement, inputAst))

        try {
            redact(inputStatement, inputAst)
            fail("Expected IllegalArgumentException but there was no Exception")
        } catch (e: IllegalArgumentException) {
            assertEquals(INPUT_AST_STATEMENT_MISMATCH, e.message)
        } catch (e: Exception) {
            fail("Expected EvaluationException but a different exception was thrown \n\t  $e")
        }
    }
}
