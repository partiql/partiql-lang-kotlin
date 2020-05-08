package org.partiql.lang.syntax

import org.junit.Test
import org.partiql.lang.domains.partiql_ast
import org.partiql.lang.domains.id

class SqlParserJoinTest : SqlParserTestBase() {
    private val projectX = partiql_ast.build { project_list(project_expr(id("x"))) }

    private fun partiql_ast.builder.selectWithOneJoin(
        joinType: partiql_ast.join_type,
        joinPredicate: partiql_ast.expr?,
        wherePredicate: partiql_ast.expr? = null
    ): partiql_ast.expr =
        select(
            project = projectX,
            from = join(
                joinType,
                scan(id("stuff"), "s"),
                scan(id("foo"), "f"),
                joinPredicate),
            where = wherePredicate)


    private fun partiql_ast.builder.selectWithFromSource(fromSource: partiql_ast.from_source): partiql_ast.expr =
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
        """,
        skipPig = false
    ) {
        selectWithOneJoin(
            joinType = partiql_ast.join_type.right(),
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
        """,
        skipPig = false
    ) {
        selectWithOneJoin(
            joinType = full(),
            joinPredicate = eq(id("s"), id("f")))
    }

    private val deeplyNestedJoins = partiql_ast.build {
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
        """,
        skipPig = false
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
        """,
        skipPig = false
    ) {
        selectWithFromSource(deeplyNestedJoins)
    }
}
