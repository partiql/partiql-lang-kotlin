
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lakeformation.LakeFormationUtil
import org.partiql.lakeformation.datafilter.ColumnFilter
import org.partiql.lakeformation.datafilter.LakeFormationDataFilter
import org.partiql.lakeformation.datafilter.Table
import org.partiql.lakeformation.exception.LakeFormationQuerySemanticException
import org.partiql.lakeformation.exception.LakeFormationQueryUnsupportedException
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.util.stream.Stream
import kotlin.test.assertTrue

// TODO : More comprehensive testing may be required.
class LakeFormationDataFilterTest {
    companion object {
        val parser = PartiQLParserBuilder.standard().build()
        val basicComparisonOps = listOf("=", "!=", "<", "<=", ">", ">=")
    }

    //
    // Successful test cases
    //
    data class SuccessTestCase(
        val testName: String,
        val query: String,
        val expectedDataFilter: LakeFormationDataFilter
    )

    private class SuccessTestCasesProvider : ArgumentsProvider {

        val successTestCases = listOf(
            // From source
            SuccessTestCase(
                "Basic SELECT FROM",
                "SELECT a FROM b",
                LakeFormationDataFilter(
                    Table(listOf("b")),
                    ColumnFilter.includeColumnsFilter("a"),
                    null
                )
            ),
            SuccessTestCase(
                "Source is a path",
                "SELECT a FROM b.c.d",
                LakeFormationDataFilter(
                    Table(listOf("b", "c", "d")),
                    ColumnFilter.includeColumnsFilter("a"),
                    null
                )
            ),
            // Projection
            SuccessTestCase(
                "Multiple from source",
                "SELECT foo, bar FROM b.c.d",
                LakeFormationDataFilter(
                    Table(listOf("b", "c", "d")),
                    ColumnFilter.includeColumnsFilter("foo", "bar"),
                    null
                )
            ),
            // project *
            SuccessTestCase(
                "project *",
                "SELECT * FROM b.c.d",
                LakeFormationDataFilter(
                    Table(listOf("b", "c", "d")),
                    ColumnFilter.allColumnsFilter(),
                    null
                )
            ),
        ) +
            // basic comparison operator
            basicComparisonOps.map { op ->
                SuccessTestCase(
                    "basicComparison $op",
                    "SELECT a FROM b.c WHERE c $op 10",
                    LakeFormationDataFilter(
                        Table(listOf("b", "c")),
                        ColumnFilter.includeColumnsFilter("a"),
                        "c $op 10"
                    )
                )
            } +
            // function calls
            listOf(
                SuccessTestCase(
                    "in",
                    "SELECT a FROM b.c WHERE c in (1,2,3)",
                    LakeFormationDataFilter(
                        Table(listOf("b", "c")),
                        ColumnFilter.includeColumnsFilter("a"),
                        "c in (1,2,3)"
                    )
                ),
                SuccessTestCase(
                    "between",
                    "SELECT a FROM b.c WHERE c between 1 and 3",
                    LakeFormationDataFilter(
                        Table(listOf("b", "c")),
                        ColumnFilter.includeColumnsFilter("a"),
                        "c between 1 and 3"
                    )
                ),
                SuccessTestCase(
                    "like",
                    "SELECT a FROM b.c WHERE c LIKE '%ABC%'",
                    LakeFormationDataFilter(
                        Table(listOf("b", "c")),
                        ColumnFilter.includeColumnsFilter("a"),
                        "c LIKE '%ABC%'"
                    )
                ),
            )
        @Throws(Exception::class)
        override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments> {
            return successTestCases.map { Arguments.of(it) }.stream()
        }
    }

    @ParameterizedTest
    @ArgumentsSource(SuccessTestCasesProvider::class)
    fun successTest(tc: SuccessTestCase) {
        val dataFilter = LakeFormationUtil.extractDataCell(tc.query, parser)
        assertTrue(
            assertDataFilterEqual(tc.expectedDataFilter, dataFilter),
            """
                expect : ${tc.expectedDataFilter}
                actual : $dataFilter
            """.trimIndent()
        )
    }

    private fun assertDataFilterEqual(expected: LakeFormationDataFilter, actual: LakeFormationDataFilter): Boolean {
        return when {
            expected.sourceTable != actual.sourceTable -> false
            expected.columnFilter != actual.columnFilter -> false
            expected.rowFilter != null && actual.rowFilter != null -> {
                parser.parseAstStatement(expected.rowFilter!!) == parser.parseAstStatement(actual.rowFilter!!)
            }
            else -> expected.rowFilter == actual.rowFilter
        }
    }

    //
    // Expected Failure test cases
    //

