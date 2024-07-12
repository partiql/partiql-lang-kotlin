package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.SetOp
import org.partiql.ast.SetQuantifier
import org.partiql.ast.exprBagOp
import org.partiql.ast.exprCollection
import org.partiql.ast.exprLit
import org.partiql.ast.exprSFW
import org.partiql.ast.exprStruct
import org.partiql.ast.exprStructField
import org.partiql.ast.fromValue
import org.partiql.ast.selectStar
import org.partiql.ast.setOp
import org.partiql.ast.statementQuery
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import kotlin.test.assertEquals

class PartiQLParserBagOpTests {

    private val parser = PartiQLParserDefault()

    private inline fun query(body: () -> Expr) = statementQuery(body())

    @OptIn(PartiQLValueExperimental::class)
    private fun createSFW(i: Int): Expr.SFW =
        exprSFW(
            select = selectStar(null),
            from = fromValue(
                expr = exprCollection(
                    type = Expr.Collection.Type.BAG,
                    values = listOf(
                        exprStruct(
                            listOf(
                                exprStructField(
                                    exprLit(stringValue("a")),
                                    exprLit(int32Value(i)),
                                )
                            )
                        )
                    )
                ),
                type = From.Value.Type.SCAN,
                asAlias = null,
                atAlias = null,
                byAlias = null,
            ),
            exclude = null,
            let = null,
            `where` = null,
            groupBy = null,
            having = null,
            setOp = null,
            orderBy = null,
            limit = null,
            offset = null
        )

    @OptIn(PartiQLValueExperimental::class)
    private fun createLit(i: Int) = exprLit(int32Value(i))

