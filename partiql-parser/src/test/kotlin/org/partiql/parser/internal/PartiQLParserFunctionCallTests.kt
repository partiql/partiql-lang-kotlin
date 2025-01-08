package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.query
import org.partiql.ast.AstNode
import org.partiql.ast.Identifier
import org.partiql.ast.Identifier.Simple.delimited
import org.partiql.ast.Identifier.Simple.regular
import org.partiql.ast.expr.Expr
import kotlin.test.assertEquals

class PartiQLParserFunctionCallTests {

    private val parser = PartiQLParserDefault()

    private inline fun queryBody(body: () -> Expr) = query(body())

    @Test
    fun callUnqualifiedNonReservedInsensitive() = assertExpression(
        "foo()",
        queryBody {
            exprCall(
                function = Identifier.regular("foo"),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callUnqualifiedNonReservedSensitive() = assertExpression(
        "\"foo\"()",
        queryBody {
            exprCall(
                function = Identifier.delimited("foo"),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callUnqualifiedReservedInsensitive() = assertExpression(
        "upper()",
        queryBody {
            exprCall(
                function = Identifier.regular("upper"),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callUnqualifiedReservedSensitive() = assertExpression(
        "\"upper\"()",
        queryBody {
            exprCall(
                function = Identifier.delimited("upper"),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callQualifiedNonReservedInsensitive() = assertExpression(
        "my_catalog.my_schema.foo()",
        queryBody {
            exprCall(
                function = Identifier.of(
                    regular("my_catalog"),
                    regular("my_schema"),
                    regular("foo")
                ),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callQualifiedNonReservedSensitive() = assertExpression(
        "my_catalog.my_schema.\"foo\"()",
        queryBody {
            exprCall(
                function = Identifier.of(
                    regular("my_catalog"),
                    regular("my_schema"),
                    delimited("foo")
                ),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callQualifiedReservedInsensitive() = assertExpression(
        "my_catalog.my_schema.upper()",
        queryBody {
            exprCall(
                function = Identifier.of(
                    regular("my_catalog"),
                    regular("my_schema"),
                    regular("upper")
                ),
                args = emptyList(),
                setq = null
            )
        }
    )

    @Test
    fun callQualifiedReservedSensitive() = assertExpression(
        "my_catalog.my_schema.\"upper\"()",
        queryBody {
            exprCall(
                function = Identifier.of(
                    regular("my_catalog"),
                    regular("my_schema"),
                    delimited("upper")
                ),
                args = emptyList(),
                setq = null
            )
        }
    )

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        assertEquals(1, result.statements.size)
        val actual = result.statements[0]
        assertEquals(expected, actual)
    }
}
