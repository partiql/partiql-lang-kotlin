package org.partiql.lang.syntax

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import org.junit.Test
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.id

class SqlParserJoinTest : SqlParserTestBase() {
    private val projectX = PartiqlAst.build { projectList(projectExpr(id("x"))) }

    private fun PartiqlAst.Builder.selectWithOneJoin(
        joinType: PartiqlAst.JoinType,
        joinPredicate: PartiqlAst.Expr?,
        wherePredicate: PartiqlAst.Expr? = null
    ): PartiqlAst.Expr =
        select(
            project = projectX,
            from = join(
                joinType,
                scan(id("stuff"), "s"),
                scan(id("foo"), "f"),
                joinPredicate),
            where = wherePredicate)


    private fun PartiqlAst.Builder.selectWithFromSource(fromSource: PartiqlAst.FromSource): PartiqlAst.Expr =
        select(project = projectX, from = fromSource)

    @Test
    fun selectRightJoin() = assertExpression(
        "SELECT x FROM stuff s RIGHT CROSS JOIN foo f",
        """(select
         (project (list (id x case_insensitive)))
         (from
           (right_join
             (as s (id stuff case_insensitive))
             (as f (id foo case_insensitive)))))
        """
    ) {
        selectWithOneJoin(
            joinType = PartiqlAst.JoinType.Right(),
            joinPredicate = null)
    }

    @Test
    fun selectFullOuterJoinOn() = assertExpression(
        "SELECT x FROM stuff s FULL OUTER JOIN foo f ON s = f",
        """(select
             (project (list (id x case_insensitive)))
             (from
               (outer_join
                 (as s (id stuff case_insensitive))
                 (as f (id foo case_insensitive))
                  (= (id s case_insensitive) (id f case_insensitive)))))
        """
    ) {
        selectWithOneJoin(
            joinType = full(),
            joinPredicate = eq(id("s"), id("f")))
    }

