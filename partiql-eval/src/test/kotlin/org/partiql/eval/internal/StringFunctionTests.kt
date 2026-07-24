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
    @MethodSource("charLengthTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun charLengthTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("bitLengthTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun bitLengthTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("octetLengthTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun octetLengthTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("substringTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun substringTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("positionTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun positionTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("trimLeadingTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun trimLeadingTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("trimTrailingTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun trimTrailingTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("trimCharsTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun trimCharsTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("likeTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun likeTests(tc: SuccessTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun replaceTestCases() = listOf(
            // --- Behavior ---
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
            SuccessTestCase(
                name = "replace: overlapping matches are non-overlapping left-to-right",
                input = "replace('aaa', 'aa', 'b');",
                expected = Datum.string("ba"),
            ),
            SuccessTestCase(
                name = "replace: to contains from is a single pass (no recursion)",
                input = "replace('a', 'a', 'aa');",
                expected = Datum.string("aa"),
            ),
            SuccessTestCase(
                name = "replace: multi-byte (non-ASCII) codepoints",
                input = "replace('héllo', 'é', 'e');",
                expected = Datum.string("hello"),
            ),
            // --- Argument-type matrix (types resolve together based on arg0; STRICT mode) ---
            //   - `string` (arg0): CHAR, VARCHAR, CLOB, STRING; CHAR/VARCHAR widen to VARCHAR in the result.
            //   - `from` / `to`  : STRING for a CHAR/VARCHAR/STRING `string` (CHAR/VARCHAR coerced to
            //                      STRING); CLOB for a CLOB `string`.
            // arg0 type -> result type
            SuccessTestCase(
                name = "replace: STRING arg0 returns STRING",
                input = "replace(CAST('hello' AS STRING), 'l', 'L');",
                expected = Datum.string("heLLo"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "replace: CHAR arg0 returns VARCHAR",
                input = "replace(CAST('hello' AS CHAR(5)), 'l', 'LL');",
                expected = Datum.varchar("heLLLLo"),
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
            // from / to types
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
            SuccessTestCase(
                name = "replace: CLOB string with CLOB from/to",
                input = "replace(CAST('hello' AS CLOB), CAST('l' AS CLOB), CAST('L' AS CLOB));",
                expected = Datum.clob("heLLo".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // --- Null / missing propagation (every parameter) ---
            SuccessTestCase(
                name = "replace: null string returns null",
                input = "replace(NULL, 'a', 'b');",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "replace: null from returns null",
                input = "replace('hello', NULL, 'b');",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "replace: null to returns null",
                input = "replace('hello', 'a', NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "replace: missing string returns missing",
                input = "replace(MISSING, 'a', 'b');",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "replace: missing from returns missing",
                input = "replace('hello', MISSING, 'b');",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "replace: missing to returns missing",
                input = "replace('hello', 'a', MISSING);",
                expected = Datum.missing(),
            ),
        )

        @JvmStatic
        fun splitTestCases() = listOf(
            // --- Behavior ---
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
            SuccessTestCase(
                name = "split: empty delimiter returns whole input as single element",
                input = "split('abc', '');",
                expected = Datum.array(listOf(Datum.string("abc"))),
            ),
            SuccessTestCase(
                name = "split: multi-byte (non-ASCII) delimiter",
                input = "split('aXbXc', 'X');",
                expected = Datum.array(listOf(Datum.string("a"), Datum.string("b"), Datum.string("c"))),
            ),
            SuccessTestCase(
                name = "split: multi-byte value preserves surrogate-pair codepoints",
                input = "split('👍,a', ',');",
                expected = Datum.array(listOf(Datum.string("👍"), Datum.string("a"))),
            ),
            // --- Argument-type matrix (types resolve together based on arg0; STRICT mode) ---
            //   - `string` (arg0): CHAR, VARCHAR, CLOB, STRING; CHAR/VARCHAR widen to VARCHAR list elements.
            //   - `delimiter`    : STRING for a CHAR/VARCHAR/STRING `string` (CHAR/VARCHAR coerced to
            //                      STRING); CLOB for a CLOB `string`.
            // arg0 type -> list element type
            SuccessTestCase(
                name = "split: STRING arg0 returns list of STRING",
                input = "split(CAST('a,b' AS STRING), ',');",
                expected = Datum.array(listOf(Datum.string("a"), Datum.string("b"))),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "split: CHAR arg0 returns list of VARCHAR",
                input = "split(CAST('a,b' AS CHAR(3)), ',');",
                expected = Datum.array(listOf(Datum.varchar("a"), Datum.varchar("b"))),
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
            // delimiter types
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
            SuccessTestCase(
                name = "split: CLOB string with CLOB delimiter",
                input = "split(CAST('a,b' AS CLOB), CAST(',' AS CLOB));",
                expected = Datum.array(listOf(Datum.clob("a".toByteArray()), Datum.clob("b".toByteArray()))),
                mode = Mode.STRICT(),
            ),
            // --- Null / missing propagation (every parameter) ---
            SuccessTestCase(
                name = "split: null string returns null",
                input = "split(NULL, ',');",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "split: null delimiter returns null",
                input = "split('a,b', NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "split: missing string returns missing",
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
         * `char_length` accepts CHAR/VARCHAR/CLOB/STRING and always returns INTEGER (the input type
         * is not preserved). STRICT mode ensures an unresolved overload surfaces as an error rather
         * than silently evaluating to MISSING. CHAR widths match the content length so trailing-space
         * padding does not change the counts.
         */
        @JvmStatic
        fun charLengthTestCases() = listOf(
            SuccessTestCase(
                name = "char_length: STRING",
                input = "char_length(CAST('hello' AS STRING));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "char_length: CHAR",
                input = "char_length(CAST('hello' AS CHAR(5)));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "char_length: VARCHAR",
                input = "char_length(CAST('hello' AS VARCHAR(5)));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "char_length: CLOB",
                input = "char_length(CAST('hello' AS CLOB));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            // null / missing propagation
            SuccessTestCase(
                name = "char_length: null arg returns null",
                input = "char_length(NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "char_length: missing arg returns missing",
                input = "char_length(MISSING);",
                expected = Datum.missing(),
            ),
        )

        /**
         * `bit_length` accepts CHAR/VARCHAR/CLOB/STRING and always returns INTEGER (8 bits per byte).
         */
        @JvmStatic
        fun bitLengthTestCases() = listOf(
            SuccessTestCase(
                name = "bit_length: STRING",
                input = "bit_length(CAST('hello' AS STRING));",
                expected = Datum.integer(40),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "bit_length: CHAR",
                input = "bit_length(CAST('hello' AS CHAR(5)));",
                expected = Datum.integer(40),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "bit_length: VARCHAR",
                input = "bit_length(CAST('hello' AS VARCHAR(5)));",
                expected = Datum.integer(40),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "bit_length: CLOB",
                input = "bit_length(CAST('hello' AS CLOB));",
                expected = Datum.integer(40),
                mode = Mode.STRICT(),
            ),
            // null / missing propagation
            SuccessTestCase(
                name = "bit_length: null arg returns null",
                input = "bit_length(NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "bit_length: missing arg returns missing",
                input = "bit_length(MISSING);",
                expected = Datum.missing(),
            ),
        )

        /**
         * `octet_length` accepts CHAR/VARCHAR/CLOB/STRING and always returns INTEGER (number of bytes).
         */
        @JvmStatic
        fun octetLengthTestCases() = listOf(
            SuccessTestCase(
                name = "octet_length: STRING",
                input = "octet_length(CAST('hello' AS STRING));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "octet_length: CHAR",
                input = "octet_length(CAST('hello' AS CHAR(5)));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "octet_length: VARCHAR",
                input = "octet_length(CAST('hello' AS VARCHAR(5)));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "octet_length: CLOB",
                input = "octet_length(CAST('hello' AS CLOB));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            // null / missing propagation
            SuccessTestCase(
                name = "octet_length: null arg returns null",
                input = "octet_length(NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "octet_length: missing arg returns missing",
                input = "octet_length(MISSING);",
                expected = Datum.missing(),
            ),
        )

        /**
         * `substring` preserves the input type (CHAR/VARCHAR/CLOB/STRING) in the result. Exercises
         * both the 2-arg and SQL `FROM ... FOR ...` (3-arg) forms across the type matrix.
         */
        @JvmStatic
        fun substringTestCases() = listOf(
            SuccessTestCase(
                name = "substring: STRING (start only)",
                input = "substring(CAST('hello' AS STRING), 2);",
                expected = Datum.string("ello"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "substring: STRING (start, length)",
                input = "substring(CAST('hello' AS STRING) FROM 2 FOR 3);",
                expected = Datum.string("ell"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "substring: CHAR (start only) returns VARCHAR",
                input = "substring(CAST('hello' AS CHAR(5)), 2);",
                expected = Datum.varchar("ello"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "substring: VARCHAR (start, length) returns VARCHAR",
                input = "substring(CAST('hello' AS VARCHAR(5)) FROM 2 FOR 3);",
                expected = Datum.varchar("ell"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "substring: CLOB (start only) returns CLOB",
                input = "substring(CAST('hello' AS CLOB), 2);",
                expected = Datum.clob("ello".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // null / missing propagation
            SuccessTestCase(
                name = "substring: null value returns null",
                input = "substring(NULL, 2);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "substring: null start returns null",
                input = "substring('hello', NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "substring: null value (with length) returns null",
                input = "substring(NULL FROM 2 FOR 3);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "substring: null start (with length) returns null",
                input = "substring('hello' FROM NULL FOR 3);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "substring: null length returns null",
                input = "substring('hello' FROM 2 FOR NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "substring: missing value returns missing",
                input = "substring(MISSING, 2);",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "substring: missing start returns missing",
                input = "substring('hello', MISSING);",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "substring: missing length returns missing",
                input = "substring('hello' FROM 2 FOR MISSING);",
                expected = Datum.missing(),
            ),
        )

        /**
         * `position(probe IN value)` returns BIGINT (1-based, 0 if not found) for CHAR/VARCHAR/CLOB/STRING.
         */
        @JvmStatic
        fun positionTestCases() = listOf(
            SuccessTestCase(
                name = "position: STRING found",
                input = "position(CAST('lo' AS STRING) IN CAST('hello' AS STRING));",
                expected = Datum.bigint(4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "position: STRING not found",
                input = "position('z' IN 'hello');",
                expected = Datum.bigint(0),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "position: CHAR",
                input = "position(CAST('lo' AS CHAR(2)) IN CAST('hello' AS CHAR(5)));",
                expected = Datum.bigint(4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "position: VARCHAR",
                input = "position(CAST('lo' AS VARCHAR(2)) IN CAST('hello' AS VARCHAR(5)));",
                expected = Datum.bigint(4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "position: CLOB",
                input = "position(CAST('lo' AS CLOB) IN CAST('hello' AS CLOB));",
                expected = Datum.bigint(4),
                mode = Mode.STRICT(),
            ),
            // null / missing propagation
            SuccessTestCase(
                name = "position: null probe returns null",
                input = "position(NULL IN 'hello');",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "position: null value returns null",
                input = "position('lo' IN NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "position: missing probe returns missing",
                input = "position(MISSING IN 'hello');",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "position: missing value returns missing",
                input = "position('lo' IN MISSING);",
                expected = Datum.missing(),
            ),
        )

        /**
         * `TRIM(LEADING <chars> FROM value)`. `trim_leading` returns VARCHAR for CHAR/VARCHAR input,
         * CLOB for CLOB, STRING for STRING.
         */
        @JvmStatic
        fun trimLeadingTestCases() = listOf(
            SuccessTestCase(
                name = "trim_leading: STRING",
                input = "TRIM(LEADING FROM CAST('  hi' AS STRING));",
                expected = Datum.string("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_leading: CHAR returns VARCHAR",
                input = "TRIM(LEADING FROM CAST('  hi' AS CHAR(4)));",
                expected = Datum.varchar("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_leading: VARCHAR",
                input = "TRIM(LEADING FROM CAST('  hi' AS VARCHAR(4)));",
                expected = Datum.varchar("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_leading: CLOB",
                input = "TRIM(LEADING FROM CAST('  hi' AS CLOB));",
                expected = Datum.clob("hi".toByteArray()),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_leading_chars: STRING",
                input = "TRIM(LEADING 'x' FROM CAST('xxhi' AS STRING));",
                expected = Datum.string("hi"),
                mode = Mode.STRICT(),
            ),
            // null / missing propagation
            SuccessTestCase(
                name = "trim_leading: null value returns null",
                input = "TRIM(LEADING FROM NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim_leading_chars: null chars returns null",
                input = "TRIM(LEADING NULL FROM 'xxhi');",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim_leading_chars: null value returns null",
                input = "TRIM(LEADING 'x' FROM NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim_leading: missing value returns missing",
                input = "TRIM(LEADING FROM MISSING);",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "trim_leading_chars: missing chars returns missing",
                input = "TRIM(LEADING MISSING FROM 'xxhi');",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "trim_leading_chars: missing value returns missing",
                input = "TRIM(LEADING 'x' FROM MISSING);",
                expected = Datum.missing(),
            ),
        )

        /**
         * `TRIM(TRAILING <chars> FROM value)`.
         */
        @JvmStatic
        fun trimTrailingTestCases() = listOf(
            SuccessTestCase(
                name = "trim_trailing: STRING",
                input = "TRIM(TRAILING FROM CAST('hi  ' AS STRING));",
                expected = Datum.string("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_trailing: CHAR returns VARCHAR",
                input = "TRIM(TRAILING FROM CAST('hi  ' AS CHAR(4)));",
                expected = Datum.varchar("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_trailing: VARCHAR",
                input = "TRIM(TRAILING FROM CAST('hi  ' AS VARCHAR(4)));",
                expected = Datum.varchar("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_trailing: CLOB",
                input = "TRIM(TRAILING FROM CAST('hi  ' AS CLOB));",
                expected = Datum.clob("hi".toByteArray()),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_trailing_chars: STRING",
                input = "TRIM(TRAILING 'x' FROM CAST('hixx' AS STRING));",
                expected = Datum.string("hi"),
                mode = Mode.STRICT(),
            ),
            // null / missing propagation
            SuccessTestCase(
                name = "trim_trailing: null value returns null",
                input = "TRIM(TRAILING FROM NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim_trailing_chars: null chars returns null",
                input = "TRIM(TRAILING NULL FROM 'hixx');",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim_trailing_chars: null value returns null",
                input = "TRIM(TRAILING 'x' FROM NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim_trailing: missing value returns missing",
                input = "TRIM(TRAILING FROM MISSING);",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "trim_trailing_chars: missing chars returns missing",
                input = "TRIM(TRAILING MISSING FROM 'hixx');",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "trim_trailing_chars: missing value returns missing",
                input = "TRIM(TRAILING 'x' FROM MISSING);",
                expected = Datum.missing(),
            ),
        )

        /**
         * `TRIM(BOTH chars FROM value)` maps to `trim_chars`.
         */
        @JvmStatic
        fun trimCharsTestCases() = listOf(
            SuccessTestCase(
                name = "trim_chars: STRING",
                input = "TRIM(BOTH 'x' FROM CAST('xxhixx' AS STRING));",
                expected = Datum.string("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_chars: CHAR returns VARCHAR",
                input = "TRIM(BOTH 'x' FROM CAST('xxhixx' AS CHAR(6)));",
                expected = Datum.varchar("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_chars: VARCHAR",
                input = "TRIM(BOTH 'x' FROM CAST('xxhixx' AS VARCHAR(6)));",
                expected = Datum.varchar("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_chars: CLOB",
                input = "TRIM(BOTH 'x' FROM CAST('xxhixx' AS CLOB));",
                expected = Datum.clob("hi".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // null / missing propagation
            SuccessTestCase(
                name = "trim_chars: null value returns null",
                input = "TRIM(BOTH 'x' FROM NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim_chars: null chars returns null",
                input = "TRIM(BOTH NULL FROM 'xxhixx');",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim_chars: missing value returns missing",
                input = "TRIM(BOTH 'x' FROM MISSING);",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "trim_chars: missing chars returns missing",
                input = "TRIM(BOTH MISSING FROM 'xxhixx');",
                expected = Datum.missing(),
            ),
        )

        /**
         * `LIKE` and `LIKE ... ESCAPE ...` predicates return BOOL for CHAR/VARCHAR/CLOB/STRING inputs.
         */
        @JvmStatic
        fun likeTestCases() = listOf(
            SuccessTestCase(
                name = "like: STRING match",
                input = "CAST('abc' AS STRING) LIKE CAST('a%' AS STRING);",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "like: CHAR match",
                input = "CAST('abc' AS CHAR(3)) LIKE CAST('a_c' AS CHAR(3));",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "like: VARCHAR no match",
                input = "CAST('abc' AS VARCHAR(3)) LIKE CAST('a%z' AS VARCHAR(3));",
                expected = Datum.bool(false),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "like: CLOB match",
                input = "CAST('abc' AS CLOB) LIKE CAST('a%' AS CLOB);",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "like_escape: STRING literal underscore",
                input = "CAST('a_c' AS STRING) LIKE CAST('a\\_c' AS STRING) ESCAPE '\\';",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
            ),
            // null / missing propagation (typed NULL so the LIKE overload resolves)
            SuccessTestCase(
                name = "like: null value returns null",
                input = "CAST(NULL AS STRING) LIKE 'a%';",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "like: null pattern returns null",
                input = "'abc' LIKE CAST(NULL AS STRING);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "like: missing value returns missing",
                input = "MISSING LIKE 'a%';",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "like: missing pattern returns missing",
                input = "'abc' LIKE MISSING;",
                expected = Datum.missing(),
            ),
        )
    }
}
