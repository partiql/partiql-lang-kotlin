package org.partiql.lang.syntax

import org.junit.Test

class SqlParserWindowTests : SqlParserTestBase() {
    // TODO: In the future when we support custom-defined window frame, we will need to change this
    @Test
    fun lagWithInlinePartitionBYOrderBy() = assertExpression(
        "SELECT LAG(a) OVER (PARTITION BY b ORDER BY c) FROM d",
        """
        (select
            (project
                (project_list
                    (project_expr
                        (call_window
                            lag
                            (over
                                (window_partition_list
                                    (id b (case_insensitive) (unqualified))
                                )
                                (window_sort_spec_list
                                    (sort_spec
                                        (id c (case_insensitive) (unqualified))
                                        null
                                        null
                                    )
                                )
                            )
                            (id a (case_insensitive) (unqualified))
                        )
                        null
                    )
                )
            )
            (from
                (scan
                    (id d (case_insensitive) (unqualified))
                    null
                    null
                    null)))
        """,
        targetParsers = setOf(ParserTypes.PARTIQL_PARSER),
        roundTrip = false
    )

    @Test
    fun leadWithInlinePartitionBYOrderBy() = assertExpression(
        "SELECT LEAD(a) OVER (PARTITION BY b ORDER BY c) FROM d",
        """
        (select
            (project
                (project_list
                    (project_expr
                        (call_window
                            lead
                            (over
                                (window_partition_list
                                    (id b (case_insensitive) (unqualified))
                                )
                                (window_sort_spec_list
                                    (sort_spec
                                        (id c (case_insensitive) (unqualified))
                                        null
                                        null
                                    )
                                )
                            )
                            (id a (case_insensitive) (unqualified))
                        )
                        null
                    )
                )
            )
            (from
                (scan
                    (id d (case_insensitive) (unqualified))
                    null
                    null
                    null)))
        """,
        targetParsers = setOf(ParserTypes.PARTIQL_PARSER),
        roundTrip = false
    )

    @Test
    fun lagWithOffsetAndDefault() = assertExpression(
        "SELECT LAG(a,2,null) OVER (PARTITION BY b ORDER BY c) FROM d",
        """
        (select
            (project
                (project_list
                    (project_expr
                        (call_window
                            lag
                            (over
                                (window_partition_list
                                    (id b (case_insensitive) (unqualified))
                                )
                                (window_sort_spec_list
                                    (sort_spec
                                        (id c (case_insensitive) (unqualified))
                                        null
                                        null
                                    )
                                )
                            )
                            (id a (case_insensitive) (unqualified))
                            (lit 2)
                            (lit null)
                        )
                        null
                    )
                )
            )
            (from
                (scan
                    (id d (case_insensitive) (unqualified))
                    null
                    null
                    null)))
        """,
        targetParsers = setOf(ParserTypes.PARTIQL_PARSER),
        roundTrip = false
    )

    @Test
    fun leadWithOffSet() = assertExpression(
        "SELECT LEAD(a,2) OVER (PARTITION BY b ORDER BY c) FROM d",
        """
        (select
            (project
                (project_list
                    (project_expr
                        (call_window
                            lead
                            (over
                                (window_partition_list
                                    (id b (case_insensitive) (unqualified))
                                )
                                (window_sort_spec_list
                                    (sort_spec
                                        (id c (case_insensitive) (unqualified))
                                        null
                                        null
                                    )
                                )
                            )
                            (id a (case_insensitive) (unqualified))
                            (lit 2)
                        )
                        null
                    )
                )
            )
            (from
                (scan
                    (id d (case_insensitive) (unqualified))
                    null
                    null
                    null)))
        """,
        targetParsers = setOf(ParserTypes.PARTIQL_PARSER),
        roundTrip = false
    )

    @Test
    fun lagWithOffset() = assertExpression(
        "SELECT LAG(a,2) OVER (PARTITION BY b ORDER BY c) FROM d",
        """
        (select
            (project
                (project_list
                    (project_expr
                        (call_window
                            lag
                            (over
                                (window_partition_list
                                    (id b (case_insensitive) (unqualified))
                                )
                                (window_sort_spec_list
                                    (sort_spec
                                        (id c (case_insensitive) (unqualified))
                                        null
                                        null
                                    )
                                )
                            )
                            (id a (case_insensitive) (unqualified))
                            (lit 2)
                        )
                        null
                    )
                )
            )
            (from
                (scan
                    (id d (case_insensitive) (unqualified))
                    null
                    null
                    null)))
        """,
        targetParsers = setOf(ParserTypes.PARTIQL_PARSER),
        roundTrip = false
    )

    @Test
    fun leadWithOffSetAndDefault() = assertExpression(
        "SELECT LEAD(a,2,null) OVER (PARTITION BY b ORDER BY c) FROM d",
        """
        (select
            (project
                (project_list
                    (project_expr
                        (call_window
                            lead
                            (over
                                (window_partition_list
                                    (id b (case_insensitive) (unqualified))
                                )
                                (window_sort_spec_list
                                    (sort_spec
                                        (id c (case_insensitive) (unqualified))
                                        null
                                        null
                                    )
                                )
                            )
                            (id a (case_insensitive) (unqualified))
                            (lit 2)
                            (lit null)
                        )
                        null
                    )
                )
            )
            (from
                (scan
                    (id d (case_insensitive) (unqualified))
                    null
                    null
                    null)))
        """,
        targetParsers = setOf(ParserTypes.PARTIQL_PARSER),
        roundTrip = false
    )
}
