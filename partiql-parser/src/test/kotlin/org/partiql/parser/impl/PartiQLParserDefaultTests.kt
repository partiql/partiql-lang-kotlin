package org.partiql.parser.impl

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.ast.Ast
import org.partiql.ast.AstNode
import org.partiql.ast.Identifier
import org.partiql.parser.PartiQLParserException
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.decimalValue
import java.io.File
import java.util.stream.Stream

class PartiQLParserDefaultTests {

    private val parser = PartiQLParserDefault()

    data class ParserTestCase(
        val input: String,
        val expected: AstNode
    )

    // TODO: Add expected exception information
    data class ParserFailingTestCase(
        val input: String,
    )

    @ParameterizedTest
    @ArgumentsSource(HandwrittenFunctionTestCases::class)
    fun test(tc: ParserTestCase) {
        val result = assertDoesNotThrow { parser.parse(tc.input) }
        // TODO: Once we can assert equality:
        //  assertEquals(tc.expected, result.root)
    }

    @ParameterizedTest
    @ArgumentsSource(CreateBuiltInFunctionTestCases::class)
    fun testPartiQLBuiltIns(tc: ParserTestCase) {
        val result = assertDoesNotThrow { parser.parse(tc.input) }
        // TODO: Once we can assert equality:
        //  assertEquals(tc.expected, result.root)
    }

    @ParameterizedTest
    @ArgumentsSource(FailingTestCases::class)
    fun testMalformedQueries(tc: ParserFailingTestCase) {
        val exception = assertThrows<PartiQLParserException> { parser.parse(tc.input) }
        // TODO: Assert some locations?
    }

    class HandwrittenFunctionTestCases : ArgumentsProviderBase() {
        @OptIn(PartiQLValueExperimental::class)
        override fun getParameters(): List<ParserTestCase> = listOf(
            ParserTestCase(
                input = """
                    CREATE FUNCTION PI () RETURNS DECIMAL RETURN 3.14;
                """.trimIndent(),
                expected = Ast.statementDDLCreateFunction(
                    specification = Ast.statementDDLCreateFunctionSpecification(
                        name = Ast.identifierQualified(
                            root = Ast.identifierSymbol("PI", caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE),
                            steps = emptyList()
                        ),
                        parameters = emptyList(),
                        returns = Ast.statementDDLCreateFunctionReturns(
                            type = Ast.typeDecimal(null, null)
                        ),
                        characteristics = emptyList()
                    ),
                    body = Ast.statementRoutineBodyInternal(
                        procedureStatement = Ast.statementReturn(
                            value = Ast.exprLit(decimalValue(3.14.toBigDecimal()))
                        )
                    )
                )
            ),
            ParserTestCase(
                input = """
                    CREATE FUNCTION "MOD" (
                    N1 DECIMAL ( 10, 0 ),
                    N2 DECIMAL ( 10, 0 ) )
                    RETURNS DECIMAL ( 10, 0 )
                    SPECIFIC MODDECIMALMP_DECIMALP
                    RETURN MOD ( N1, N2 ) ;
                """.trimIndent(),
                expected = Ast.typeNullType() // TODO
            ),
            ParserTestCase(
                input = """
                    CREATE FUNCTION "MOD" (
                    N2 DECIMAL ( 10, 0 ) )
                    RETURNS DECIMAL ( 10, 0 )
                    SPECIFIC MODDECIMALMP_DECIMALP
                    RETURN MOD ( N1, N2 ) ;
                """.trimIndent(),
                expected = Ast.typeNullType() // TODO
            ),
            ParserTestCase(
                input = """
                    CREATE FUNCTION "BIT_LENGTH"(
                        S1 CHARACTER VARYING)
                    RETURNS NUMERIC
                    SPECIFIC BIT_LENGTH2
                    RETURN BIT_LENGTH ( S1 ) ;
                """.trimIndent(),
                expected = Ast.typeNullType() // TODO
            ),
            // Without parameter name
            ParserTestCase(
                input = """
                    CREATE FUNCTION "BIT_LENGTH"(
                        CHARACTER VARYING)
                    RETURNS NUMERIC
                    SPECIFIC BIT_LENGTH2
                    RETURN BIT_LENGTH ( S1 ) ;
                """.trimIndent(),
                expected = Ast.typeNullType() // TODO
            ),
            // Custom type
            ParserTestCase(
                input = """
                    CREATE FUNCTION HelloWorld (a custom_type) RETURNS INT RETURN 1;
                """.trimIndent(),
                expected = Ast.typeNullType() // TODO
            ),
            // Custom type without param name
            ParserTestCase(
                input = """
                    CREATE FUNCTION HelloWorld (custom_type) RETURNS INT RETURN 1;
                """.trimIndent(),
                expected = Ast.typeNullType() // TODO
            ),
            // Pre-defined type without param name
            ParserTestCase(
                input = """
                    CREATE FUNCTION HelloWorld (INT) RETURNS INT RETURN 1;
                """.trimIndent(),
                expected = Ast.typeNullType() // TODO
            ),
        )
    }

