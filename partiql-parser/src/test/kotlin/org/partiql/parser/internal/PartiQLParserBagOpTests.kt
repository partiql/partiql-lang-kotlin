package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.v1.Ast.exprBag
import org.partiql.ast.v1.Ast.exprLit
import org.partiql.ast.v1.Ast.exprQuerySet
import org.partiql.ast.v1.Ast.exprStruct
import org.partiql.ast.v1.Ast.exprStructField
import org.partiql.ast.v1.Ast.from
import org.partiql.ast.v1.Ast.fromExpr
import org.partiql.ast.v1.Ast.query
import org.partiql.ast.v1.Ast.queryBodySFW
import org.partiql.ast.v1.Ast.queryBodySetOp
import org.partiql.ast.v1.Ast.selectStar
import org.partiql.ast.v1.Ast.setOp
import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.FromType
import org.partiql.ast.v1.SetOpType
import org.partiql.ast.v1.SetQuantifier
import org.partiql.ast.v1.expr.Expr
import org.partiql.ast.v1.expr.ExprQuerySet
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import kotlin.test.assertEquals

class PartiQLParserBagOpTests {

    private val parser = V1PartiQLParserDefault()

    private fun queryBody(body: () -> Expr) = query(body())

    @OptIn(PartiQLValueExperimental::class)
    private fun createSFW(i: Int): ExprQuerySet =
        exprQuerySet(
            body = queryBodySFW(
                select = selectStar(setq = null),
                from = from(
                    tableRefs = listOf(
                        fromExpr(
                            expr = exprBag(
                                values = mutableListOf(
                                    exprStruct(
                                        fields = mutableListOf(
                                            exprStructField(
                                                name = exprLit(value = stringValue("a")),
                                                value = exprLit(value = int32Value(i))
                                            )
                                        )
                                    )
                                )
                            ),
                            fromType = FromType.SCAN(),
                            asAlias = null,
                            atAlias = null
                        )
                    )
                ),
                exclude = null,
                let = null,
                where = null,
                groupBy = null,
                having = null,
            ),
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
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.UNION(),
                        setq = null
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun sqlUnionMultiple() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION ALL SELECT * FROM <<{'a': 2}>> UNION DISTINCT SELECT * FROM <<{'a': 3}>>",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.UNION(),
                        setq = SetQuantifier.DISTINCT()
                    ),
                    lhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.UNION(),
                                setq = SetQuantifier.ALL()
                            ),
                            lhs = createSFW(1),
                            rhs = createSFW(2),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    rhs = createSFW(3),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun sqlUnionMultipleRight() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION ALL (SELECT * FROM <<{'a': 2}>> UNION DISTINCT SELECT * FROM <<{'a': 3}>>)",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.UNION(),
                        setq = SetQuantifier.ALL()
                    ),
                    lhs = createSFW(1),
                    rhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.UNION(),
                                setq = SetQuantifier.DISTINCT()
                            ),
                            lhs = createSFW(2),
                            rhs = createSFW(3),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    // Outer Union
    @Test
    fun outerUnion() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> OUTER UNION SELECT * FROM <<{'a': 2}>>",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.UNION(),
                        setq = null
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    isOuter = true
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun outerUnionNonSpecified() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION 2",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.UNION(),
                        setq = null
                    ),
                    lhs = createSFW(1),
                    rhs = createLit(2),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun sqlUnionAndOuterUnion() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION ALL SELECT * FROM <<{'a': 2}>> UNION DISTINCT 3",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.UNION(),
                        setq = SetQuantifier.DISTINCT()
                    ),
                    lhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.UNION(),
                                setq = SetQuantifier.ALL()
                            ),
                            lhs = createSFW(1),
                            rhs = createSFW(2),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    rhs = createLit(3),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun outerUnionAndSQLUnion() = assertExpression(
        "1 UNION ALL SELECT * FROM <<{'a': 2}>> UNION DISTINCT SELECT * FROM <<{'a': 3}>>",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.UNION(),
                        setq = SetQuantifier.DISTINCT()
                    ),
                    lhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.UNION(),
                                setq = SetQuantifier.ALL()
                            ),
                            lhs = createLit(1),
                            rhs = createSFW(2),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    rhs = createSFW(3),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    // SQL Except
    @Test
    fun sqlExcept() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT SELECT * FROM <<{'a': 2}>>",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.EXCEPT(),
                        setq = null
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun sqlExceptMultiple() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT ALL SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT SELECT * FROM <<{'a': 3}>>",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.EXCEPT(),
                        setq = SetQuantifier.DISTINCT()
                    ),
                    lhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.EXCEPT(),
                                setq = SetQuantifier.ALL()
                            ),
                            lhs = createSFW(1),
                            rhs = createSFW(2),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    rhs = createSFW(3),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun sqlExceptMultipleRight() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT ALL (SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT SELECT * FROM <<{'a': 3}>>)",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.EXCEPT(),
                        setq = SetQuantifier.ALL()
                    ),
                    lhs = createSFW(1),
                    rhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.EXCEPT(),
                                setq = SetQuantifier.DISTINCT()
                            ),
                            lhs = createSFW(2),
                            rhs = createSFW(3),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    // Outer Except
    @Test
    fun outerExcept() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> OUTER EXCEPT SELECT * FROM <<{'a': 2}>>",
        queryBody {
            exprQuerySet(

                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.EXCEPT(),
                        setq = null
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    isOuter = true
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun outerExceptNonSpecified() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT 2",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.EXCEPT(),
                        setq = null
                    ),
                    lhs = createSFW(1),
                    rhs = createLit(2),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun sqlExceptAndOuterExcept() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT ALL SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT 3",
        queryBody {
            exprQuerySet(

                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.EXCEPT(),
                        setq = SetQuantifier.DISTINCT()
                    ),
                    lhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.EXCEPT(),
                                setq = SetQuantifier.ALL()
                            ),
                            lhs = createSFW(1),
                            rhs = createSFW(2),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    rhs = createLit(3),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun outerExceptAndSQLExcept() = assertExpression(
        "1 EXCEPT ALL SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT SELECT * FROM <<{'a': 3}>>",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.EXCEPT(),
                        setq = SetQuantifier.DISTINCT()
                    ),
                    lhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.EXCEPT(),
                                setq = SetQuantifier.ALL()
                            ),
                            lhs = createLit(1),
                            rhs = createSFW(2),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    rhs = createSFW(3),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    // SQL Intersect
    @Test
    fun sqlIntersect() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT SELECT * FROM <<{'a': 2}>>",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.INTERSECT(),
                        setq = null
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun sqlIntersectMultiple() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT ALL SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT SELECT * FROM <<{'a': 3}>>",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.INTERSECT(),
                        setq = SetQuantifier.DISTINCT()
                    ),
                    lhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.INTERSECT(),
                                setq = SetQuantifier.ALL()
                            ),
                            lhs = createSFW(1),
                            rhs = createSFW(2),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    rhs = createSFW(3),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun sqlIntersectMultipleRight() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT ALL (SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT SELECT * FROM <<{'a': 3}>>)",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.INTERSECT(),
                        setq = SetQuantifier.ALL()
                    ),
                    lhs = createSFW(1),
                    rhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.INTERSECT(),
                                setq = SetQuantifier.DISTINCT()
                            ),
                            lhs = createSFW(2),
                            rhs = createSFW(3),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    // Outer Intersect
    @Test
    fun outerIntersect() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> OUTER INTERSECT SELECT * FROM <<{'a': 2}>>",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.INTERSECT(),
                        setq = null
                    ),
                    lhs = createSFW(1),
                    rhs = createSFW(2),
                    isOuter = true
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun outerIntersectNonSpecified() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT 2",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.INTERSECT(),
                        setq = null
                    ),
                    lhs = createSFW(1),
                    rhs = createLit(2),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun sqlIntersectAndOuterIntersect() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT ALL SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT 3",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.INTERSECT(),
                        setq = SetQuantifier.DISTINCT()
                    ),
                    lhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.INTERSECT(),
                                setq = SetQuantifier.ALL()
                            ),
                            lhs = createSFW(1),
                            rhs = createSFW(2),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    rhs = createLit(3),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    @Test
    fun outerIntersectAndSQLIntersect() = assertExpression(
        "1 INTERSECT ALL SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT SELECT * FROM <<{'a': 3}>>",
        queryBody {
            exprQuerySet(
                body = queryBodySetOp(
                    type = setOp(
                        setOpType = SetOpType.INTERSECT(),
                        setq = SetQuantifier.DISTINCT()
                    ),
                    lhs = exprQuerySet(
                        body = queryBodySetOp(
                            type = setOp(
                                setOpType = SetOpType.INTERSECT(),
                                setq = SetQuantifier.ALL()
                            ),
                            lhs = createLit(1),
                            rhs = createSFW(2),
                            isOuter = false
                        ),
                        orderBy = null,
                        limit = null,
                        offset = null
                    ),
                    rhs = createSFW(3),
                    isOuter = false
                ),
                orderBy = null,
                limit = null,
                offset = null
            )
        }
    )

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        val actual = result.root
        assertEquals(expected, actual)
    }
}
