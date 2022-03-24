package org.partiql.lang.prettyprint

import com.amazon.ion.system.IonSystemBuilder
import org.junit.Assert
import org.junit.Test
import org.partiql.lang.syntax.SqlParser

class QueryPrettyPrinterTest {
    private val prettyPrinter = QueryPrettyPrinter()
    private val sqlParser = SqlParser(IonSystemBuilder.standard().build())

    private fun checkPrettyPrintQuery(query: String, expected: String) {
        // In triples quotes, a tab consists of 4 whitespaces. We need to transform them into a tab.
        val newExpected = expected.replace("    ", "\t")

        println(newExpected)

        println(prettyPrinter.prettyPrintQuery(query))

        Assert.assertEquals(newExpected, prettyPrinter.prettyPrintQuery(query))
        // New sting and old string should be the same when transformed into PIG AST
        Assert.assertEquals(sqlParser.parseAstStatement(query), sqlParser.parseAstStatement(newExpected))
    }

    // ********
    // * EXEC *
    // ********

    // *******
    // * DDL *
    // *******

    // *******
    // * Dml *
    // *******

    // *********
    // * Query *
    // *********
    @Test
    fun id1() {
        checkPrettyPrintQuery("a", "a")
    }

    @Test
    fun id2() {
        checkPrettyPrintQuery("\"a\"", "\"a\"")
    }

    @Test
    fun missing() {
        checkPrettyPrintQuery("MISSING", "MISSING")
    }

    @Test
    fun null1() {
        checkPrettyPrintQuery("NULL", "NULL")
    }

    @Test
    fun null2() {
        checkPrettyPrintQuery("`null`", "NULL")
    }

    @Test
    fun litBoolean1() {
        checkPrettyPrintQuery("TRUE", "TRUE")
    }

    @Test
    fun litBoolean2() {
        checkPrettyPrintQuery("`false`", "FALSE")
    }

    @Test
    fun litInt1() {
        checkPrettyPrintQuery("1", "1")
    }

    @Test
    fun litInt2() {
        checkPrettyPrintQuery("`1`", "1")
    }

    @Test
    fun litDecimal() {
        checkPrettyPrintQuery("0.1", "0.1")
    }

    @Test
    fun litString1() {
        checkPrettyPrintQuery("'0.1'", "'0.1'")
    }

    @Test
    fun litString2() {
        checkPrettyPrintQuery("'TRUE'", "'TRUE'")
    }

    @Test
    fun litString3() {
        checkPrettyPrintQuery("'NULL'", "'NULL'")
    }

    @Test
    fun litString4() {
        checkPrettyPrintQuery("`\"0.1\"`", "'0.1'")
    }

    @Test
    fun litString5() {
        checkPrettyPrintQuery("`\"TRUE\"`", "'TRUE'")
    }

    @Test
    fun litString6() {
        checkPrettyPrintQuery("`\"NULL\"`", "'NULL'")
    }

    @Test
    fun litTimestamp() {
        checkPrettyPrintQuery("`2017-01-10T05:30:55Z`", "`2017-01-10T05:30:55Z`")
    }

    @Test
    fun date() {
        checkPrettyPrintQuery("DATE '2022-03-16'", "DATE '2022-03-16'")
    }

    @Test
    fun litTime1() {
        checkPrettyPrintQuery("TIME (9) '23:59:59.123456789'", "TIME (9) '23:59:59.123456789'")
    }

    @Test
    fun litTime2() {
        checkPrettyPrintQuery("TIME (7) WITH TIME ZONE '23:59:59.123456789'", "TIME (7) WITH TIME ZONE '23:59:59.123456789'")
    }

    @Test
    fun litTime3() {
        checkPrettyPrintQuery("TIME (7) WITH TIME ZONE '23:59:59.123456789-01:00'", "TIME (7) WITH TIME ZONE '23:59:59.123456789-01:00'")
    }