    // Enum class to track Exception type.
    enum class ExceptionType {
        UNSUPPORTED,
        SEMANTIC
    }

    data class FailedTestCase(
        val testName: String,
        val query: String,
        val failureType: ExceptionType
    )

    private class FailedTestCasesProvider : ArgumentsProvider {
        val failedTestCases =
            // Basic unsupported features
            listOf(
                FailedTestCase(
                    "Unsupported - SELECT nested value",
                    "SELECT a.b FROM b",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - SELECT VALUE",
                    "SELECT VALUE a FROM b",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - PIVOT",
                    "PIVOT a AT b FROM c",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - ProjectAll",
                    "SELECT a.* FROM b",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - Sub-query in SELECT",
                    "SELECT (SELECT a FROM b)  from b",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - Join",
                    "SELECT a from b, c",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - Unpivot",
                    "SELECT a FROM UNPIVOT b AT c",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - Path wildcard",
                    "SELECT a from b[*]",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - Path Unpivot",
                    "SELECT a from b.*",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - Sub-query in FROM",
                    "SELECT a FROM SELECT b FROM c",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - FROM LET",
                    "SELECT a FROM b LET 1 as x",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - GROUP BY",
                    "SELECT a FROM b GROUP BY c",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - HAVING",
                    "SELECT a FROM b GROUP BY c HAVING d < 10",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - ORDER BY",
                    "SELECT a FROM b ORDER BY c",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - LIMIT",
                    "SELECT a FROM b LIMIT 10",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - OFFSET",
                    "SELECT VALUE a FROM b LIMIT 10 OFFSET 3",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - DML",
                    "INSERT INTO foo.bar VALUE 1",
                    ExceptionType.UNSUPPORTED
                ),
                FailedTestCase(
                    "Unsupported - DDL",
                    "CREATE TABLE test (id INT);",
                    ExceptionType.UNSUPPORTED
                ),

            ) + basicComparisonOps.map { op ->
                listOf(
                    FailedTestCase(
                        "Semantic - basic comparison operator $op lhs is literal not id",
                        "Select a FROM b.c WHERE 1 $op 2",
                        ExceptionType.SEMANTIC
                    ),
                    FailedTestCase(
                        "Semantic - basic comparison operator $op rhs is id not literal",
                        "Select a FROM b.c WHERE d $op e",
                        ExceptionType.SEMANTIC
                    ),
                )
            }.flatten() + listOf(
                // between
                FailedTestCase(
                    "Semantic - between operator: lhs is literal not id",
                    "Select a FROM b.c WHERE 1 BETWEEN 0 AND 2",
                    ExceptionType.SEMANTIC
                ),
                FailedTestCase(
                    "Semantic - between operator: rhs contains non-literal",
                    "Select a FROM b.c WHERE d BETWEEN e and 100",
                    ExceptionType.SEMANTIC
                ),

                // in
                FailedTestCase(
                    "Semantic - in operator:  lhs is literal not id",
                    "Select a FROM b.c WHERE 1 in (1,2,3)",
                    ExceptionType.SEMANTIC
                ),
                FailedTestCase(
                    "Semantic - in operator rhs contains non-list",
                    "Select a FROM b.c WHERE d in e",
                    ExceptionType.SEMANTIC
                ),
                FailedTestCase(
                    "Semantic - in operator rhs contains list which contains non lit element",
                    "Select a FROM b.c WHERE d in [1,2,e]",
                    ExceptionType.SEMANTIC
                ),

                // like
                FailedTestCase(
                    "Semantic - like operator:  lhs is literal not id",
                    "Select a FROM b.c WHERE 'ABC' LIKE '%ABC%'",
                    ExceptionType.SEMANTIC
                ),
                FailedTestCase(
                    "Semantic - like operator rhs contains non-literal",
                    "Select a FROM b.c WHERE d like e",
                    ExceptionType.SEMANTIC
                ),
                FailedTestCase(
                    "Semantic - in operator rhs contains non string",
                    "Select a FROM b.c WHERE d like 1 ",
                    ExceptionType.SEMANTIC
                ),
            )
        @Throws(Exception::class)
        override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments> {
            return failedTestCases.map { Arguments.of(it) }.stream()
        }
    }

    @ParameterizedTest
    @ArgumentsSource(FailedTestCasesProvider::class)
    fun failedTest(tc: FailedTestCase) {
        when (tc.failureType) {
            ExceptionType.UNSUPPORTED -> assertThrows<LakeFormationQueryUnsupportedException> {
                LakeFormationUtil.extractDataCell(tc.query, parser)
            }
            ExceptionType.SEMANTIC -> assertThrows<LakeFormationQuerySemanticException> {
                LakeFormationUtil.extractDataCell(tc.query, parser)
            }
        }
    }
}
