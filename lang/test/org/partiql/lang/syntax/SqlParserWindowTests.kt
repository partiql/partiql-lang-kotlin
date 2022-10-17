package org.partiql.lang.syntax

import org.junit.Test

class SqlParserWindowTests : SqlParserTestBase() {

    @Test
    fun lagWithInlinePartitionBYOrderBy() = assertExpression(
        "SELECT LAG(a) OVER (PARTITION BY b ORDER BY c) FROM d",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                (id bar (case_insensitive) (unqualified))
                (on_conflict
                    (id a (case_insensitive) (unqualified))
                    (do_nothing)
                )
              )
            ))
          )
        """,
        targetParsers = setOf(ParserTypes.PARTIQL_PARSER)
    )
}
