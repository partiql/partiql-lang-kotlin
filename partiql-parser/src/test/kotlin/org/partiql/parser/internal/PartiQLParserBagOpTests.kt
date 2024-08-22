package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.SetOp
import org.partiql.ast.SetQuantifier
import org.partiql.ast.builder.AstBuilder
import org.partiql.ast.builder.ast
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import kotlin.test.assertEquals

class PartiQLParserBagOpTests {

    private val parser = PartiQLParserDefault()

    private fun query(block: AstBuilder.() -> Expr) = ast { statementQuery { expr = block() } }

    @OptIn(PartiQLValueExperimental::class)
    private fun createSFW(i: Int): Expr.QuerySet =
        ast {
            exprQuerySet {
                body = queryBodySFW {
                    select = selectStar()
                    from = fromValue {
                        expr = exprCollection {
                            type = Expr.Collection.Type.BAG
                            values = mutableListOf(
                                exprStruct {
                                    fields = mutableListOf(
                                        exprStructField {
                                            name = exprLit { value = stringValue("a") }
                                            value = exprLit { value = int32Value(i) }
                                        }
                                    )
                                }
                            )
                        }
                        type = From.Value.Type.SCAN
                    }
                }
            }
        }

    @OptIn(PartiQLValueExperimental::class)
    private fun createLit(i: Int) = ast { exprLit { value = int32Value(i) } }