    @Test
    fun parameter() {
        checkPrettyPrintQuery("?", "?")
    }

    @Test
    fun struct1() {
        checkPrettyPrintQuery("{ a: 1 }", "{ a: 1 }")
    }

    @Test
    fun struct2() {
        checkPrettyPrintQuery("{a:1,'b':\"c\"}", "{ a: 1, 'b': \"c\" }")
    }

    @Test
    fun bag() {
        checkPrettyPrintQuery("<<1,2,3>>", "<< 1, 2, 3 >>")
    }

    @Test
    fun list() {
        checkPrettyPrintQuery("[1,2,3]", "[ 1, 2, 3 ]")
    }

    @Test
    fun sexp() {
        checkPrettyPrintQuery("sexp(1,2,3)", "sexp(1, 2, 3)")
    }

    @Test
    fun not1() {
        checkPrettyPrintQuery("NOT TRUE", "NOT TRUE")
    }

    @Test
    fun not2() {
        checkPrettyPrintQuery("NOT (TRUE AND FALSE)", "NOT (TRUE AND FALSE)")
    }

    @Test
    fun pos() {
        checkPrettyPrintQuery("+function1()", "+function1()")
    }

    @Test
    fun neg() {
        checkPrettyPrintQuery("-function1()", "-function1()")
    }

    @Test
    fun plus1() {
        checkPrettyPrintQuery("1 + 2", "1 + 2")
    }

    @Test
    fun plus2() {
        checkPrettyPrintQuery("1 + 2 + 3", "(1 + 2) + 3")
    }

    @Test
    fun plus3() {
        checkPrettyPrintQuery("1 + (2 + 3)", "1 + (2 + 3)")
    }

    @Test
    fun minus1() {
        checkPrettyPrintQuery("1 - 2", "1 - 2")
    }

    @Test
    fun minus2() {
        checkPrettyPrintQuery("1 - 2 - 3", "(1 - 2) - 3")
    }

    @Test
    fun minus3() {
        checkPrettyPrintQuery("1 - 2 - 3 - 4", "((1 - 2) - 3) - 4")
    }

    @Test
    fun times() {
        checkPrettyPrintQuery("1 * 2", "1 * 2")
    }

    @Test
    fun divide() {
        checkPrettyPrintQuery("1 / 2", "1 / 2")
    }

    @Test
    fun modulo() {
        checkPrettyPrintQuery("3 % 2", "3 % 2")
    }

    @Test
    fun concat() {
        checkPrettyPrintQuery("1 || 2", "1 || 2")
    }

    @Test
    fun and() {
        checkPrettyPrintQuery("TRUE AND FALSE", "TRUE AND FALSE")
    }

    @Test
    fun or1() {
        checkPrettyPrintQuery("TRUE OR FALSE", "TRUE OR FALSE")
    }

    @Test
    fun or2() {
        checkPrettyPrintQuery("TRUE OR FALSE AND TRUE", "TRUE OR (FALSE AND TRUE)")
    }

    @Test
    fun or3() {
        checkPrettyPrintQuery("(TRUE OR FALSE) AND TRUE", "(TRUE OR FALSE) AND TRUE")
    }

    @Test
    fun eq() {
        checkPrettyPrintQuery("1 = 2 = 3", "(1 = 2) = 3")
    }

    @Test
    fun ne() {
        checkPrettyPrintQuery("1 != 2", "1 != 2")
    }

    @Test
    fun gt() {
        checkPrettyPrintQuery("2 > 1", "2 > 1")
    }

    @Test
    fun gte() {
        checkPrettyPrintQuery("2 >= 1", "2 >= 1")
    }

    @Test
    fun lt() {
        checkPrettyPrintQuery("1 < 2", "1 < 2")
    }

    @Test
    fun lte() {
        checkPrettyPrintQuery("1 <= 2", "1 <= 2")
    }

