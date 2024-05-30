package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.exprCall
import org.partiql.ast.identifierQualified
import org.partiql.ast.identifierSymbol
import org.partiql.ast.statementQuery
import kotlin.test.assertEquals

class PartiQLParserFunctionCallTests {

    private val parser = PartiQLParserDefault()

    private inline fun query(body: () -> Expr) = statementQuery(body())

    @Test
    fun callUnqualifiedNonReservedInsensitive() = assertExpression(
        "foo()",
        query {
            exprCall(
                function = identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callUnqualifiedNonReservedSensitive() = assertExpression(
        "\"foo\"()",
        query {
            exprCall(
                function = identifierSymbol("foo", Identifier.CaseSensitivity.SENSITIVE),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callUnqualifiedReservedInsensitive() = assertExpression(
        "upper()",
        query {
            exprCall(
                function = identifierSymbol("upper", Identifier.CaseSensitivity.INSENSITIVE),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callUnqualifiedReservedSensitive() = assertExpression(
        "\"upper\"()",
        query {
            exprCall(
                function = identifierSymbol("upper", Identifier.CaseSensitivity.SENSITIVE),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callQualifiedNonReservedInsensitive() = assertExpression(
        "my_catalog.my_schema.foo()",
        query {
            exprCall(
                function = identifierQualified(
                    root = identifierSymbol("my_catalog", Identifier.CaseSensitivity.INSENSITIVE),
                    steps = listOf(
                        identifierSymbol("my_schema", Identifier.CaseSensitivity.INSENSITIVE),
                        identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
                    )
                ),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callQualifiedNonReservedSensitive() = assertExpression(
        "my_catalog.my_schema.\"foo\"()",
        query {
            exprCall(
                function = identifierQualified(
                    root = identifierSymbol("my_catalog", Identifier.CaseSensitivity.INSENSITIVE),
                    steps = listOf(
                        identifierSymbol("my_schema", Identifier.CaseSensitivity.INSENSITIVE),
                        identifierSymbol("foo", Identifier.CaseSensitivity.SENSITIVE),
                    )
                ),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callQualifiedReservedInsensitive() = assertExpression(
        "my_catalog.my_schema.upper()",
        query {
            exprCall(
                function = identifierQualified(
                    root = identifierSymbol("my_catalog", Identifier.CaseSensitivity.INSENSITIVE),
                    steps = listOf(
                        identifierSymbol("my_schema", Identifier.CaseSensitivity.INSENSITIVE),
                        identifierSymbol("upper", Identifier.CaseSensitivity.INSENSITIVE),
                    )
                ),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callQualifiedReservedSensitive() = assertExpression(
        "my_catalog.my_schema.\"upper\"()",
        query {
            exprCall(
                function = identifierQualified(
                    root = identifierSymbol("my_catalog", Identifier.CaseSensitivity.INSENSITIVE),
                    steps = listOf(
                        identifierSymbol("my_schema", Identifier.CaseSensitivity.INSENSITIVE),
                        identifierSymbol("upper", Identifier.CaseSensitivity.SENSITIVE),
                    )
                ),
                args = emptyList(),
                setq = null
            )
        }
    )

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        val actual = result.root
        assertEquals(expected, actual)
    }
}