    // SQL Union
    @Test
    fun sqlUnion() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION SELECT * FROM <<{'a': 2}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.UNION
                    }
                    lhs = createSFW(1)
                    rhs = createSFW(2)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun sqlUnionMultiple() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION ALL SELECT * FROM <<{'a': 2}>> UNION DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.UNION
                        setq = SetQuantifier.DISTINCT
                    }
                    lhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.UNION
                                setq = SetQuantifier.ALL
                            }
                            lhs = createSFW(1)
                            rhs = createSFW(2)
                            isOuter = false
                        }
                    }
                    rhs = createSFW(3)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun sqlUnionMultipleRight() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION ALL (SELECT * FROM <<{'a': 2}>> UNION DISTINCT SELECT * FROM <<{'a': 3}>>)",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.UNION
                        setq = SetQuantifier.ALL
                    }
                    lhs = createSFW(1)
                    rhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.UNION
                                setq = SetQuantifier.DISTINCT
                            }
                            lhs = createSFW(2)
                            rhs = createSFW(3)
                            isOuter = false
                        }
                        isOuter = false
                    }
                }
            }
        }
    )

    // Outer Union
    @Test
    fun outerUnion() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> OUTER UNION SELECT * FROM <<{'a': 2}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.UNION
                    }
                    lhs = createSFW(1)
                    rhs = createSFW(2)
                    isOuter = true
                }
            }
        }
    )

    @Test
    fun outerUnionNonSpecified() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION 2",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.UNION
                    }
                    lhs = createSFW(1)
                    rhs = createLit(2)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun sqlUnionAndOuterUnion() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> UNION ALL SELECT * FROM <<{'a': 2}>> UNION DISTINCT 3",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.UNION
                        setq = SetQuantifier.DISTINCT
                    }
                    lhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.UNION
                                setq = SetQuantifier.ALL
                            }
                            lhs = createSFW(1)
                            rhs = createSFW(2)
                            isOuter = false
                        }
                    }
                    rhs = createLit(3)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun outerUnionAndSQLUnion() = assertExpression(
        "1 UNION ALL SELECT * FROM <<{'a': 2}>> UNION DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.UNION
                        setq = SetQuantifier.DISTINCT
                    }
                    lhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.UNION
                                setq = SetQuantifier.ALL
                            }
                            lhs = createLit(1)
                            rhs = createSFW(2)
                            isOuter = false
                        }
                    }
                    rhs = createSFW(3)
                    isOuter = false
                }
            }
        }
    )

    // SQL Except
    @Test
    fun sqlExcept() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT SELECT * FROM <<{'a': 2}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.EXCEPT
                    }
                    lhs = createSFW(1)
                    rhs = createSFW(2)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun sqlExceptMultiple() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT ALL SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.EXCEPT
                        setq = SetQuantifier.DISTINCT
                    }
                    lhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.EXCEPT
                                setq = SetQuantifier.ALL
                            }
                            lhs = createSFW(1)
                            rhs = createSFW(2)
                            isOuter = false
                        }
                    }
                    rhs = createSFW(3)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun sqlExceptMultipleRight() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT ALL (SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT SELECT * FROM <<{'a': 3}>>)",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.EXCEPT
                        setq = SetQuantifier.ALL
                    }
                    lhs = createSFW(1)
                    rhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.EXCEPT
                                setq = SetQuantifier.DISTINCT
                            }
                            lhs = createSFW(2)
                            rhs = createSFW(3)
                            isOuter = false
                        }
                    }
                    isOuter = false
                }
            }
        }
    )

    // Outer Except
    @Test
    fun outerExcept() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> OUTER EXCEPT SELECT * FROM <<{'a': 2}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.EXCEPT
                    }
                    lhs = createSFW(1)
                    rhs = createSFW(2)
                    isOuter = true
                }
            }
        }
    )

    @Test
    fun outerExceptNonSpecified() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT 2",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.EXCEPT
                    }
                    lhs = createSFW(1)
                    rhs = createLit(2)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun sqlExceptAndOuterExcept() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> EXCEPT ALL SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT 3",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.EXCEPT
                        setq = SetQuantifier.DISTINCT
                    }
                    lhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.EXCEPT
                                setq = SetQuantifier.ALL
                            }
                            lhs = createSFW(1)
                            rhs = createSFW(2)
                            isOuter = false
                        }
                    }
                    rhs = createLit(3)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun outerExceptAndSQLExcept() = assertExpression(
        "1 EXCEPT ALL SELECT * FROM <<{'a': 2}>> EXCEPT DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.EXCEPT
                        setq = SetQuantifier.DISTINCT
                    }
                    lhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.EXCEPT
                                setq = SetQuantifier.ALL
                            }
                            lhs = createLit(1)
                            rhs = createSFW(2)
                            isOuter = false
                        }
                    }
                    rhs = createSFW(3)
                    isOuter = false
                }
            }
        }
    )

    // SQL Intersect
    @Test
    fun sqlIntersect() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT SELECT * FROM <<{'a': 2}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.INTERSECT
                    }
                    lhs = createSFW(1)
                    rhs = createSFW(2)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun sqlIntersectMultiple() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT ALL SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.INTERSECT
                        setq = SetQuantifier.DISTINCT
                    }
                    lhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.INTERSECT
                                setq = SetQuantifier.ALL
                            }
                            lhs = createSFW(1)
                            rhs = createSFW(2)
                            isOuter = false
                        }
                    }
                    rhs = createSFW(3)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun sqlIntersectMultipleRight() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT ALL (SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT SELECT * FROM <<{'a': 3}>>)",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.INTERSECT
                        setq = SetQuantifier.ALL
                    }
                    lhs = createSFW(1)
                    rhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.INTERSECT
                                setq = SetQuantifier.DISTINCT
                            }
                            lhs = createSFW(2)
                            rhs = createSFW(3)
                            isOuter = false
                        }
                    }
                    isOuter = false
                }
            }
        }
    )

    // Outer Intersect
    @Test
    fun outerIntersect() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> OUTER INTERSECT SELECT * FROM <<{'a': 2}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.INTERSECT
                    }
                    lhs = createSFW(1)
                    rhs = createSFW(2)
                    isOuter = true
                }
            }
        }
    )

    @Test
    fun outerIntersectNonSpecified() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT 2",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.INTERSECT
                    }
                    lhs = createSFW(1)
                    rhs = createLit(2)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun sqlIntersectAndOuterIntersect() = assertExpression(
        "SELECT * FROM <<{'a': 1}>> INTERSECT ALL SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT 3",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.INTERSECT
                        setq = SetQuantifier.DISTINCT
                    }
                    lhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.INTERSECT
                                setq = SetQuantifier.ALL
                            }
                            lhs = createSFW(1)
                            rhs = createSFW(2)
                            isOuter = false
                        }
                    }
                    rhs = createLit(3)
                    isOuter = false
                }
            }
        }
    )

    @Test
    fun outerIntersectAndSQLIntersect() = assertExpression(
        "1 INTERSECT ALL SELECT * FROM <<{'a': 2}>> INTERSECT DISTINCT SELECT * FROM <<{'a': 3}>>",
        query {
            exprQuerySet {
                body = queryBodySetOp {
                    type = setOp {
                        type = SetOp.Type.INTERSECT
                        setq = SetQuantifier.DISTINCT
                    }
                    lhs = exprQuerySet {
                        body = queryBodySetOp {
                            type = setOp {
                                type = SetOp.Type.INTERSECT
                                setq = SetQuantifier.ALL
                            }
                            lhs = createLit(1)
                            rhs = createSFW(2)
                            isOuter = false
                        }
                    }
                    rhs = createSFW(3)
                    isOuter = false
                }
            }
        }
    )

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        val actual = result.root
        assertEquals(expected, actual)
    }
}
