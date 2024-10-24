package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.v1.Ast.exprCall
import org.partiql.ast.v1.Ast.identifier
import org.partiql.ast.v1.Ast.identifierChain
import org.partiql.ast.v1.Ast.query
import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.expr.Expr
import kotlin.test.assertEquals

class PartiQLParserFunctionCallTests {

    private val parser = V1PartiQLParserDefault()

    private inline fun queryBody(body: () -> Expr) = query(body())

    @Test
    fun callUnqualifiedNonReservedInsensitive() = assertExpression(
        "foo()",
        queryBody {
            exprCall(
                function = identifierChain(identifier("foo", false), null),
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
                function = identifierChain(identifier("foo", true), null),
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
                function = identifierChain(identifier("upper", false), null),
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
                function = identifierChain(identifier("upper", true), null),
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
                function = identifierChain(
                    root = identifier("my_catalog", false),
                    next = identifierChain(
                        root = identifier("my_schema", false),
                        next = identifierChain(identifier("foo", false), null),
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
        queryBody {
            exprCall(
                function = identifierChain(
                    root = identifier("my_catalog", false),
                    next = identifierChain(
                        identifier("my_schema", false),
                        identifierChain(identifier("foo", true), null),
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
        queryBody {
            exprCall(
                function = identifierChain(
                    root = identifier("my_catalog", false),
                    next = identifierChain(
                        identifier("my_schema", false),
                        identifierChain(identifier("upper", false), null),
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
        queryBody {
            exprCall(
                function = identifierChain(
                    root = identifier("my_catalog", false),
                    next = identifierChain(
                        identifier("my_schema", false),
                        identifierChain(identifier("upper", true), null),
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
