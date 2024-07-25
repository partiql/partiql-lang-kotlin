package org.partiql.lang.syntax

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.util.ArgumentsProviderBase

internal class PartiQLParserDDLTest : PartiQLParserTestBase() {
    // As we expended the functionality of DDL, making sure that the PIG Parser is not impacted.

    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT)

    internal data class ParserErrorTestCase(
        val description: String? = null,
        val query: String,
        val code: ErrorCode,
        val context: Map<Property, Any> = emptyMap()
    )

    @ArgumentsSource(ErrorTestProvider::class)
    @ParameterizedTest
    fun errorTests(tc: ParserErrorTestCase) = checkInputThrowingParserException(tc.query, tc.code, tc.context, assertContext = false)

    class ErrorTestProvider : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            ParserErrorTestCase(
                description = "PIG Parser does not support qualified Identifier as input for Create",
                query = "CREATE TABLE foo.bar",
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf()
            ),
            ParserErrorTestCase(
                description = "PIG Parser does not support qualified Identifier as input for DROP",
                query = "DROP Table foo.bar",
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),
            ParserErrorTestCase(
                description = "PIG Parser does not support Unique Constraints in CREATE TABLE",
                query = """
                    CREATE TABLE tbl (
                        a INT2 UNIQUE
                    )
                """.trimIndent(),
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),
            ParserErrorTestCase(
                description = "PIG Parser does not support Primary Key Constraint in CREATE TABLE",
                query = """
                    CREATE TABLE tbl (
                        a INT2 PRIMARY KEY
                    )
                """.trimIndent(),
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),
            ParserErrorTestCase(
                description = "PIG Parser does not support CHECK Constraint in CREATE TABLE",
                query = """
                    CREATE TABLE tbl (
                        a INT2 CHECK(a > 0)
                    )
                """.trimIndent(),
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),
            ParserErrorTestCase(
                description = "PIG Parser does not support table constraint in CREATE TABLE",
                query = """
                    CREATE TABLE tbl (
                       check (a > 0)
                    )
                """.trimIndent(),
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),
            ParserErrorTestCase(
                description = "PIG Parser does not support Struct Type with field declaration",
                query = """
                    CREATE TABLE tbl (
                       a STRUCT<b : INT2>
                    )
                """.trimIndent(),
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),
            ParserErrorTestCase(
                description = "PIG Parser does not support element type declaration in Array Type",
                query = """
                    CREATE TABLE tbl (
                       a ARRAY<INT2>
                    )
                """.trimIndent(),
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),

            // Putting those tests here are they are impacted by DDL implementation
            ParserErrorTestCase(
                description = "PIG Parser does not support Struct Type with field declaration -- IS operator",
                query = """
                    a IS STRUCT<b : INT2>
                """.trimIndent(),
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),
            ParserErrorTestCase(
                description = "PIG Parser does not support Struct Type with field declaration -- CAST operator",
                query = """
                    CAST(a AS STRUCT<b : INT2>)
                """.trimIndent(),
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),
            ParserErrorTestCase(
                description = "PIG Parser does not support element type declaration in Array Type -- IS operator",
                query = """
                    a IS ARRAY<INT2>
                """.trimIndent(),
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),
            ParserErrorTestCase(
                description = "PIG Parser does not support element type declaration in Array Type -- cast operator",
                query = """
                    CAST(a AS ARRAY<INT2>)
                """.trimIndent(),
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(),
            ),
        )
    }
}
