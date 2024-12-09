package org.partiql.parser.internal

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

class UpdateStatementTests {

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
                "Simplest test" to "UPDATE tbl SET x = 1",
                "With multiple sets" to "UPDATE tbl SET x = 1, y = 2, z = 3",
                "With multiple sets and condition" to "UPDATE tbl SET x = 1, y = 2, z = 3 WHERE a < 4",
                "Namespaced table name" to "UPDATE \"CaT1\".schema1.\"TBL_1\" SET x = 1, y = 2",
                "Namespaced table name with condition" to "UPDATE \"CaT1\".schema1.\"TBL_1\" SET x = 1, y = 2 WHERE y < x",
                "Multiple complex steps" to "UPDATE tbl SET x.y.z = 1, x['y'].z[0].\"a\".b = 2",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = true) }
            return m
        }
    }

    object FailureTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<ParserTestCaseSimple> {
            val m = mapOf(
                "Lack of table name" to "UPDATE SET x = 1 WHERE a = 2",
                "Lack of condition" to "UPDATE tbl SET x = 1 WHERE",
                "Lack of SET" to "UPDATE tbl WHERE a = 2",
                "Bad step index" to "UPDATE tbl SET x[1 + 2] = 0",
                "Bad step key" to "UPDATE tbl SET x['a' || 'b'] = 0",
                "Bad step number where root is not a col ref" to "UPDATE tbl SET 1 = 0",
                "Bad step string where root is not a col ref" to "UPDATE tbl SET 'a' = 0",
                "Bad step struct where root is not a col ref" to "UPDATE tbl SET {'a': 1}.a = 0",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = false) }
            return m
        }
    }
}
