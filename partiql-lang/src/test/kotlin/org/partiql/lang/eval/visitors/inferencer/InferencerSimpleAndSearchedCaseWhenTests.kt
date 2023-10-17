package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.StaticType

class InferencerSimpleAndSearchedCaseWhenTests {
    @ParameterizedTest
    @MethodSource("parametersForSimpleAndSearchedCaseWhen")
    fun simpleAndSearchedCaseWhenTests(tc: TestCase) = runTest(tc)

    companion object {
        /**
         * Creates a SimpleCaseWhen and SearchedCaseWhen clause [TestCase] for testing the inferred static type of the
         * THEN and ELSE expression results.
         *
         * For each SimpleCaseWhen test, every `case-value` and `when-value` will be an integer.
         * For each SearchedCaseWhen test, every `when-predicate` will be `true`.
         *
         * The `then-result` types are specified by [thenTypes]. An `else-result` type can optionally be specified
         * by [elseType]. If no [elseType] is specified, the [TestCase] will not have an else clause. The resulting
         * SimpleCaseWhen and SearchedCaseWhen clause [TestCase] will have its output type checked with [expectedType].
         */
        private fun createSimpleAndSearchedCaseWhenTestCases(
            name: String,
            thenTypes: List<StaticType>,
            elseType: StaticType? = null,
            expectedType: StaticType
        ): List<TestCase> {
            val globals = mutableMapOf<String, StaticType>()
            var simpleCaseWhenQuery = "CASE 0\n"
            var searchedCaseWhenQuery = "CASE\n"

            thenTypes.mapIndexed { index, staticType ->
                simpleCaseWhenQuery += "WHEN $index THEN t_$index\n"
                searchedCaseWhenQuery += "WHEN true THEN t_$index\n"
                globals.put("t_$index", staticType)
            }

            if (elseType != null) {
                val elseClause = "ELSE t_${thenTypes.size}\n"
                simpleCaseWhenQuery += elseClause
                searchedCaseWhenQuery += elseClause
                globals["t_${thenTypes.size}"] = elseType
            }
            simpleCaseWhenQuery += "END"
            searchedCaseWhenQuery += "END"

            return listOf(
                TestCase(
                    name = "SimpleCaseWhen $name",
                    originalSql = simpleCaseWhenQuery,
                    globals = globals,
                    handler = expectQueryOutputType(expectedType)
                ),
                TestCase(
                    name = "SearchedCaseWhen $name",
                    originalSql = searchedCaseWhenQuery,
                    globals = globals,
                    handler = expectQueryOutputType(expectedType)
                )
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun parametersForSimpleAndSearchedCaseWhen() = listOf(
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of INT",
                thenTypes = listOf(StaticType.INT, StaticType.INT, StaticType.INT),
                elseType = StaticType.INT,
                expectedType = StaticType.INT
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of INT",
                thenTypes = listOf(StaticType.INT, StaticType.INT, StaticType.INT),
                expectedType = StaticType.unionOf(StaticType.INT, StaticType.NULL)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of mixed known types",
                thenTypes = listOf(StaticType.INT, StaticType.STRING, StaticType.TIMESTAMP),
                elseType = StaticType.CLOB,
                expectedType = StaticType.unionOf(
                    StaticType.INT,
                    StaticType.STRING,
                    StaticType.TIMESTAMP,
                    StaticType.CLOB
                )
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of mixed known types",
                thenTypes = listOf(StaticType.INT, StaticType.STRING, StaticType.TIMESTAMP),
                expectedType = StaticType.unionOf(
                    StaticType.INT,
                    StaticType.STRING,
                    StaticType.TIMESTAMP,
                    StaticType.NULL
                )
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of mixed known union types",
                thenTypes = listOf(
                    StaticType.unionOf(StaticType.INT, StaticType.DECIMAL),
                    StaticType.STRING,
                    StaticType.TIMESTAMP
                ),
                elseType = StaticType.CLOB,
                expectedType = StaticType.unionOf(
                    StaticType.INT,
                    StaticType.DECIMAL,
                    StaticType.STRING,
                    StaticType.TIMESTAMP,
                    StaticType.CLOB
                )
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of mixed known union types",
                thenTypes = listOf(
                    StaticType.unionOf(StaticType.INT, StaticType.DECIMAL),
                    StaticType.STRING,
                    StaticType.TIMESTAMP
                ),
                expectedType = StaticType.unionOf(
                    StaticType.INT,
                    StaticType.DECIMAL,
                    StaticType.STRING,
                    StaticType.TIMESTAMP,
                    StaticType.NULL
                )
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of unknown types",
                thenTypes = listOf(StaticType.NULL, StaticType.MISSING),
                elseType = StaticType.NULL_OR_MISSING,
                expectedType = StaticType.unionOf(StaticType.NULL, StaticType.MISSING)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of unknown types",
                thenTypes = listOf(StaticType.NULL, StaticType.MISSING),
                expectedType = StaticType.unionOf(StaticType.NULL, StaticType.MISSING)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of unknown types and ANY",
                thenTypes = listOf(StaticType.NULL, StaticType.MISSING, StaticType.ANY),
                elseType = StaticType.NULL_OR_MISSING,
                expectedType = StaticType.ANY
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of unknown types and ELSE of ANY",
                thenTypes = listOf(StaticType.NULL, StaticType.MISSING, StaticType.NULL_OR_MISSING),
                elseType = StaticType.ANY,
                expectedType = StaticType.ANY
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of unknown types and ANY",
                thenTypes = listOf(StaticType.NULL, StaticType.MISSING, StaticType.ANY),
                expectedType = StaticType.ANY
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of INT, MISSING, NULL, ELSE STRING",
                thenTypes = listOf(StaticType.INT, StaticType.MISSING, StaticType.NULL),
                elseType = StaticType.STRING,
                expectedType = StaticType.unionOf(
                    StaticType.INT,
                    StaticType.MISSING,
                    StaticType.NULL,
                    StaticType.STRING
                )
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of INT, MISSING, NULL",
                thenTypes = listOf(StaticType.INT, StaticType.MISSING, StaticType.NULL),
                expectedType = StaticType.unionOf(StaticType.INT, StaticType.MISSING, StaticType.NULL)
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "with ELSE, THEN of INT, MISSING, NULL, ELSE ANY",
                thenTypes = listOf(StaticType.INT, StaticType.MISSING, StaticType.NULL),
                elseType = StaticType.ANY,
                expectedType = StaticType.ANY
            ),
            createSimpleAndSearchedCaseWhenTestCases(
                name = "without ELSE, THEN of INT, MISSING, NULL, ELSE ANY",
                thenTypes = listOf(StaticType.INT, StaticType.MISSING, StaticType.NULL, StaticType.ANY),
                expectedType = StaticType.ANY
            )
        ).flatten()
    }
}