    @Test
    fun inCollection() {
        checkPrettyPrintQuery("1 IN [1, 2, 3]", "1 IN [ 1, 2, 3 ]")
    }

    @Test
    fun union1() {
        checkPrettyPrintQuery("a UNION b", "a UNION b")
    }

    @Test
    fun union2() {
        checkPrettyPrintQuery("a UNION ALL b", "a UNION ALL b")
    }

    @Test
    fun except() {
        checkPrettyPrintQuery("a EXCEPT b", "a EXCEPT b")
    }

    @Test
    fun intersect() {
        checkPrettyPrintQuery("a INTERSECT b", "a INTERSECT b")
    }

    @Test
    fun like() {
        checkPrettyPrintQuery("a LIKE b ESCAPE c", "a LIKE b ESCAPE c")
    }

    @Test
    fun between1() {
        checkPrettyPrintQuery("a BETWEEN b AND c", "a BETWEEN b AND c")
    }

    @Test
    fun between2() {
        checkPrettyPrintQuery("a NOT BETWEEN b AND c", "NOT (a BETWEEN b AND c)")
    }

    @Test
    fun path1() {
        checkPrettyPrintQuery("a.b", "a.b")
    }

    @Test
    fun path2() {
        checkPrettyPrintQuery("a.\"b\"", "a['b']")
    }

    @Test
    fun path3() {
        checkPrettyPrintQuery("a[b]", "a[b]")
    }

    @Test
    fun path4() {
        checkPrettyPrintQuery("a.b.c", "a.b.c")
    }

    @Test
    fun path5() {
        checkPrettyPrintQuery("(a.b).c", "(a.b).c")
    }

    @Test
    fun path6() {
        checkPrettyPrintQuery("(a.b)[c.d].e", "(a.b)[c.d].e")
    }

    @Test
    fun call1() {
        checkPrettyPrintQuery("function1(1)", "function1(1)")
    }

    @Test
    fun call2() {
        checkPrettyPrintQuery("function1(a)", "function1(a)")
    }

    @Test
    fun callAgg1() {
        checkPrettyPrintQuery("sum(a)", "sum(a)")
    }

    @Test
    fun callAgg2() {
        checkPrettyPrintQuery("sum(DISTINCT a)", "sum(DISTINCT a)")
    }

    @Test
    fun isType() {
        checkPrettyPrintQuery("1 IS INT", "1 IS INT")
    }

    @Test
    fun cast() {
        checkPrettyPrintQuery("CAST (1 AS STRING)", "CAST (1 AS STRING)")
    }

    @Test
    fun canCast() {
        checkPrettyPrintQuery("CAN_CAST (1 AS STRING)", "CAN_CAST (1 AS STRING)")
    }

    @Test
    fun canLosslessCast() {
        checkPrettyPrintQuery("CAN_LOSSLESS_CAST (1 AS STRING)", "CAN_LOSSLESS_CAST (1 AS STRING)")
    }

    @Test
    fun nullIf() {
        checkPrettyPrintQuery("NULLIF(1, 2)", "NULLIF(1, 2)")
    }

    @Test
    fun coalesce() {
        checkPrettyPrintQuery("COALESCE(1, 2)", "COALESCE(1, 2)")
    }

    @Test
    fun simpleCase1() {
        checkPrettyPrintQuery(
            "CASE name WHEN 'jack' THEN 1 WHEN 'joe' THEN 2 ELSE 3 END",
            """
                CASE name
                    WHEN 'jack' THEN 1
                    WHEN 'joe' THEN 2
                    ELSE 3
                END
            """.trimIndent()
        )
    }

    @Test
    fun simpleCase2() {
        checkPrettyPrintQuery(
            "CASE name WHEN a.b + c THEN 1 WHEN 1 + 1 - 2 THEN 2 ELSE 3 END",
            """
                CASE name
                    WHEN a.b + c THEN 1
                    WHEN (1 + 1) - 2 THEN 2
                    ELSE 3
                END
            """.trimIndent()
        )
    }