    @Test
    fun selectSingleJoinParensTest() = assertExpression(
        "SELECT x FROM (A INNER JOIN B ON A = B)",
        """(select
            (project
                (list
                    (id x case_insensitive)))
            (from
                (inner_join
                    (id A case_insensitive)
                    (id B case_insensitive)
                    (=
                        (id A case_insensitive)
                        (id B case_insensitive)))))
        """
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                scan(id("A")),
                scan(id("B")),
                eq(id("A"), id("B"))),
            where = null)
    }

    @Test
    fun selectSingleJoinMultiParensTest() = assertExpression(
        "SELECT x FROM (((A INNER JOIN B ON A = B)))",
        """(select
            (project
                (list
                    (id x case_insensitive)))
            (from
                (inner_join
                    (id A case_insensitive)
                    (id B case_insensitive)
                    (=
                        (id A case_insensitive)
                        (id B case_insensitive)))))
        """
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                scan(id("A")),
                scan(id("B")),
                eq(id("A"), id("B"))),
            where = null)
    }

    @Test
    fun selectTwoJoinsNaturalOrderParensTest() = assertExpression(
        "SELECT x FROM (A INNER JOIN B ON A = B) INNER JOIN C ON B = C",
        """(select
            (project
                (list
                    (id x case_insensitive)))
            (from
                (inner_join
                    (inner_join
                        (id A case_insensitive)
                        (id B case_insensitive)
                        (=
                            (id A case_insensitive)
                            (id B case_insensitive)))
                    (id C case_insensitive)
                    (=
                        (id B case_insensitive)
                        (id C case_insensitive)))))
        """
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                join(inner(),
                    scan(id("A")),
                    scan(id("B")),
                    eq(id("A"), id("B"))),
                scan(id("C")),
                eq(id("B"), id("C"))),
            where = null)
    }

    @Test
    fun selectTwoJoinsSpecifiedOrderParensTest() = assertExpression(
        "SELECT x FROM A INNER JOIN (B INNER JOIN C ON B = C) ON A = B",
        """(select
            (project
                (list
                    (id x case_insensitive)))
            (from
                (inner_join
                    (inner_join
                        (id B case_insensitive)
                        (id C case_insensitive)
                        (=
                            (id B case_insensitive)
                            (id C case_insensitive)))
                    (id A case_insensitive)
                    (=
                        (id A case_insensitive)
                        (id B case_insensitive)))))
        """
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                join(inner(),
                    scan(id("B")),
                    scan(id("C")),
                    eq(id("B"), id("C"))),
                scan(id("A")),
                eq(id("A"), id("B"))),
            where = null)
    }

    @Test
    fun selectThreeJoinsSpecifiedOrderParensTest() = assertExpression(
        "SELECT x FROM A INNER JOIN (B INNER JOIN (C INNER JOIN D ON C = D) ON B = C) ON A = B",
        """(select
            (project
                (list
                    (id x case_insensitive)))
            (from
                (inner_join
                    (inner_join
                        (inner_join
                            (id C case_insensitive)
                            (id D case_insensitive)
                            (=
                                (id C case_insensitive)
                                (id D case_insensitive)))
                        (id B case_insensitive)
                        (=
                            (id B case_insensitive)
                            (id C case_insensitive)))
                    (id A case_insensitive)
                    (=
                        (id A case_insensitive)
                        (id B case_insensitive)))))
        """
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                join(inner(),
                    join(inner(),
                        scan(id("C")),
                        scan(id("D")),
                        eq(id("C"), id("D"))),
                    scan(id("B")),
                    eq(id("B"), id("C"))),
                scan(id("A")),
                eq(id("A"), id("B"))),
            where = null)
    }

    @Test
    fun selectLiteralWrappedInParensTest() = assertExpression(
        "SELECT x FROM A INNER JOIN (1) ON true",
        """(select
            (project
                (list
                    (id x case_insensitive)))
            (from
                (inner_join
                    (id A case_insensitive)
                    (lit 1)
                    (lit true))))
        """
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                scan(id("A")),
                scan(lit(ionInt(1))),
                lit(ionBool(true))),
            where = null)
    }

    @Test
    fun selectSubqueryWrappedInParensTest() = assertExpression(
        "SELECT x FROM A INNER JOIN (SELECT x FROM 1) ON true",
        """(select
            (project
                (list
                    (id x case_insensitive)))
            (from
                (inner_join
                    (id A case_insensitive)
                    (select
                        (project
                            (list
                                (id x case_insensitive)))
                        (from
                            (lit 1)))
                    (lit true))))
        """
    ) {
        select(
            project = projectX,
            from = join(
                inner(),
                scan(id("A")),
                scan(
                    select(
                        project = projectX,
                        from = scan(lit(ionInt(1))),
                        where = null
                    )
                ),
                lit(ionBool(true))),
            where = null)
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
                            scan(id("a")),
                            scan(id("b")),
                            null),
                        scan(id("c")),
                        null),
                    scan(id("d")),
                    id("e")),
                scan(id("f")),
                null),
            scan(id("g")),
            id("h"))
    }

    @Test
    fun selectJoins() = assertExpression(
        "SELECT x FROM a, b CROSS JOIN c LEFT JOIN d ON e RIGHT OUTER CROSS JOIN f OUTER JOIN g ON h",
        """(select
             (project (list (id x case_insensitive)))
             (from
               (outer_join
                 (right_join
                   (left_join
                     (inner_join
                       (inner_join
                         (id a case_insensitive)
                         (id b case_insensitive))
                       (id c case_insensitive))
                     (id d case_insensitive)
                     (id e case_insensitive))
                   (id f case_insensitive))
                 (id g case_insensitive)
                 (id h case_insensitive))))
        """
    ) {
        selectWithFromSource(deeplyNestedJoins)
    }

    @Test
    fun selectJoins2() = assertExpression(
        "SELECT x FROM a INNER CROSS JOIN b CROSS JOIN c LEFT JOIN d ON e RIGHT OUTER CROSS JOIN f OUTER JOIN g ON h",
        """(select
             (project (list (id x case_insensitive)))
             (from
               (outer_join
                 (right_join
                   (left_join
                     (inner_join
                       (inner_join
                         (id a case_insensitive)
                         (id b case_insensitive))
                       (id c case_insensitive))
                     (id d case_insensitive)
                     (id e case_insensitive))
                   (id f case_insensitive))
                 (id g case_insensitive)
                 (id h case_insensitive))))
        """
    ) {
        selectWithFromSource(deeplyNestedJoins)
    }
}
