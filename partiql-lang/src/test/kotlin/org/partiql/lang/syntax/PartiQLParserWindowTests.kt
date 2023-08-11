package org.partiql.lang.syntax

import org.junit.Test
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.util.getAntlrDisplayString
import org.partiql.parser.antlr.PartiQLParser

class PartiQLParserWindowTests : PartiQLParserTestBase() {

    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT, ParserTarget.EXPERIMENTAL)

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
        """
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
        """
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
        """
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
        """
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
        """
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
        """
    )

    @Test
    fun lagWithoutOrderBy() {
        checkInputThrowingParserException(
            "SELECT lag(a) OVER () FROM b",
            ErrorCode.PARSE_EXPECTED_WINDOW_ORDER_BY,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LAG.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("lag")
            ),
            assertContext = false,
        )
    }

    @Test
    fun lagWrongNumberOfParameter() {
        checkInputThrowingParserException(
            "SELECT lag(a,b,c,d) OVER (ORDER BY e) FROM f",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 17L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            )
        )
    }

    @Test
    fun leadWithoutOrderBy() {
        checkInputThrowingParserException(
            "SELECT lead(a) OVER () FROM b",
            ErrorCode.PARSE_EXPECTED_WINDOW_ORDER_BY,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LAG.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("lag")
            ),
            assertContext = false,
        )
    }

    @Test
    fun leadWrongNumberOfParameter() {
        checkInputThrowingParserException(
            "SELECT lead(a,b,c,d) OVER (ORDER BY e) FROM f",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            )
        )
    }
}
