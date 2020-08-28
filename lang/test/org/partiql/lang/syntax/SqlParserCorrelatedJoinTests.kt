package org.partiql.lang.syntax

import org.junit.Test
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.id

class SqlParserCorrelatedJoinTests : SqlParserTestBase() {
    private fun PartiqlAst.Builder.callFWithS() =
        call("f", id("s", caseInsensitive(), unqualified()))

    private fun PartiqlAst.Builder.selectWithCorrelatedJoin(
        joinType: PartiqlAst.JoinType,
        joinPredicate: PartiqlAst.Expr?,
        wherePredicate: PartiqlAst.Expr? = null
    ): PartiqlAst.Expr =
            select(
                project = projectList(
                    projectExpr(id("a")),
                    projectExpr(id("b"))),
                from = join(
                    joinType,
                    scan(id("stuff"), "s"),
                    scan(id("s", caseInsensitive(), localsFirst())),
                    joinPredicate),
                where = wherePredicate)

    @Test
    fun selectCorrelatedExplicitCrossJoin() = assertExpression(
        "SELECT a, b FROM stuff s CROSS JOIN @s WHERE f(s)",
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """
    ) {
        selectWithCorrelatedJoin(
            joinType = PartiqlAst.JoinType.Inner(),
            joinPredicate = null,
            wherePredicate = callFWithS())
    }

    @Test
    fun selectCorrelatedExplicitLeftJoin() = assertExpression(
        "SELECT a, b FROM stuff s LEFT CROSS JOIN @s WHERE f(s)",
        """(select
            (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (left_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """
    ) {
        selectWithCorrelatedJoin(
            joinType = PartiqlAst.JoinType.Left(),
            joinPredicate = null,
            wherePredicate = callFWithS())
    }

    @Test
    fun selectCorrelatedLeftOuterJoinOn() = assertExpression(
        "SELECT a, b FROM stuff s LEFT JOIN @s ON f(s)",
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from
               (left_join
                 (as s (id stuff case_insensitive))
                 (@ (id s case_insensitive))
                 (call f (id s case_insensitive))
               )
             )
           )
        """
    ) {
        selectWithCorrelatedJoin(
            joinType = PartiqlAst.JoinType.Left(),
            joinPredicate = callFWithS())
    }


    @Test
    fun selectCorrelatedJoin() = assertExpression(
        "SELECT a, b FROM stuff s, @s WHERE f(s)",
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """
    ) {
        selectWithCorrelatedJoin(
            joinType = PartiqlAst.JoinType.Inner(),
            joinPredicate = null,
            wherePredicate = callFWithS())
    }

    @Test
    fun selectCorrelatedExplicitInnerJoin() = assertExpression(
        "SELECT a, b FROM stuff s INNER CROSS JOIN @s WHERE f(s)",
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """
    ) {
        selectWithCorrelatedJoin(
            joinType = PartiqlAst.JoinType.Inner(),
            joinPredicate = null,
            wherePredicate = callFWithS())
    }
}