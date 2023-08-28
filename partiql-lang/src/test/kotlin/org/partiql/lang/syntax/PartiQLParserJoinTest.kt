package org.partiql.lang.syntax

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import org.junit.Test
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.vr

class PartiQLParserJoinTest : PartiQLParserTestBase() {

    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT, ParserTarget.EXPERIMENTAL)

    private val projectX = PartiqlAst.build { projectList(projectExpr(vr("x"))) }

    private fun PartiqlAst.Builder.selectWithOneJoin(
        joinType: PartiqlAst.JoinType,
        joinPredicate: PartiqlAst.Expr?,
        wherePredicate: PartiqlAst.Expr? = null
    ): PartiqlAst.Expr =
        select(
            project = projectX,
            from = join(
                joinType,
                scan(vr("stuff"), defnid("s")),
                scan(vr("foo"), defnid("f")),
                joinPredicate
            ),
            where = wherePredicate
        )

    private fun PartiqlAst.Builder.selectWithFromSource(fromSource: PartiqlAst.FromSource): PartiqlAst.Expr =
        select(project = projectX, from = fromSource)

    @Test
    fun selectRightJoin() = assertExpression(
        "SELECT x FROM stuff s RIGHT CROSS JOIN foo f"
    ) {
        selectWithOneJoin(
            joinType = PartiqlAst.JoinType.Right(),
            joinPredicate = null
        )
    }

    @Test
    fun selectFullOuterJoinOn() = assertExpression(
        "SELECT x FROM stuff s FULL OUTER JOIN foo f ON s = f"
    ) {
        selectWithOneJoin(
            joinType = full(),
            joinPredicate = eq(vr("s"), vr("f"))
        )
    }

    @Test
    fun selectSingleJoinParensTest() = assertExpression(
        "SELECT x FROM (A INNER JOIN B ON A = B)"
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                scan(vr("A")),
                scan(vr("B")),
                eq(vr("A"), vr("B"))
            ),
            where = null
        )
    }

    @Test
    fun selectSingleJoinMultiParensTest() = assertExpression(
        "SELECT x FROM (((A INNER JOIN B ON A = B)))"
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                scan(vr("A")),
                scan(vr("B")),
                eq(vr("A"), vr("B"))
            ),
            where = null
        )
    }

    @Test
    fun selectTwoJoinsNaturalOrderParensTest() = assertExpression(
        "SELECT x FROM (A INNER JOIN B ON A = B) INNER JOIN C ON B = C"
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                join(
                    inner(),
                    scan(vr("A")),
                    scan(vr("B")),
                    eq(vr("A"), vr("B"))
                ),
                scan(vr("C")),
                eq(vr("B"), vr("C"))
            ),
            where = null
        )
    }

    @Test
    fun selectTwoJoinsSpecifiedOrderParensTest() = assertExpression(
        "SELECT x FROM A INNER JOIN (B INNER JOIN C ON B = C) ON A = B"
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                scan(vr("A")),
                join(
                    inner(),
                    scan(vr("B")),
                    scan(vr("C")),
                    eq(vr("B"), vr("C"))
                ),
                eq(vr("A"), vr("B"))
            ),
            where = null
        )
    }

    @Test
    fun selectThreeJoinsSpecifiedOrderParensTest() = assertExpression(
        "SELECT x FROM A INNER JOIN (B INNER JOIN (C INNER JOIN D ON C = D) ON B = C) ON A = B"
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                scan(vr("A")),
                join(
                    inner(),
                    scan(vr("B")),
                    join(
                        inner(),
                        scan(vr("C")),
                        scan(vr("D")),
                        eq(vr("C"), vr("D"))
                    ),
                    eq(vr("B"), vr("C"))
                ),
                eq(vr("A"), vr("B"))
            ),
            where = null
        )
    }

    @Test
    fun selectLiteralWrappedInParensTest() = assertExpression(
        "SELECT x FROM A INNER JOIN (1) ON true"
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                scan(vr("A")),
                scan(lit(ionInt(1))),
                lit(ionBool(true))
            ),
            where = null
        )
    }

    @Test
    fun selectSubqueryWrappedInParensTest() = assertExpression(
        "SELECT x FROM A INNER JOIN (SELECT x FROM 1) ON true"
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                scan(vr("A")),
                scan(
                    select(
                        project = projectX,
                        from = scan(lit(ionInt(1))),
                        where = null
                    )
                ),
                lit(ionBool(true))
            ),
            where = null
        )
    }

    private val deeplyNestedJoins = PartiqlAst.build {
        join(
            full(),
            join(
                right(),
                join(
                    left(),
                    join(
                        inner(),
                        join(
                            inner(),
                            scan(vr("a")),
                            scan(vr("b")),
                            null
                        ),
                        scan(vr("c")),
                        null
                    ),
                    scan(vr("d")),
                    vr("e")
                ),
                scan(vr("f")),
                null
            ),
            scan(vr("g")),
            vr("h")
        )
    }

    @Test
    fun selectJoins() = assertExpression(
        "SELECT x FROM a, b CROSS JOIN c LEFT JOIN d ON e RIGHT OUTER CROSS JOIN f OUTER JOIN g ON h"
    ) {
        selectWithFromSource(deeplyNestedJoins)
    }

    @Test
    fun selectJoins2() = assertExpression(
        "SELECT x FROM a INNER CROSS JOIN b CROSS JOIN c LEFT JOIN d ON e RIGHT OUTER CROSS JOIN f OUTER JOIN g ON h"
    ) {
        selectWithFromSource(deeplyNestedJoins)
    }
}