    // SQL Union
    @Test
    fun sqlUnion() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION SELECT * FROM <<{'a': 2}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.UNION,
                    null
                ),
                lhs = createSFW(1),
                rhs = createSFW(2),
                outer = false
            )
        }
    )

    @Test
    fun sqlUnionMultiple() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION ALL SELECT * FROM <<{'a': 2}>> UNION DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.UNION,
                    SetQuantifier.DISTINCT
                ),
                lhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.UNION,
                        SetQuantifier.ALL
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    outer = false
                ),
                rhs = createSFW(3),
                outer = false
            )
        }
    )

    @Test
    fun sqlUnionMultipleRight() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION ALL (SELECT * FROM <<{'a': 2}>> UNION DISTINCT SELECT * FROM <<{'a': 3}>>)",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.UNION,
                    SetQuantifier.ALL
                ),
                lhs = createSFW(1),
                rhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.UNION,
                        SetQuantifier.DISTINCT
                    ),
                    lhs = createSFW(2),
                    rhs = createSFW(3),
                    outer = false
                ),
                outer = false
            )
        }
    )

    // Outer Union
    @Test
    fun outerUnion() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> OUTER UNION SELECT * FROM <<{'a': 2}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.UNION,
                    null
                ),
                lhs = createSFW(1),
                rhs = createSFW(2),
                outer = true
            )
        }
    )

    @Test
    fun outerUnionNonSpecified() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION 2",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.UNION,
                    null
                ),
                lhs = createSFW(1),
                rhs = createLit(2),
                outer = true
            )
        }
    )

    @Test
    fun sqlUnionAndOuterUnion() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION ALL SELECT * FROM <<{'a': 2}>> UNION DISTINCT 3",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.UNION,
                    SetQuantifier.DISTINCT
                ),
                lhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.UNION,
                        SetQuantifier.ALL
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    outer = false
                ),
                rhs = createLit(3),
                outer = true // outer
            )
        }
    )

    @Test
    fun outerUnionAndSQLUnion() = assertExpression(
        "1 UNION ALL SELECT * FROM <<{'a': 2}>> UNION DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.UNION,
                    SetQuantifier.DISTINCT
                ),
                lhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.UNION,
                        SetQuantifier.ALL
                    ),
                    lhs = createLit(1),
                    rhs = createSFW(2),
                    outer = true // outer
                ),
                rhs = createSFW(3),
                outer = true // also outer
            )
        }
    )

    // SQL Except
    @Test
    fun sqlExcept() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT SELECT * FROM <<{'a': 2}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.EXCEPT,
                    null
                ),
                lhs = createSFW(1),
                rhs = createSFW(2),
                outer = false
            )
        }
    )

    @Test
    fun sqlExceptMultiple() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT ALL SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.EXCEPT,
                    SetQuantifier.DISTINCT
                ),
                lhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.EXCEPT,
                        SetQuantifier.ALL
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    outer = false
                ),
                rhs = createSFW(3),
                outer = false
            )
        }
    )

    @Test
    fun sqlExceptMultipleRight() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT ALL (SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT SELECT * FROM <<{'a': 3}>>)",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.EXCEPT,
                    SetQuantifier.ALL
                ),
                lhs = createSFW(1),
                rhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.EXCEPT,
                        SetQuantifier.DISTINCT
                    ),
                    lhs = createSFW(2),
                    rhs = createSFW(3),
                    outer = false
                ),
                outer = false
            )
        }
    )

    // Outer Except
    @Test
    fun outerExcept() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> OUTER EXCEPT SELECT * FROM <<{'a': 2}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.EXCEPT,
                    null
                ),
                lhs = createSFW(1),
                rhs = createSFW(2),
                outer = true
            )
        }
    )

    @Test
    fun outerExceptNonSpecified() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT 2",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.EXCEPT,
                    null
                ),
                lhs = createSFW(1),
                rhs = createLit(2),
                outer = true
            )
        }
    )

    @Test
    fun sqlExceptAndOuterExcept() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT ALL SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT 3",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.EXCEPT,
                    SetQuantifier.DISTINCT
                ),
                lhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.EXCEPT,
                        SetQuantifier.ALL
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    outer = false
                ),
                rhs = createLit(3),
                outer = true // outer
            )
        }
    )

    @Test
    fun outerExceptAndSQLExcept() = assertExpression(
        "1 EXCEPT ALL SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.EXCEPT,
                    SetQuantifier.DISTINCT
                ),
                lhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.EXCEPT,
                        SetQuantifier.ALL
                    ),
                    lhs = createLit(1),
                    rhs = createSFW(2),
                    outer = true // outer
                ),
                rhs = createSFW(3),
                outer = true // also outer
            )
        }
    )

    // SQL Intersect
    @Test
    fun sqlIntersect() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT SELECT * FROM <<{'a': 2}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.INTERSECT,
                    null
                ),
                lhs = createSFW(1),
                rhs = createSFW(2),
                outer = false
            )
        }
    )

    @Test
    fun sqlIntersectMultiple() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT ALL SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.INTERSECT,
                    SetQuantifier.DISTINCT
                ),
                lhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.INTERSECT,
                        SetQuantifier.ALL
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    outer = false
                ),
                rhs = createSFW(3),
                outer = false
            )
        }
    )

    @Test
    fun sqlIntersectMultipleRight() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT ALL (SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT SELECT * FROM <<{'a': 3}>>)",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.INTERSECT,
                    SetQuantifier.ALL
                ),
                lhs = createSFW(1),
                rhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.INTERSECT,
                        SetQuantifier.DISTINCT
                    ),
                    lhs = createSFW(2),
                    rhs = createSFW(3),
                    outer = false
                ),
                outer = false
            )
        }
    )

    // Outer Intersect
    @Test
    fun outerIntersect() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> OUTER INTERSECT SELECT * FROM <<{'a': 2}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.INTERSECT,
                    null
                ),
                lhs = createSFW(1),
                rhs = createSFW(2),
                outer = true
            )
        }
    )

    @Test
    fun outerIntersectNonSpecified() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT 2",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.INTERSECT,
                    null
                ),
                lhs = createSFW(1),
                rhs = createLit(2),
                outer = true
            )
        }
    )

    @Test
    fun sqlIntersectAndOuterIntersect() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT ALL SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT 3",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.INTERSECT,
                    SetQuantifier.DISTINCT
                ),
                lhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.INTERSECT,
                        SetQuantifier.ALL
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    outer = false
                ),
                rhs = createLit(3),
                outer = true // outer
            )
        }
    )

    @Test
    fun outerIntersectAndSQLIntersect() = assertExpression(
        "1 INTERSECT ALL SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprBagOp(
                type = setOp(
                    SetOp.Type.INTERSECT,
                    SetQuantifier.DISTINCT
                ),
                lhs = exprBagOp(
                    type = setOp(
                        SetOp.Type.INTERSECT,
                        SetQuantifier.ALL
                    ),
                    lhs = createLit(1),
                    rhs = createSFW(2),
                    outer = true // outer
                ),
                rhs = createSFW(3),
                outer = true // also outer
            )
        }
    )

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        val actual = result.root
        assertEquals(expected, actual)
    }
}
