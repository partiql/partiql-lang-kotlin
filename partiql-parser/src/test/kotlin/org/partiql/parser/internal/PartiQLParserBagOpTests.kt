package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprBag
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprStruct
import org.partiql.ast.Ast.exprStructField
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.query
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.queryBodySetOp
import org.partiql.ast.Ast.selectStar
import org.partiql.ast.Ast.setOp
import org.partiql.ast.AstNode
import org.partiql.ast.FromType
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.literal.LiteralInteger.litInt
import org.partiql.ast.literal.LiteralString.litString
import kotlin.test.assertEquals

class PartiQLParserBagOpTests {

    private val parser = PartiQLParserDefault()

    private fun queryBody(body: () -> Expr) = query(body())

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
                                                name = exprLit(litString("a")),
                                                value = exprLit(litInt(i))
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

    private fun createLit(i: Int) = exprLit(litInt(i))

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
        val parseResult = parser.parse(input)
        assertEquals(1, parseResult.statements.size)
        val actual = parseResult.statements[0]
        assertEquals(expected, actual)
    }
}
