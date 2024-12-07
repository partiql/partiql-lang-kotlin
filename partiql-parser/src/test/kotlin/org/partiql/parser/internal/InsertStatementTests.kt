package org.partiql.parser.internal

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

class InsertStatementTests {

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
                "Simplest insert of a row" to "INSERT INTO tbl VALUES (1, 2, 3)",
                "Column definitions" to "INSERT INTO tbl (a, b, c) VALUES (1, 2, 3)",
                "Multiple rows" to "INSERT INTO tbl VALUES (1, 2, 3), (4, 5, 6), (7, 8, 8)",
                "Multiple explicit rows" to "INSERT INTO tbl VALUES ROW (1, 2, 3), (4, 5, 6), ROW (7, 8, 8)",
                "Multiple explicit rows to disambiguate a single parenthesized expression" to "INSERT INTO tbl VALUES ROW (1), 2, ROW (3)",
                "Multiple non-row values" to "INSERT INTO tbl VALUES 1, 2, 3",
                "Using DEFAULT VALUES" to "INSERT INTO tbl DEFAULT VALUES",
                "Explicit bag" to "INSERT INTO tbl << 1, 2, 3 >>",
                "Explicit array" to "INSERT INTO tbl [ 1, 2, 3 ]",
                "Namespaced table name with delimited alias" to "INSERT INTO \"CaT1\".schema1.\"TBL_1\" AS \"myt\" VALUES (1, 2, 3)",
                "Namespaced table name with regular alias" to "INSERT INTO \"CaT1\".schema1.\"TBL_1\" AS myt VALUES (1, 2, 3)",
                "Do nothing on conflict" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT DO NOTHING",
                "Do nothing on conflict with target index" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT (a, b) DO NOTHING",
                "Do nothing on conflict with named constraint" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT ON CONSTRAINT my_constraint DO NOTHING",
                "Do replace excluded on conflict" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT DO REPLACE EXCLUDED",
                "Do replace excluded on conflict with condition" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT DO REPLACE EXCLUDED WHERE a < 2",
                "Do replace excluded on conflict with target index" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT (a, b) DO REPLACE EXCLUDED",
                "Do replace excluded on conflict with target index with condition" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT (a, b) DO REPLACE EXCLUDED WHERE a < 2",
                "Do replace excluded on conflict with named constraint" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT ON CONSTRAINT my_constraint DO REPLACE EXCLUDED",
                "Do replace excluded on conflict with named constraint with condition" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT ON CONSTRAINT my_constraint DO REPLACE EXCLUDED WHERE a < 2",
                "Do update excluded on conflict" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT DO UPDATE EXCLUDED",
                "Do update excluded on conflict with condition" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT DO UPDATE EXCLUDED WHERE a < 2",
                "Do update excluded on conflict with target index" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT (a, b) DO UPDATE EXCLUDED",
                "Do update excluded on conflict with target index with condition" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT (a, b) DO UPDATE EXCLUDED WHERE a < 2",
                "Do update excluded on conflict with named constraint" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT ON CONSTRAINT my_constraint DO UPDATE EXCLUDED",
                "Do update excluded on conflict with named constraint with condition" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT ON CONSTRAINT my_constraint DO UPDATE EXCLUDED WHERE a < 2",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = true) }
            return m
        }
    }

    object FailureTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<ParserTestCaseSimple> {
            val m = mapOf(
                "Lack of table name" to "INSERT INTO VALUES (1, 2, 3)",
                "Trailing comma for VALUES" to "INSERT INTO tbl VALUES (1, 2, 3), (4, 5, 6),",
                "Not DEFAULT VALUES" to "INSERT INTO tbl DEFAULT VALUE",
                "Bad column definitions" to "INSERT INTO tbl (a.b, b, c) VALUES (1, 2, 3)",
                "Empty column definitions" to "INSERT INTO tbl () VALUES (1, 2, 3)",
                "No values" to "INSERT INTO tbl (a, b) VALUES",
                "Bad alias" to "INSERT INTO tbl AS alias1.alias2 (a, b) VALUES",
                "Do something on conflict" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT DO SOMETHING",
                "No parenthesis on conflict with target index" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT a, b DO NOTHING",
                "Bad reference for index on conflict with target index" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT (a, b.c) DO NOTHING",
                "Do nothing on conflict with no named constraint" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT ON CONSTRAINT DO NOTHING",
                "Do update excluded on conflict with condition in wrong spot" to "INSERT INTO tbl << 1, 2, 3 >> ON CONFLICT WHERE a < 2 DO UPDATE EXCLUDED",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = false) }
            return m
        }
    }
}
