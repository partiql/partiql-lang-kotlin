package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.spi.value.Datum

class StringFunctionTests {

    @ParameterizedTest
    @MethodSource("replaceTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun replaceTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("splitTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun splitTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("nullPropagationTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun nullPropagationTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("replaceTypeTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun replaceTypeTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("splitTypeTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun splitTypeTests(tc: SuccessTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun replaceTestCases() = listOf(
            SuccessTestCase(
                name = "replace: basic substitution",
                input = "replace('hello world', 'world', 'PartiQL');",
                expected = Datum.string("hello PartiQL"),
            ),
            SuccessTestCase(
                name = "replace: all occurrences",
                input = "replace('aaa', 'a', 'bb');",
                expected = Datum.string("bbbbbb"),
            ),
            SuccessTestCase(
                name = "replace: no match returns original",
                input = "replace('hello', 'xyz', 'abc');",
                expected = Datum.string("hello"),
            ),
            SuccessTestCase(
                name = "replace: empty from matches nothing",
                input = "replace('hello', '', 'x');",
                expected = Datum.string("hello"),
            ),
            SuccessTestCase(
                name = "replace: delete by replacing with empty string",
                input = "replace('hello world', ' world', '');",
                expected = Datum.string("hello"),
            ),
            SuccessTestCase(
                name = "replace: empty string input",
                input = "replace('', 'a', 'b');",
                expected = Datum.string(""),
            ),
        )

        @JvmStatic
        fun splitTestCases() = listOf(
            SuccessTestCase(
                name = "split: basic delimiter",
                input = "split('a,b,c', ',');",
                expected = Datum.array(
                    listOf(Datum.string("a"), Datum.string("b"), Datum.string("c"))
                ),
            ),
            SuccessTestCase(
                name = "split: no match returns single-element array",
                input = "split('hello', ',');",
                expected = Datum.array(listOf(Datum.string("hello"))),
            ),
            SuccessTestCase(
                name = "split: multi-character delimiter",
                input = "split('one::two::three', '::');",
                expected = Datum.array(
                    listOf(Datum.string("one"), Datum.string("two"), Datum.string("three"))
                ),
            ),
            SuccessTestCase(
                name = "split: leading delimiter produces empty first element",
                input = "split(',a,b', ',');",
                expected = Datum.array(
                    listOf(Datum.string(""), Datum.string("a"), Datum.string("b"))
                ),
            ),
            SuccessTestCase(
                name = "split: trailing delimiter produces empty last element",
                input = "split('a,b,', ',');",
                expected = Datum.array(
                    listOf(Datum.string("a"), Datum.string("b"), Datum.string(""))
                ),
            ),
            SuccessTestCase(
                name = "split: empty string input",
                input = "split('', ',');",
                expected = Datum.array(listOf(Datum.string(""))),
            ),
        )

        @JvmStatic
        fun nullPropagationTestCases() = listOf(
            SuccessTestCase(
                name = "replace: null first arg returns null",
                input = "replace(NULL, 'a', 'b');",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "replace: null second arg returns null",
                input = "replace('hello', NULL, 'b');",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "replace: null third arg returns null",
                input = "replace('hello', 'a', NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "split: null first arg returns null",
                input = "split(NULL, ',');",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "split: null delimiter returns null",
                input = "split('a,b', NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "replace: missing first arg returns missing",
                input = "replace(MISSING, 'a', 'b');",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "replace: missing second arg returns missing",
                input = "replace('hello', MISSING, 'b');",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "replace: missing third arg returns missing",
                input = "replace('hello', 'a', MISSING);",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "split: missing first arg returns missing",
                input = "split(MISSING, ',');",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "split: missing delimiter returns missing",
                input = "split('a,b', MISSING);",
                expected = Datum.missing(),
            ),
        )

        /**
         * Argument-type matrix for `replace(string, from, to)`:
         * - `string` (arg0) should accept CHAR, VARCHAR, CLOB, STRING
         * - `from` / `to` (arg1, arg2) should accept CHAR, VARCHAR, STRING
         *
         * STRICT mode is used so that an unresolved overload surfaces as an error rather than
         * silently evaluating to MISSING.
         */
        @JvmStatic
        fun replaceTypeTestCases() = listOf(
            // arg0 types
            SuccessTestCase(
                name = "replace: STRING arg0",
                input = "replace(CAST('hello' AS STRING), 'l', 'L');",
                expected = Datum.string("heLLo"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "replace: CHAR arg0 returns CHAR",
                input = "replace(CAST('hello' AS CHAR(5)), 'l', 'L');",
                expected = Datum.character("heLLo"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "replace: VARCHAR arg0 returns VARCHAR",
                input = "replace(CAST('hello' AS VARCHAR(5)), 'l', 'L');",
                expected = Datum.varchar("heLLo"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "replace: CLOB arg0 returns CLOB",
                input = "replace(CAST('hello' AS CLOB), 'l', 'L');",
                expected = Datum.clob("heLLo".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // arg1 / arg2 types
            SuccessTestCase(
                name = "replace: CHAR from/to",
                input = "replace('hello', CAST('l' AS CHAR(1)), CAST('L' AS CHAR(1)));",
                expected = Datum.string("heLLo"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "replace: VARCHAR from/to",
                input = "replace('hello', CAST('l' AS VARCHAR(1)), CAST('L' AS VARCHAR(1)));",
                expected = Datum.string("heLLo"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "replace: STRING from/to",
                input = "replace('hello', CAST('l' AS STRING), CAST('L' AS STRING));",
                expected = Datum.string("heLLo"),
                mode = Mode.STRICT(),
            ),
        )

        /**
         * Argument-type matrix for `split(string, delimiter)`:
         * - `string` (arg0) should accept CHAR, VARCHAR, CLOB, STRING
         * - `delimiter` (arg1) should accept CHAR, VARCHAR, STRING
         *
         * STRICT mode is used so that an unresolved overload surfaces as an error rather than
         * silently evaluating to MISSING.
         */
        @JvmStatic
        fun splitTypeTestCases() = listOf(
            // arg0 types
            SuccessTestCase(
                name = "split: STRING arg0",
                input = "split(CAST('a,b' AS STRING), ',');",
                expected = Datum.array(listOf(Datum.string("a"), Datum.string("b"))),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "split: CHAR arg0 returns list of CHAR",
                input = "split(CAST('a,b' AS CHAR(3)), ',');",
                expected = Datum.array(listOf(Datum.character("a"), Datum.character("b"))),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "split: VARCHAR arg0 returns list of VARCHAR",
                input = "split(CAST('a,b' AS VARCHAR(3)), ',');",
                expected = Datum.array(listOf(Datum.varchar("a"), Datum.varchar("b"))),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "split: CLOB arg0 returns list of CLOB",
                input = "split(CAST('a,b' AS CLOB), ',');",
                expected = Datum.array(listOf(Datum.clob("a".toByteArray()), Datum.clob("b".toByteArray()))),
                mode = Mode.STRICT(),
            ),
            // arg1 (delimiter) types
            SuccessTestCase(
                name = "split: CHAR delimiter",
                input = "split('a,b', CAST(',' AS CHAR(1)));",
                expected = Datum.array(listOf(Datum.string("a"), Datum.string("b"))),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "split: VARCHAR delimiter",
                input = "split('a,b', CAST(',' AS VARCHAR(1)));",
                expected = Datum.array(listOf(Datum.string("a"), Datum.string("b"))),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "split: STRING delimiter",
                input = "split('a,b', CAST(',' AS STRING));",
                expected = Datum.array(listOf(Datum.string("a"), Datum.string("b"))),
                mode = Mode.STRICT(),
            ),
        )
    }
}