    @Test
    fun searchedCase() {
        checkPrettyPrintQuery(
            "CASE WHEN name = 'jack' THEN 1 WHEN 1 + 1 = 2 THEN 2 ELSE 3 END",
            """
                CASE
                    WHEN name = 'jack' THEN 1
                    WHEN (1 + 1) = 2 THEN 2
                    ELSE 3
                END
            """.trimIndent()
        )
    }

    // Select
    @Test
    fun selectFrom() {
        checkPrettyPrintQuery(
            "SELECT * FROM 1",
            """
                SELECT *
                FROM 1
            """.trimIndent()
        )
    }

    @Test
    fun selectFromLet() {
        checkPrettyPrintQuery(
            "SELECT * FROM 1 LET 1 AS a",
            """
                SELECT *
                FROM 1 LET 1 AS a
            """.trimIndent()
        )
    }

    @Test
    fun selectFromLetWhere() {
        checkPrettyPrintQuery(
            "SELECT * FROM 1 LET 1 AS a WHERE a = b",
            """
                SELECT *
                FROM 1 LET 1 AS a
                WHERE a = b
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhereGroupHaving() {
        checkPrettyPrintQuery(
            "SELECT * FROM 1 LET 1 AS a WHERE b = c GROUP BY d GROUP AS e HAVING f = '123'",
            """
                SELECT *
                FROM 1 LET 1 AS a
                WHERE b = c
                GROUP BY d GROUP AS e
                HAVING f = '123'
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhereGroupHavingLimitOffset() {
        checkPrettyPrintQuery(
            "SELECT * FROM 1 LET 1 AS a WHERE b = c GROUP BY d GROUP AS e HAVING f = '123' LIMIT 3 OFFSET 4",
            """
                SELECT *
                FROM 1 LET 1 AS a
                WHERE b = c
                GROUP BY d GROUP AS e
                HAVING f = '123'
                LIMIT 3
                OFFSET 4
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhereGroupHavingLimitOffsetWithSubQuery1() {
        checkPrettyPrintQuery(
            "SELECT (SELECT * FROM foo WHERE bar = 1) FROM 1 LET 1 AS a WHERE b = c GROUP BY d GROUP AS e HAVING f = '123' LIMIT 3 OFFSET 4",
            """
                SELECT (
                    SELECT *
                    FROM foo
                    WHERE bar = 1)
                FROM 1 LET 1 AS a
                WHERE b = c
                GROUP BY d GROUP AS e
                HAVING f = '123'
                LIMIT 3
                OFFSET 4
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhereGroupHavingLimitOffsetWithSubQuery2() {
        checkPrettyPrintQuery(
            "SELECT (SELECT * FROM (SELECT foo FROM t1 AS t2) AS foo1 WHERE bar = 1) FROM 1 LET 1 AS a WHERE b = c GROUP BY d GROUP AS e HAVING f = '123' LIMIT 3 OFFSET 4",
            """
                SELECT (
                    SELECT *
                    FROM (
                        SELECT foo
                        FROM t1 AS t2) AS foo1
                    WHERE bar = 1)
                FROM 1 LET 1 AS a
                WHERE b = c
                GROUP BY d GROUP AS e
                HAVING f = '123'
                LIMIT 3
                OFFSET 4
            """.trimIndent()
        )
    }

    // TODO: Make the following queries looks better after formatting
    @Test
    fun selectInFunction() {
        checkPrettyPrintQuery(
            "function0((SELECT a FROM b), c)",
            """
                function0((
                    SELECT a
                    FROM b), c)
            """.trimIndent()
        )
    }

    @Test
    fun caseInFunction() {
        checkPrettyPrintQuery(
            "function0((CASE name WHEN 'jack' THEN 1 WHEN 'joe' THEN 2 END), c)",
            """
                function0((
                    CASE name
                        WHEN 'jack' THEN 1
                        WHEN 'joe' THEN 2
                    END), c)
            """.trimIndent()
        )
    }

    @Test
    fun selectInContainerType() {
        checkPrettyPrintQuery(
            "<< (SELECT a FROM b), c >>",
            """
                << (
                    SELECT a
                    FROM b), c >>
            """.trimIndent()
        )
    }

    @Test
    fun caseInContainerType() {
        checkPrettyPrintQuery(
            "<< (CASE name WHEN 'jack' THEN 1 WHEN 'joe' THEN 2 END), c >>",
            """
                << (
                    CASE name
                        WHEN 'jack' THEN 1
                        WHEN 'joe' THEN 2
                    END), c >>
            """.trimIndent()
        )
    }

    @Test
    fun selectInOperator1() {
        checkPrettyPrintQuery(
            "(SELECT a FROM b) UNION (SELECT c FROM d) UNION (SELECT e FROM f)",
            """
                ((
                    SELECT a
                    FROM b) UNION (
                    SELECT c
                    FROM d)) UNION (
                    SELECT e
                    FROM f)
            """.trimIndent()
        )
    }

    @Test
    fun selectInOperator2() {
        checkPrettyPrintQuery(
            "(SELECT a FROM b) UNION c",
            """
                (
                    SELECT a
                    FROM b) UNION c
            """.trimIndent()
        )
    }

    @Test
    fun selectInOperator3() {
        checkPrettyPrintQuery(
            "CAST((SELECT VALUE a FROM b) AS STRING)",
            """
                CAST ((
                    SELECT VALUE a
                    FROM b) AS STRING)
            """.trimIndent()
        )
    }

    @Test
    fun caseInOperator1() {
        checkPrettyPrintQuery(
            "(CASE name WHEN 'jack' THEN 1 WHEN 'joe' THEN 2 END) || ' alice'",
            """
                (
                    CASE name
                        WHEN 'jack' THEN 1
                        WHEN 'joe' THEN 2
                    END) || ' alice'
            """.trimIndent()
        )
    }

    @Test
    fun caseInOperator2() {
        checkPrettyPrintQuery(
            "CAST((CASE name WHEN 'jack' THEN 1 WHEN 'joe' THEN 2 END) AS STRING)",
            """
                CAST ((
                    CASE name
                        WHEN 'jack' THEN 1
                        WHEN 'joe' THEN 2
                    END) AS STRING)
            """.trimIndent()
        )
    }

    @Test
    fun selectInSimpleCase() {
        checkPrettyPrintQuery(
            "CASE (SELECT name FROM t) WHEN (SELECT a FROM b) UNION c THEN 1 WHEN (SELECT c FROM d) THEN 2 ELSE (SELCT e FROM f) END",
            """
                CASE (
                    SELECT name
                    FROM t)
                    WHEN (
                        SELECT a
                        FROM b) UNION c THEN 1
                    WHEN (
                        SELECT c
                        FROM d) THEN 2
                    ELSE (
                        SELECT e 
                        FROM f)
                END
            """.trimIndent()
        )
    }

    @Test
    fun selectInSearchedCase() {
        checkPrettyPrintQuery(
            "CASE WHEN name = 'jack' THEN (SELECT a FROM b) WHEN (SELECT c FROM d) IS INT THEN (SELECT f FROM g) WHEN (SELECT foo FROM t1) THEN (SELECT bar FROM t2) ELSE (SELECT h FROM i) END",
            """
                CASE
                    WHEN name = 'jack' THEN (
                        SELECT a
                        FROM b)
                    WHEN (
                        SELECT c
                        FROM d) IS INT THEN (
                        SELECT f
                        FROM g)
                    WHEN (
                        SELECT foo
                        FROM t1) THEN (
                        SELECT bar
                        FROM t2)
                    ELSE (
                        SELECT h
                        FROM i)
                END
            """.trimIndent()
        )
    }
}