    class CreateBuiltInFunctionTestCases : ArgumentsProviderBase() {
        private val loader: ClassLoader = PartiQLParserDefault::class.java.classLoader
        private val resource = loader.getResource("built-in-functions/partiql-converted")!!
        private val file = File(resource.toURI())
        override fun getParameters() = file.listFiles()!!.map { statementFile ->
            val statement = statementFile.readText()
            ParserTestCase(input = statement, expected = Ast.typeNullType())
        }
    }

    class FailingTestCases : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // Missing FUNCTION
            ParserFailingTestCase(
                input = """
                    CREATE FUNC HelloWorld () RETURNS INT RETURN 1;
                """.trimIndent()
            ),
            // No PARENS
            ParserFailingTestCase(
                input = """
                    CREATE FUNCTION HelloWorld RETURNS INT RETURN 1;
                """.trimIndent()
            ),
            // RETURN before RETURNS
            ParserFailingTestCase(
                input = """
                    CREATE FUNCTION HelloWorld () RETURN 1 RETURNS INT;
                """.trimIndent()
            ),
            // COMMA in arguments
            ParserFailingTestCase(
                input = """
                    CREATE FUNCTION HelloWorld (,) RETURNS INT RETURN 1;
                """.trimIndent()
            ),
            // SPECIFIC before RETURNS
            ParserFailingTestCase(
                input = """
                    CREATE FUNCTION HelloWorld () SPECIFIC HelloWorld1 RETURNS INT RETURN 1;
                """.trimIndent()
            ),
            // Missing return
            ParserFailingTestCase(
                input = """
                    CREATE FUNCTION HelloWorld () RETURNS INT;
                """.trimIndent()
            ),
            // TODO: This should eventually pass
            ParserFailingTestCase(
                input = """
                    CREATE FUNCTION SomeCatalog.HelloWorld () RETURNS INT RETURN 1;
                """.trimIndent()
            ),
            // Trailing COMMA
            ParserFailingTestCase(
                input = """
                    CREATE FUNCTION HelloWorld (a INT,) RETURNS INT RETURN 1;
                """.trimIndent()
            ),
            // Expression instead of RETURN
            ParserFailingTestCase(
                input = """
                    CREATE FUNCTION HelloWorld (a INT) RETURNS INT 1;
                """.trimIndent()
            ),
            // Expression instead of RETURN
            ParserFailingTestCase(
                input = """
                    CREATE FUNCTION HelloWorld (a INT) RETURNS INT SELECT 1 FROM <<>>;
                """.trimIndent()
            ),
            // SPECIFIC not name
            ParserFailingTestCase(
                input = """
                    CREATE FUNCTION HelloWorld (a INT) RETURNS INT SPECIFIC 1 RETURN 1;
                """.trimIndent()
            ),
        )
    }

    abstract class ArgumentsProviderBase : ArgumentsProvider {
        abstract fun getParameters(): List<Any>

        @Throws(Exception::class)
        override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments>? {
            return getParameters().map { Arguments.of(it) }.stream()
        }
    }
}
