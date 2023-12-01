package org.partiql.ast.normalize

import org.junit.jupiter.api.Test
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.builder.ast
import org.partiql.ast.exprLit
import org.partiql.ast.exprVar
import org.partiql.ast.identifierSymbol
import org.partiql.ast.selectProjectItemExpression
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import kotlin.test.assertEquals

class NormalizeSelectTest {

    /**
     * SELECT a, b, c FROM T
     *
     * SELECT VALUE {
     *    'a': a,
     *    'b': b,
     *    'c': c
     * } FROM T
     */
    @Test
    fun testDerivedBinders_00() {
        val input = select(
            varItem("a"),
            varItem("b"),
            varItem("c"),
        )
        val expected = selectValue(
            "a" to variable("a"),
            "b" to variable("b"),
            "c" to variable("c"),
        )
        val actual = NormalizeSelect.apply(input)
        assertEquals(expected, actual)
    }

    /**
     * SELECT 1, 2, 3 FROM T
     *
     * SELECT VALUE {
     *      '_1': 1,
     *      '_2': 2,
     *      '_3': 3
     * } FROM T
     */
    @Test
    fun testDerivedBinders_01() {
        val input = select(
            litItem(1),
            litItem(2),
            litItem(3),
        )
        val expected = selectValue(
            "_1" to lit(1),
            "_2" to lit(2),
            "_3" to lit(3),
        )
        val actual = NormalizeSelect.apply(input)
        assertEquals(expected, actual)
    }

    /**
     * SELECT a, 2, 3 FROM T
     *
     * SELECT VALUE {
     *      'a': a,
     *      '_1': 2,
     *      '_2': 3
     * } FROM T
     */
    @Test
    fun testDerivedBinders_02() {
        val input = select(
            varItem("a"),
            litItem(2),
            litItem(3),
        )
        val expected = selectValue(
            "a" to variable("a"),
            "_1" to lit(2),
            "_2" to lit(3),
        )
        val actual = NormalizeSelect.apply(input)
        assertEquals(expected, actual)
    }

    /**
     * SELECT a AS a, 2 AS b, 3 AS c FROM T
     *
     * SELECT VALUE {
     *      'a': a,
     *      'b': 2,
     *      'c': 3
     * } FROM T
     */
    @Test
    fun testDerivedBinders_03() {
        val input = select(
            varItem("a", "a"),
            litItem(2, "b"),
            litItem(3, "c"),
        )
        val expected = selectValue(
            "a" to variable("a"),
            "b" to lit(2),
            "c" to lit(3),
        )
        val actual = NormalizeSelect.apply(input)
        assertEquals(expected, actual)
    }

    // ----- HELPERS -------------------------

    private fun variable(name: String) = exprVar(
        identifier = identifierSymbol(
            symbol = name,
            caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE,
        ),
        scope = Expr.Var.Scope.DEFAULT,
    )

    private fun select(vararg items: Select.Project.Item) = ast {
        statementQuery {
            expr = exprSFW {
                select = selectProject {
                    this.items += items
                }
                from = fromValue {
                    expr = variable("T")
                    type = From.Value.Type.SCAN
                }
            }
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun selectValue(vararg items: Pair<String, Expr>) = ast {
        statementQuery {
            expr = exprSFW {
                select = selectValue {
                    constructor = exprStruct {
                        for ((k, v) in items) {
                            fields += exprStructField {
                                name = exprLit(stringValue(k))
                                value = v
                            }
                        }
                    }
                }
                from = fromValue {
                    expr = exprVar {
                        identifier = identifierSymbol {
                            symbol = "T"
                            caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE
                        }
                        scope = Expr.Var.Scope.DEFAULT
                    }
                    type = From.Value.Type.SCAN
                }
            }
        }
    }

    private fun varItem(symbol: String, asAlias: String? = null) = selectProjectItemExpression(
        expr = variable(symbol),
        asAlias = asAlias?.let { identifierSymbol(asAlias, Identifier.CaseSensitivity.INSENSITIVE) }
    )

    private fun litItem(value: Int, asAlias: String? = null) = selectProjectItemExpression(
        expr = lit(value),
        asAlias = asAlias?.let { identifierSymbol(asAlias, Identifier.CaseSensitivity.INSENSITIVE) }
    )

    @OptIn(PartiQLValueExperimental::class)
    private fun lit(value: Int) = exprLit(int32Value(value))
}
