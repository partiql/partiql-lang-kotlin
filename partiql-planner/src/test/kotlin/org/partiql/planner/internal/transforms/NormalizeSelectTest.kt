package org.partiql.planner.internal.transforms

import org.junit.jupiter.api.Test
import org.partiql.ast.v1.Ast.exprLit
import org.partiql.ast.v1.Ast.exprQuerySet
import org.partiql.ast.v1.Ast.exprStruct
import org.partiql.ast.v1.Ast.exprStructField
import org.partiql.ast.v1.Ast.exprVarRef
import org.partiql.ast.v1.Ast.from
import org.partiql.ast.v1.Ast.fromExpr
import org.partiql.ast.v1.Ast.identifier
import org.partiql.ast.v1.Ast.identifierChain
import org.partiql.ast.v1.Ast.queryBodySFW
import org.partiql.ast.v1.Ast.selectItemExpr
import org.partiql.ast.v1.Ast.selectList
import org.partiql.ast.v1.Ast.selectValue
import org.partiql.ast.v1.FromType
import org.partiql.ast.v1.SelectItem
import org.partiql.ast.v1.expr.Expr
import org.partiql.ast.v1.expr.Scope
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
        val actual = NormalizeSelect.normalize(input)
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
        val actual = NormalizeSelect.normalize(input)
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
        val actual = NormalizeSelect.normalize(input)
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
        val actual = NormalizeSelect.normalize(input)
        assertEquals(expected, actual)
    }

    // ----- HELPERS -------------------------

    private fun variable(name: String) = exprVarRef(
        identifierChain = identifierChain(
            identifier(
                symbol = name,
                isDelimited = false,
            ),
            next = null
        ),
        scope = Scope.DEFAULT(),
    )

    private fun select(vararg items: SelectItem) =
        exprQuerySet(
            body = queryBodySFW(
                select = selectList(
                    items = items.toList(),
                    setq = null
                ),
                exclude = null,
                from = from(
                    listOf(
                        fromExpr(
                            expr = variable("T"),
                            fromType = FromType.SCAN(),
                            asAlias = null,
                            atAlias = null
                        )
                    )
                ),
                let = null,
                where = null,
                groupBy = null,
                having = null,
            ),
            limit = null,
            offset = null,
            orderBy = null
        )

    @OptIn(PartiQLValueExperimental::class)
    private fun selectValue(vararg items: Pair<String, Expr>) =
        exprQuerySet(
            body = queryBodySFW(
                select = selectValue(
                    constructor = exprStruct(
                        items.map {
                            exprStructField(
                                name = exprLit(stringValue(it.first)),
                                value = it.second
                            )
                        }
                    ),
                    setq = null
                ),
                exclude = null,
                from = from(
                    listOf(
                        fromExpr(
                            expr = exprVarRef(
                                identifierChain = identifierChain(
                                    identifier(
                                        symbol = "T",
                                        isDelimited = false
                                    ),
                                    next = null
                                ),
                                scope = Scope.DEFAULT()
                            ),
                            fromType = FromType.SCAN(),
                            asAlias = null,
                            atAlias = null
                        ),
                    )
                ),
                let = null,
                where = null,
                groupBy = null,
                having = null,
            ),
            limit = null,
            offset = null,
            orderBy = null
        )

    private fun varItem(symbol: String, asAlias: String? = null) = selectItemExpr(
        expr = variable(symbol),
        asAlias = asAlias?.let { identifier(asAlias, isDelimited = false) }
    )

    private fun litItem(value: Int, asAlias: String? = null) = selectItemExpr(
        expr = lit(value),
        asAlias = asAlias?.let { identifier(asAlias, isDelimited = false) }
    )

    @OptIn(PartiQLValueExperimental::class)
    private fun lit(value: Int) = exprLit(int32Value(value))
}
