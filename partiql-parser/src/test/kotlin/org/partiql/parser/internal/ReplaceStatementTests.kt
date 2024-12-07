package org.partiql.parser.internal

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

class ReplaceStatementTests {

    @ParameterizedTest
    @ArgumentsSource(SuccessTestCases::class)
    fun success(tc: PTestDef) {
        tc.assert()
    }

    @ParameterizedTest
    @ArgumentsSource(FailureTestCases::class)
    fun failure(tc: PTestDef) {
        tc.assert()
    }

    object SuccessTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<ParserTestCaseSimple> {
            val m = mapOf(
                "Simplest test" to "REPLACE INTO tbl VALUES (1, 2, 3)",
                "Column definitions" to "REPLACE INTO tbl (a, b, c) VALUES (1, 2, 3)",
                "Multiple rows" to "REPLACE INTO tbl VALUES (1, 2, 3), (4, 5, 6), (7, 8, 8)",
                "Multiple non-row values" to "REPLACE INTO tbl VALUES 1, 2, 3",
                "Using DEFAULT VALUES" to "REPLACE INTO tbl DEFAULT VALUES",
                "Explicit bag" to "REPLACE INTO tbl << 1, 2, 3 >>",
                "Explicit array" to "REPLACE INTO tbl [ 1, 2, 3 ]",
                "Namespaced table name with delimited alias" to "REPLACE INTO \"CaT1\".schema1.\"TBL_1\" AS \"myt\" VALUES (1, 2, 3)",
                "Namespaced table name with regular alias" to "REPLACE INTO \"CaT1\".schema1.\"TBL_1\" AS myt VALUES (1, 2, 3)",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = true) }
            return m
        }
    }

    object FailureTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<ParserTestCaseSimple> {
            val m = mapOf(
                "Lack of table name" to "REPLACE INTO VALUES (1, 2, 3)",
                "Trailing comma for VALUES" to "REPLACE INTO tbl VALUES (1, 2, 3), (4, 5, 6),",
                "Not DEFAULT VALUES" to "REPLACE INTO tbl DEFAULT VALUE",
                "Bad column definitions" to "REPLACE INTO tbl (a.b, b, c) VALUES (1, 2, 3)",
                "Empty column definitions" to "REPLACE INTO tbl () VALUES (1, 2, 3)",
                "No values" to "REPLACE INTO tbl (a, b) VALUES",
                "Bad alias" to "REPLACE INTO tbl AS alias1.alias2 (a, b) VALUES",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = false) }
            return m
        }
    }
}
