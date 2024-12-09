package org.partiql.parser.internal

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

class UpsertStatementTests {

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
                "Simplest test" to "UPSERT INTO tbl VALUES (1, 2, 3)",
                "Column definitions" to "UPSERT INTO tbl (a, b, c) VALUES (1, 2, 3)",
                "Multiple rows" to "UPSERT INTO tbl VALUES (1, 2, 3), (4, 5, 6), (7, 8, 8)",
                "Multiple non-row values" to "UPSERT INTO tbl VALUES 1, 2, 3",
                "Using DEFAULT VALUES" to "UPSERT INTO tbl DEFAULT VALUES",
                "Explicit bag" to "UPSERT INTO tbl << 1, 2, 3 >>",
                "Explicit array" to "UPSERT INTO tbl [ 1, 2, 3 ]",
                "Namespaced table name with delimited alias" to "UPSERT INTO \"CaT1\".schema1.\"TBL_1\" AS \"myt\" VALUES (1, 2, 3)",
                "Namespaced table name with regular alias" to "UPSERT INTO \"CaT1\".schema1.\"TBL_1\" AS myt VALUES (1, 2, 3)",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = true) }
            return m
        }
    }

    object FailureTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<ParserTestCaseSimple> {
            val m = mapOf(
                "Lack of table name" to "UPSERT INTO VALUES (1, 2, 3)",
                "Trailing comma for VALUES" to "UPSERT INTO tbl VALUES (1, 2, 3), (4, 5, 6),",
                "Not DEFAULT VALUES" to "UPSERT INTO tbl DEFAULT VALUE",
                "Bad column definitions" to "UPSERT INTO tbl (a.b, b, c) VALUES (1, 2, 3)",
                "Empty column definitions" to "UPSERT INTO tbl () VALUES (1, 2, 3)",
                "No values" to "UPSERT INTO tbl (a, b) VALUES",
                "Bad alias" to "UPSERT INTO tbl AS alias1.alias2 (a, b) VALUES",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = false) }
            return m
        }
    }
}
