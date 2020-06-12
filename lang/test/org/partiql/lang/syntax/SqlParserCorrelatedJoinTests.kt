package org.partiql.lang.syntax

import org.junit.Test
import org.partiql.lang.domains.id
import org.partiql.lang.domains.partiql_ast

class SqlParserCorrelatedJoinTests : SqlParserTestBase() {
    private fun partiql_ast.builder.callFWithS() =
        call("f", id("s", case_insensitive(), unqualified()))

    private fun partiql_ast.builder.selectWithCorrelatedJoin(
        joinType: partiql_ast.join_type,
        joinPredicate: partiql_ast.expr?,
        wherePredicate: partiql_ast.expr? = null
    ): partiql_ast.expr =
        select(
            project = project_list(
                project_expr(id("a")),
                project_expr(id("b"))),
            from = join(
                joinType,
                scan(id("stuff"), "s"),
                scan(id("s", case_insensitive(), locals_first())),
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
        """,
        skipPig = false
    ) {
        selectWithCorrelatedJoin(
            joinType = partiql_ast.join_type.inner(),
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
        """,
        skipPig = false
    ) {
        selectWithCorrelatedJoin(
            joinType = partiql_ast.join_type.left(),
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
        """,
        skipPig = false
    ) {
        selectWithCorrelatedJoin(
            joinType = partiql_ast.join_type.left(),
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
        """,
        skipPig = false
    ) {
        selectWithCorrelatedJoin(
            joinType = partiql_ast.join_type.inner(),
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
        """,
        skipPig = false
    ) {
        selectWithCorrelatedJoin(
            joinType = partiql_ast.join_type.inner(),
            joinPredicate = null,
            wherePredicate = callFWithS())
    }
}