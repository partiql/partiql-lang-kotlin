package org.partiql.parser.internal

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

class DeleteStatementTests {

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
                "Simplest test" to "DELETE FROM tbl",
                "Simplest deletion with condition" to "DELETE FROM tbl WHERE a = 2",
                "Namespaced table name" to "DELETE FROM \"CaT1\".schema1.\"TBL_1\"",
                "Namespaced table name with condition" to "DELETE FROM \"CaT1\".schema1.\"TBL_1\" WHERE y < x",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = true) }
            return m
        }
    }

    object FailureTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<ParserTestCaseSimple> {
            val m = mapOf(
                "Lack of table name" to "DELETE FROM WHERE a = 2",
                "Lack of condition" to "DELETE FROM tbl WHERE",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = false) }
            return m
        }
    }
}
