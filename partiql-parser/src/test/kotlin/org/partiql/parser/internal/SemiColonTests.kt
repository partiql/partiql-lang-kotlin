package org.partiql.parser.internal

import org.junit.jupiter.api.Test

class SemiColonTests {
    @Test
    fun t1() {
        val tc = ParserTestCaseSimple(
            "Valid multi-statement",
            "1 + 1; 1 + 2; SELECT * FROM t; SELECT * FROM t2 AS t2;"
        )
        tc.assert()
    }

    // TODO: This is not completely settled, however, we can always allow for this in the future.
    @Test
    fun t2() {
        val tc = ParserTestCaseSimple(
            "Invalid multi-statement where last statement doesn't have a semi-colon",
            "1 + 1; 1 + 2; SELECT * FROM t; SELECT * FROM t2 AS t2",
            false
        )
        tc.assert()
    }

    @Test
    fun t3() {
        val tc = ParserTestCaseSimple(
            "First statement has two semi-colons.",
            "1 + 1;; 1 + 2; SELECT * FROM t; SELECT * FROM t2 AS t2;",
            false
        )
        tc.assert()
    }

    @Test
    fun t4() {
        val tc = ParserTestCaseSimple(
            "Trailing semi-colon",
            "1 + 1; 1 + 2; SELECT * FROM t; SELECT * FROM t2 AS t2;;",
            false
        )
        tc.assert()
    }

    @Test
    fun t5() {
        val tc = ParserTestCaseSimple(
            "Preceding semi-colon",
            ";1 + 1; 1 + 2; SELECT * FROM t; SELECT * FROM t2 AS t2;",
            false
        )
        tc.assert()
    }

    @Test
    fun t6() {
        val tc = ParserTestCaseSimple(
            "Empty statement with semi-colon",
            ";",
            false
        )
        tc.assert()
    }

    @Test
    fun t7() {
        val tc = ParserTestCaseSimple(
            "Empty statement",
            "",
            false
        )
        tc.assert()
    }
}
