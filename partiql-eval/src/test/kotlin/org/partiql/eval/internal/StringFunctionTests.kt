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
    @MethodSource("lowerTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun lowerTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("upperTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun upperTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("concatTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun concatTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("trimTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun trimTests(tc: SuccessTestCase) = tc.run()

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
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST truncates before replace: 'hello' truncated to CHAR(3) = 'hel', replacing 'e' -> 'hEl'.
            // Content shorter than n is space-padded, and the padding is unaffected by a replace that
            // does not target spaces, so it survives into the result.
            SuccessTestCase(
                name = "replace: CHAR arg0 longer than n is truncated before replacing",
                input = "replace(CAST('hello' AS CHAR(3)), 'e', 'E');",
                expected = Datum.varchar("hEl"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "replace: CHAR arg0 shorter than n keeps padding",
                input = "replace(CAST('hi' AS CHAR(5)), 'i', 'I');",
                expected = Datum.varchar("hI   "),
                mode = Mode.STRICT(),
            ),
            // Replace can grow the string past arg0's declared length, so the result type must be
            // unbounded — a bounded one would truncate. 300 a's -> 600 b's, well past the former
            // VARCHAR(255) default that used to cut the value at 255 characters.
            SuccessTestCase(
                // Note: the expected value needs an explicit length, since `Datum.varchar(value)`
                // defaults to 255 and would truncate the 600-character expectation itself.
                name = "replace: result longer than arg0's declared length is not truncated",
                input = "replace(CAST('${"a".repeat(300)}' AS VARCHAR(300)), 'a', 'bb');",
                expected = Datum.varchar("b".repeat(600), 600),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "replace: result longer than a short arg0 is not truncated",
                input = "replace(CAST('aaa' AS VARCHAR(3)), 'a', 'bb');",
                expected = Datum.varchar("bbbbbb", 6),
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
            //   - `string` (arg0): CHAR, VARCHAR, CLOB, STRING; CHAR/VARCHAR widen to VARCHAR list
            //                      elements, keeping arg0's declared length (a part is never longer).
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
                name = "split: CHAR arg0 returns list of VARCHAR(n)",
                input = "split(CAST('a,b' AS CHAR(3)), ',');",
                expected = Datum.array(listOf(Datum.varchar("a", 3), Datum.varchar("b", 3))),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "split: VARCHAR arg0 returns list of VARCHAR(n)",
                input = "split(CAST('a,b' AS VARCHAR(3)), ',');",
                expected = Datum.array(listOf(Datum.varchar("a", 3), Datum.varchar("b", 3))),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "split: CLOB arg0 returns list of CLOB(n)",
                input = "split(CAST('a,b' AS CLOB(3)), ',');",
                expected = Datum.array(listOf(Datum.clob("a".toByteArray(), 3), Datum.clob("b".toByteArray(), 3))),
                mode = Mode.STRICT(),
            ),
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST truncates before split: 'a,b,c' truncated to CHAR(3) = 'a,b', split on ',' = [a, b].
            // Content shorter than n is space-padded by the CHAR cast, and that padding is part of the
            // value: CHAR(5) 'a' is 'a    ', which has no delimiter, so it splits to one padded element.
            SuccessTestCase(
                name = "split: CHAR arg0 longer than n is truncated before splitting",
                input = "split(CAST('a,b,c' AS CHAR(3)), ',');",
                expected = Datum.array(listOf(Datum.varchar("a", 3), Datum.varchar("b", 3))),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "split: CHAR arg0 shorter than n keeps padded single element",
                input = "split(CAST('a' AS CHAR(5)), ',');",
                expected = Datum.array(listOf(Datum.varchar("a    ", 5))),
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
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST to CHAR(n)/VARCHAR(n) truncates content longer than n. Only CHAR space-pads content
            // shorter than n (so CHAR always counts n); VARCHAR keeps the true content length, as does
            // CLOB(n), which enforces neither bound.
            SuccessTestCase(
                name = "char_length: CHAR content longer than n is truncated to n",
                input = "char_length(CAST('hello world' AS CHAR(5)));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "char_length: CHAR content shorter than n is padded to n",
                input = "char_length(CAST('hi' AS CHAR(5)));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "char_length: VARCHAR content longer than n is truncated to n",
                input = "char_length(CAST('hello world' AS VARCHAR(5)));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "char_length: VARCHAR content shorter than n is not padded",
                input = "char_length(CAST('hi' AS VARCHAR(5)));",
                expected = Datum.integer(2),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "char_length: CLOB does not enforce n; counts true content length",
                input = "char_length(CAST('hello world' AS CLOB(5)));",
                expected = Datum.integer(11),
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
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CHAR(5)/VARCHAR(5) truncate or space-pad to 5 characters => 5 bytes => 40 bits; CLOB(5)
            // keeps the full content, so 'hello world' is 11 bytes => 88 bits.
            SuccessTestCase(
                name = "bit_length: CHAR content longer than n is truncated to n",
                input = "bit_length(CAST('hello world' AS CHAR(5)));",
                expected = Datum.integer(40),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "bit_length: CHAR content shorter than n is padded to n",
                input = "bit_length(CAST('hi' AS CHAR(5)));",
                expected = Datum.integer(40),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "bit_length: CLOB does not enforce n; counts true content length",
                input = "bit_length(CAST('hello world' AS CLOB(5)));",
                expected = Datum.integer(88),
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
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CHAR(5)/VARCHAR(5) truncate or space-pad to 5 bytes; CLOB(5) keeps all 11 bytes.
            SuccessTestCase(
                name = "octet_length: CHAR content longer than n is truncated to n",
                input = "octet_length(CAST('hello world' AS CHAR(5)));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "octet_length: CHAR content shorter than n is padded to n",
                input = "octet_length(CAST('hi' AS CHAR(5)));",
                expected = Datum.integer(5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "octet_length: CLOB does not enforce n; counts true content length",
                input = "octet_length(CAST('hello world' AS CLOB(5)));",
                expected = Datum.integer(11),
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
                name = "substring: CHAR (start only) returns VARCHAR(n)",
                input = "substring(CAST('hello' AS CHAR(5)), 2);",
                expected = Datum.varchar("ello", 5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "substring: VARCHAR (start, length) returns VARCHAR(n)",
                input = "substring(CAST('hello' AS VARCHAR(5)) FROM 2 FOR 3);",
                expected = Datum.varchar("ell", 5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "substring: CLOB (start only) returns CLOB",
                input = "substring(CAST('hello' AS CLOB), 2);",
                expected = Datum.clob("ello".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST truncates 'hello world' to CHAR(5) = 'hello' before substring, so FROM 2 FOR 3 = 'ell'.
            // Content shorter than n is space-padded, so extracting past the content yields that padding:
            // CHAR(5) 'hi' is 'hi   ', and FROM 2 FOR 3 takes 'i  '.
            SuccessTestCase(
                name = "substring: value truncated to n before extraction",
                input = "substring(CAST('hello world' AS CHAR(5)) FROM 2 FOR 3);",
                expected = Datum.varchar("ell", 5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "substring: value padded to n, extraction past content is trailing space",
                input = "substring(CAST('hi' AS CHAR(5)) FROM 2 FOR 3);",
                expected = Datum.varchar("i  ", 5),
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
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CHAR truncates 'hello world' to 'hello', so 'w' is no longer present (0). Content shorter
            // than n is space-padded, so a probe still matches within the original characters.
            SuccessTestCase(
                name = "position: value truncated to n so probe past n is not found",
                input = "position('w' IN CAST('hello world' AS CHAR(5)));",
                expected = Datum.bigint(0),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "position: value padded to n still matches original characters",
                input = "position('i' IN CAST('hi' AS CHAR(5)));",
                expected = Datum.bigint(2),
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
         * `LOWER(value)` preserves the input type and length: CHAR(n) -> CHAR(n),
         * VARCHAR(n) -> VARCHAR(n), CLOB(n) -> CLOB(n), STRING -> STRING.
         */
        @JvmStatic
        fun lowerTestCases() = listOf(
            // --- Behavior ---
            SuccessTestCase(
                name = "lower: basic",
                input = "lower('HELLO');",
                expected = Datum.string("hello"),
            ),
            SuccessTestCase(
                name = "lower: mixed case",
                input = "lower('HeLLo WoRLd');",
                expected = Datum.string("hello world"),
            ),
            SuccessTestCase(
                name = "lower: already lowercase is unchanged",
                input = "lower('hello');",
                expected = Datum.string("hello"),
            ),
            SuccessTestCase(
                name = "lower: non-alphabetic characters are unchanged",
                input = "lower('A1-B2!');",
                expected = Datum.string("a1-b2!"),
            ),
            SuccessTestCase(
                name = "lower: empty string",
                input = "lower('');",
                expected = Datum.string(""),
            ),
            // --- Argument-type matrix (type and length are preserved; STRICT mode) ---
            SuccessTestCase(
                name = "lower: STRING",
                input = "lower(CAST('HI' AS STRING));",
                expected = Datum.string("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "lower: CHAR preserves CHAR(n)",
                input = "lower(CAST('HI' AS CHAR(4)));",
                expected = Datum.character("hi", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "lower: VARCHAR preserves VARCHAR(n)",
                input = "lower(CAST('HI' AS VARCHAR(4)));",
                expected = Datum.varchar("hi", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "lower: CLOB preserves CLOB",
                input = "lower(CAST('HI' AS CLOB));",
                expected = Datum.clob("hi".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST truncates content longer than n and space-pads content shorter than n before LOWER
            // runs; LOWER preserves the CHAR(n) type. CLOB(n) does not enforce n.
            SuccessTestCase(
                name = "lower: CHAR content longer than n is truncated to n",
                input = "lower(CAST('HELLO WORLD' AS CHAR(5)));",
                expected = Datum.character("hello", 5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "lower: CHAR content shorter than n is padded to n",
                input = "lower(CAST('HI' AS CHAR(5)));",
                expected = Datum.character("hi", 5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "lower: CLOB does not enforce n; keeps full content",
                input = "lower(CAST('HELLO WORLD' AS CLOB(5)));",
                expected = Datum.clob("hello world".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // --- Null / missing propagation (untyped NULL resolves via the UNKNOWN instance) ---
            SuccessTestCase(
                name = "lower: null returns null",
                input = "lower(NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "lower: typed null returns null",
                input = "lower(CAST(NULL AS STRING));",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "lower: missing returns missing",
                input = "lower(MISSING);",
                expected = Datum.missing(),
            ),
        )

        /**
         * `UPPER(value)` preserves the input type and length: CHAR(n) -> CHAR(n),
         * VARCHAR(n) -> VARCHAR(n), CLOB(n) -> CLOB(n), STRING -> STRING.
         */
        @JvmStatic
        fun upperTestCases() = listOf(
            // --- Behavior ---
            SuccessTestCase(
                name = "upper: basic",
                input = "upper('hello');",
                expected = Datum.string("HELLO"),
            ),
            SuccessTestCase(
                name = "upper: mixed case",
                input = "upper('HeLLo WoRLd');",
                expected = Datum.string("HELLO WORLD"),
            ),
            SuccessTestCase(
                name = "upper: already uppercase is unchanged",
                input = "upper('HELLO');",
                expected = Datum.string("HELLO"),
            ),
            SuccessTestCase(
                name = "upper: non-alphabetic characters are unchanged",
                input = "upper('a1-b2!');",
                expected = Datum.string("A1-B2!"),
            ),
            SuccessTestCase(
                name = "upper: empty string",
                input = "upper('');",
                expected = Datum.string(""),
            ),
            // --- Argument-type matrix (type and length are preserved; STRICT mode) ---
            SuccessTestCase(
                name = "upper: STRING",
                input = "upper(CAST('hi' AS STRING));",
                expected = Datum.string("HI"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "upper: CHAR preserves CHAR(n)",
                input = "upper(CAST('hi' AS CHAR(4)));",
                expected = Datum.character("HI", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "upper: VARCHAR preserves VARCHAR(n)",
                input = "upper(CAST('hi' AS VARCHAR(4)));",
                expected = Datum.varchar("HI", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "upper: CLOB preserves CLOB",
                input = "upper(CAST('hi' AS CLOB));",
                expected = Datum.clob("HI".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST truncates content longer than n and space-pads content shorter than n before UPPER
            // runs; UPPER preserves the VARCHAR(n) type. CLOB(n) does not enforce n.
            SuccessTestCase(
                name = "upper: VARCHAR content longer than n is truncated to n",
                input = "upper(CAST('hello world' AS VARCHAR(5)));",
                expected = Datum.varchar("HELLO", 5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "upper: VARCHAR content shorter than n is padded to n",
                input = "upper(CAST('hi' AS VARCHAR(5)));",
                expected = Datum.varchar("HI", 5),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "upper: CLOB does not enforce n; keeps full content",
                input = "upper(CAST('hello world' AS CLOB(5)));",
                expected = Datum.clob("HELLO WORLD".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // --- Null / missing propagation (untyped NULL resolves via the UNKNOWN instance) ---
            SuccessTestCase(
                name = "upper: null returns null",
                input = "upper(NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "upper: typed null returns null",
                input = "upper(CAST(NULL AS STRING));",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "upper: missing returns missing",
                input = "upper(MISSING);",
                expected = Datum.missing(),
            ),
        )

        /**
         * `lhs || rhs`. The result type follows the coercibility order STRING > CLOB > VARCHAR > CHAR,
         * and the result length is the sum of the operand lengths.
         */
        @JvmStatic
        fun concatTestCases() = listOf(
            // --- Behavior ---
            SuccessTestCase(
                name = "concat: basic",
                input = "'hello' || ' world';",
                expected = Datum.string("hello world"),
            ),
            SuccessTestCase(
                name = "concat: empty lhs",
                input = "'' || 'abc';",
                expected = Datum.string("abc"),
            ),
            SuccessTestCase(
                name = "concat: empty rhs",
                input = "'abc' || '';",
                expected = Datum.string("abc"),
            ),
            SuccessTestCase(
                name = "concat: chained",
                input = "'a' || 'b' || 'c';",
                expected = Datum.string("abc"),
            ),
            SuccessTestCase(
                name = "concat: multi-byte value preserves surrogate-pair codepoints",
                input = "'👍' || 'a';",
                expected = Datum.string("👍a"),
            ),
            // --- Argument-type matrix; result type follows STRING > CLOB > VARCHAR > CHAR ---
            SuccessTestCase(
                name = "concat: STRING || STRING is STRING",
                input = "CAST('a' AS STRING) || CAST('b' AS STRING);",
                expected = Datum.string("ab"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "concat: CHAR || CHAR is CHAR with summed length",
                input = "CAST('a' AS CHAR(1)) || CAST('b' AS CHAR(2));",
                expected = Datum.character("ab ", 3),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "concat: VARCHAR || VARCHAR is VARCHAR with summed length",
                input = "CAST('a' AS VARCHAR(1)) || CAST('b' AS VARCHAR(2));",
                expected = Datum.varchar("ab", 3),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "concat: VARCHAR || CHAR is VARCHAR",
                input = "CAST('a' AS VARCHAR(1)) || CAST('b' AS CHAR(1));",
                expected = Datum.varchar("ab", 2),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "concat: CLOB || CLOB is CLOB with summed length",
                input = "CAST('a' AS CLOB(1)) || CAST('b' AS CLOB(2));",
                expected = Datum.clob("ab".toByteArray(), 3),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                // Both operands carry the maximum CLOB length, so the summed length clamps back to the
                // maximum rather than overflowing. This used to fail as a length overflow.
                name = "concat: unbounded CLOB || CLOB clamps length to the maximum",
                input = "CAST('a' AS CLOB) || CAST('b' AS CLOB);",
                expected = Datum.clob("ab".toByteArray(), Int.MAX_VALUE),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                // replace's result type is unbounded VARCHAR (its length is not computable at plan
                // time), so concatenating onto it clamps to the maximum instead of overflowing.
                name = "concat: unbounded replace result || VARCHAR clamps length to the maximum",
                input = "replace(CAST('abc' AS VARCHAR(5)), 'a', 'z') || CAST('x' AS VARCHAR(1));",
                expected = Datum.varchar("zbcx", Int.MAX_VALUE),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "concat: CLOB || VARCHAR is CLOB",
                input = "CAST('a' AS CLOB(1)) || CAST('b' AS VARCHAR(1));",
                expected = Datum.clob("ab".toByteArray(), 2),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "concat: STRING || CHAR is STRING",
                input = "CAST('a' AS STRING) || CAST('b' AS CHAR(1));",
                expected = Datum.string("ab"),
                mode = Mode.STRICT(),
            ),
            // --- Length boundary: operand content longer/shorter than its declared length ---
            // CHAR pads each operand to its own length before concatenation, so 'hi'/CHAR(5) contributes
            // 'hi   ' and 'yo'/CHAR(3) contributes 'yo ', giving CHAR(8) 'hi   yo '. Content longer than
            // the declared length is truncated first, so 'hello world'/CHAR(3) contributes only 'hel'.
            SuccessTestCase(
                name = "concat: CHAR operands shorter than n keep interior padding",
                input = "CAST('hi' AS CHAR(5)) || CAST('yo' AS CHAR(3));",
                expected = Datum.character("hi   yo ", 8),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "concat: CHAR operand longer than n is truncated before concatenation",
                input = "CAST('hello world' AS CHAR(3)) || CAST('x' AS CHAR(1));",
                expected = Datum.character("helx", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "concat: VARCHAR operand longer than n is truncated before concatenation",
                input = "CAST('hello world' AS VARCHAR(3)) || CAST('x' AS VARCHAR(1));",
                expected = Datum.varchar("helx", 4),
                mode = Mode.STRICT(),
            ),
            // --- Null / missing propagation (untyped NULL resolves via the UNKNOWN instance) ---
            SuccessTestCase(
                name = "concat: null lhs returns null",
                input = "NULL || 'a';",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "concat: null rhs returns null",
                input = "'a' || NULL;",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "concat: both null returns null",
                input = "NULL || NULL;",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "concat: typed null returns null",
                input = "CAST(NULL AS STRING) || 'a';",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "concat: missing lhs returns missing",
                input = "MISSING || 'a';",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "concat: missing rhs returns missing",
                input = "'a' || MISSING;",
                expected = Datum.missing(),
            ),
        )

        /**
         * `TRIM(value)` / `TRIM(BOTH FROM value)`. Unlike the length-changing string functions, TRIM
         * preserves the input length: CHAR(n)/VARCHAR(n) -> VARCHAR(n), CLOB(n) -> CLOB(n),
         * STRING -> STRING.
         */
        @JvmStatic
        fun trimTestCases() = listOf(
            // --- Behavior ---
            SuccessTestCase(
                name = "trim: both sides",
                input = "TRIM('  hi  ');",
                expected = Datum.string("hi"),
            ),
            SuccessTestCase(
                name = "trim: leading only",
                input = "TRIM('  hi');",
                expected = Datum.string("hi"),
            ),
            SuccessTestCase(
                name = "trim: trailing only",
                input = "TRIM('hi  ');",
                expected = Datum.string("hi"),
            ),
            SuccessTestCase(
                name = "trim: interior whitespace is preserved",
                input = "TRIM('  a b  ');",
                expected = Datum.string("a b"),
            ),
            SuccessTestCase(
                name = "trim: no whitespace is unchanged",
                input = "TRIM('hi');",
                expected = Datum.string("hi"),
            ),
            SuccessTestCase(
                name = "trim: all whitespace becomes empty",
                input = "TRIM('   ');",
                expected = Datum.string(""),
            ),
            SuccessTestCase(
                name = "trim: BOTH FROM is the same as the one-argument form",
                input = "TRIM(BOTH FROM '  hi  ');",
                expected = Datum.string("hi"),
            ),
            // --- Argument-type matrix (length is preserved; STRICT mode) ---
            SuccessTestCase(
                name = "trim: STRING",
                input = "TRIM(CAST('  hi  ' AS STRING));",
                expected = Datum.string("hi"),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim: CHAR returns VARCHAR(n)",
                input = "TRIM(CAST('  hi  ' AS CHAR(6)));",
                expected = Datum.varchar("hi", 6),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim: VARCHAR returns VARCHAR(n)",
                input = "TRIM(CAST('  hi  ' AS VARCHAR(6)));",
                expected = Datum.varchar("hi", 6),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim: CLOB returns CLOB",
                input = "TRIM(CAST('  hi  ' AS CLOB));",
                expected = Datum.clob("hi".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST truncates before TRIM runs: '  hi  ' truncated to CHAR(3) = '  h', trimmed = 'h';
            // '  hello world  ' truncated to VARCHAR(4) = '  he', trimmed = 'he'. Content shorter than n
            // is space-padded, which TRIM then removes.
            SuccessTestCase(
                name = "trim: CHAR content longer than n is truncated before trimming",
                input = "TRIM(CAST('  hi  ' AS CHAR(3)));",
                expected = Datum.varchar("h", 3),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim: VARCHAR content longer than n is truncated before trimming",
                input = "TRIM(CAST('  hello world  ' AS VARCHAR(4)));",
                expected = Datum.varchar("he", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim: CHAR content shorter than n has its padding trimmed away",
                input = "TRIM(CAST('hi' AS CHAR(6)));",
                expected = Datum.varchar("hi", 6),
                mode = Mode.STRICT(),
            ),
            // --- Null / missing propagation (untyped NULL resolves via the UNKNOWN instance) ---
            SuccessTestCase(
                name = "trim: null returns null",
                input = "TRIM(NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim: BOTH FROM null returns null",
                input = "TRIM(BOTH FROM NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim: typed null returns null",
                input = "TRIM(CAST(NULL AS STRING));",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "trim: missing returns missing",
                input = "TRIM(MISSING);",
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
                name = "trim_leading: CHAR returns VARCHAR(n)",
                input = "TRIM(LEADING FROM CAST('  hi' AS CHAR(4)));",
                expected = Datum.varchar("hi", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_leading: VARCHAR",
                input = "TRIM(LEADING FROM CAST('  hi' AS VARCHAR(4)));",
                expected = Datum.varchar("hi", 4),
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
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST truncates before trimming: '  hello world' truncated to CHAR(4) = '  he', leading
            // trimmed = 'he'. Content shorter than n is space-padded, which becomes leading/trailing
            // space that trimming removes.
            SuccessTestCase(
                name = "trim_leading: CHAR content longer than n is truncated before trimming",
                input = "TRIM(LEADING FROM CAST('  hello world' AS CHAR(4)));",
                expected = Datum.varchar("he", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                // The CHAR(6) cast pads '  hi' to '  hi  '; leading-trim removes only the leading spaces,
                // so the trailing padding remains in the result.
                name = "trim_leading: CHAR content shorter than n has only its leading padding trimmed",
                input = "TRIM(LEADING FROM CAST('  hi' AS CHAR(6)));",
                expected = Datum.varchar("hi  ", 6),
                mode = Mode.STRICT(),
            ),
            // --- `chars` keeps its own text type (no coercion to STRING) ---
            SuccessTestCase(
                name = "trim_leading_chars: CLOB chars",
                input = "TRIM(LEADING CAST('x' AS CLOB) FROM CAST('xxhi' AS VARCHAR(4)));",
                expected = Datum.varchar("hi", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_leading_chars: VARCHAR chars",
                input = "TRIM(LEADING CAST('x' AS VARCHAR(1)) FROM CAST('xxhi' AS CLOB));",
                expected = Datum.clob("hi".toByteArray()),
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
                name = "trim_trailing: CHAR returns VARCHAR(n)",
                input = "TRIM(TRAILING FROM CAST('hi  ' AS CHAR(4)));",
                expected = Datum.varchar("hi", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_trailing: VARCHAR",
                input = "TRIM(TRAILING FROM CAST('hi  ' AS VARCHAR(4)));",
                expected = Datum.varchar("hi", 4),
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
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST truncates before trimming: 'hello world  ' truncated to CHAR(4) = 'hell' (no trailing
            // space left to trim). Content shorter than n is space-padded, which trailing-trim removes.
            SuccessTestCase(
                name = "trim_trailing: CHAR content longer than n is truncated before trimming",
                input = "TRIM(TRAILING FROM CAST('hello world  ' AS CHAR(4)));",
                expected = Datum.varchar("hell", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_trailing: CHAR content shorter than n has its trailing padding trimmed",
                input = "TRIM(TRAILING FROM CAST('hi' AS CHAR(6)));",
                expected = Datum.varchar("hi", 6),
                mode = Mode.STRICT(),
            ),
            // --- `chars` keeps its own text type (no coercion to STRING) ---
            SuccessTestCase(
                name = "trim_trailing_chars: CLOB chars",
                input = "TRIM(TRAILING CAST('x' AS CLOB) FROM CAST('hixx' AS VARCHAR(4)));",
                expected = Datum.varchar("hi", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_trailing_chars: VARCHAR chars",
                input = "TRIM(TRAILING CAST('x' AS VARCHAR(1)) FROM CAST('hixx' AS CLOB));",
                expected = Datum.clob("hi".toByteArray()),
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
                name = "trim_chars: CHAR returns VARCHAR(n)",
                input = "TRIM(BOTH 'x' FROM CAST('xxhixx' AS CHAR(6)));",
                expected = Datum.varchar("hi", 6),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_chars: VARCHAR",
                input = "TRIM(BOTH 'x' FROM CAST('xxhixx' AS VARCHAR(6)));",
                expected = Datum.varchar("hi", 6),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_chars: CLOB",
                input = "TRIM(BOTH 'x' FROM CAST('xxhixx' AS CLOB));",
                expected = Datum.clob("hi".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST truncates before trimming: 'xxhelloxx' truncated to CHAR(4) = 'xxhe', trimming 'x'
            // leaves 'he'. Content shorter than n is space-padded, and that padding survives in the
            // result because ' ' is not in the trimmed character set.
            SuccessTestCase(
                name = "trim_chars: CHAR content longer than n is truncated before trimming",
                input = "TRIM(BOTH 'x' FROM CAST('xxhelloxx' AS CHAR(4)));",
                expected = Datum.varchar("he", 4),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_chars: CHAR content shorter than n keeps padding not matching trim char",
                input = "TRIM(BOTH 'x' FROM CAST('xxhi' AS CHAR(6)));",
                expected = Datum.varchar("hi  ", 6),
                mode = Mode.STRICT(),
            ),
            // --- `chars` keeps its own text type (no coercion to STRING) ---
            SuccessTestCase(
                name = "trim_chars: CLOB chars",
                input = "TRIM(BOTH CAST('x' AS CLOB) FROM CAST('xxhixx' AS VARCHAR(6)));",
                expected = Datum.varchar("hi", 6),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "trim_chars: VARCHAR chars",
                input = "TRIM(BOTH CAST('x' AS VARCHAR(1)) FROM CAST('xxhixx' AS CLOB));",
                expected = Datum.clob("hi".toByteArray()),
                mode = Mode.STRICT(),
            ),
            // CHAR chars are space-padded by the CAST, so the padding widens the trimmed character
            // set: CHAR(3) 'x' is 'x  ', which trims both 'x' and ' '.
            SuccessTestCase(
                name = "trim_chars: CHAR chars padding widens the trimmed character set",
                input = "TRIM(BOTH CAST('x' AS CHAR(3)) FROM CAST('  xxhixx  ' AS STRING));",
                expected = Datum.string("hi"),
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
            // --- Length boundary: content longer/shorter than the declared type length ---
            // CAST truncates 'hello world' to CHAR(5) = 'hello', which matches the pattern 'hello'.
            // Content shorter than n is space-padded, so CHAR(5) <- 'hi' is 'hi   ' and does NOT match
            // the bare pattern 'hi'; a trailing '%' is needed to match the padding.
            SuccessTestCase(
                name = "like: CHAR value longer than n is truncated to n and matches",
                input = "CAST('hello world' AS CHAR(5)) LIKE 'hello';",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "like: CHAR value shorter than n is padded so bare pattern does not match",
                input = "CAST('hi' AS CHAR(5)) LIKE 'hi';",
                expected = Datum.bool(false),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                name = "like: CHAR value shorter than n matches with trailing wildcard",
                input = "CAST('hi' AS CHAR(5)) LIKE 'hi%';",
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
            // Untyped NULL/MISSING literals resolve as UNKNOWN, which the overload must accept so the
            // framework can propagate null/missing — rather than failing to resolve.
            SuccessTestCase(
                name = "like: untyped null value returns null",
                input = "NULL LIKE 'a%';",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "like: untyped null pattern returns null",
                input = "'abc' LIKE NULL;",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "like: untyped missing value returns missing",
                input = "MISSING LIKE 'a%';",
                expected = Datum.missing(),
            ),
            // The result of `NULL || VARCHAR(1)` is a typed null of type UNKNOWN; LIKE must resolve on
            // it just as it does for the other operand order. This is the reported regression.
            SuccessTestCase(
                name = "like: UNKNOWN-typed concat result value returns null",
                input = "(NULL || CAST('a' AS VARCHAR(1))) LIKE 'a';",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "like: VARCHAR concat result with trailing null returns null",
                input = "(CAST('a' AS VARCHAR(1)) || NULL) LIKE 'a';",
                expected = Datum.nullValue(),
            ),
            // like_escape null/missing propagation, including an untyped escape argument.
            SuccessTestCase(
                name = "like_escape: untyped null value returns null",
                input = "NULL LIKE 'a\\_c' ESCAPE '\\';",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "like_escape: untyped null escape returns null",
                input = "'a_c' LIKE 'a\\_c' ESCAPE NULL;",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "like_escape: untyped missing escape returns missing",
                input = "'a_c' LIKE 'a\\_c' ESCAPE MISSING;",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "like_escape: CLOB arguments match",
                input = "CAST('a_c' AS CLOB) LIKE CAST('a\\_c' AS CLOB) ESCAPE CAST('\\' AS CLOB);",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
            ),
        )
    }
}
