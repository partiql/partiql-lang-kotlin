package org.partiql.parser.internal

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

class PartiQLParserSLLTests {
    @ParameterizedTest
    @ArgumentsSource(SLLTestCases::class)
    fun `parses in SLL mode`(tc: PTestDef) {
        tc.assert()
    }

    object SLLTestCases : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            ParserTestCaseSLL("single COALESCE should succeed", "SELECT COALESCE(a, b) FROM T", true),
            ParserTestCaseSLL("nested COALESCE should succeed", "SELECT COALESCE(COALESCE(a, b), c) FROM T", true),
            ParserTestCaseSLL("NULLIF should succeed", "SELECT NULLIF(a, b) FROM T", true),
            ParserTestCaseSLL("SUBSTRING should failed", "SELECT SUBSTRING(a, b) FROM T", false)
        )
    }
}
