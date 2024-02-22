package org.partiql.eval.util.testdsl

import org.junit.jupiter.api.assertDoesNotThrow
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.syntax.PartiQLParserBuilder

/** Defines a test case for query evaluation. */
data class IonResultTestCase(
    /** Name of the test.  Shows in IDE's test runner.  Not required to be unique. */
    val name: String,

    /** The test's group name, if any. */
    val group: String? = null,

    /** Useful additional details about the test. */
    val note: String? = null,

    /** The query to be evaluated. */
    val sqlUnderTest: String,

    /**
     * The expected result when run in [org.partiql.lang.eval.TypingMode.LEGACY], formatted in Ion text.
     */
    val expectedLegacyModeIonResult: String,

    /**
     * The expected result when run in [org.partiql.lang.eval.TypingMode.PERMISSIVE], formatted in Ion text.
     */
    val expectedPermissiveModeIonResult: String,

    /**
     * If the test unexpectedly succeeds, cause the unit test to fail.
     *
     * This should be set to true for all tests which are on a "fail list".
     *
     * When a failing test is fixed, it should be removed from all fail lists.  This ensures that all passing tests
     * are removed from all fail lists.  Without this, our fail lists will likely include passing tests.
     */
    val expectFailure: Boolean = false,

    /** The compile options to use. */
    val compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },

    /** An optional block in which to execute additional assertions. */
    val extraAssertions: (ExprValue) -> Unit,
) {
    private val cleanedSqlUnderTest =
        sqlUnderTest.replace("\n", "")

    override fun toString(): String = listOfNotNull(group, name, note, cleanedSqlUnderTest).joinToString(" - ")

    fun toStatementTestCase(): StatementTestCase =
        assertDoesNotThrow("IonResultTestCase ${toString()} should not throw when parsing") {
            val parser = PartiQLParserBuilder.standard().build()
            StatementTestCase(name, parser.parseAstStatement(sqlUnderTest))
        }
}
