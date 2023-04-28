package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.util.ArgumentsProviderBase

/** Queries that demonstrate the "headstrong" optimization of SELECT items,
 *  implemented in [EvaluatingCompiler.compileProjectItemSource]. */
class EvaluatingCompilerSelectHeadstrongTest : EvaluatorTestBase() {

    private val session = mapOf(
        "data" to "[ {'id':1, 'num': 10}, {'id':2, 'num': 20}, {'id':3, 'num': 30} ]"
    ).toSession()

    @ParameterizedTest
    @ArgumentsSource(StableOptimTests::class)
    fun testIterStableOptimTest(tc: EvaluatorTestCase) {
        runEvaluatorTestCase(tc, session)
    }

    class StableOptimTests : ArgumentsProviderBase() {
        override fun getParameters(): List<EvaluatorTestCase> = listOf(
            EvaluatorTestCase( // `Z` is optimized
                "SELECT 'Z' AS name FROM << {'id': 'A'}, {'id': 'B'}, {'id': 'C'} >>",
                "<< {'name': 'Z'}, {'name': 'Z'}, {'name': 'Z'} >>"
            ),
            EvaluatorTestCase( // `id` is not optimized
                "SELECT id AS name FROM << {'id': 'A'}, {'id': 'B'}, {'id': 'C'} >>",
                "<< {'name': 'A'}, {'name': 'B'}, {'name': 'C'} >>"
            ),
            EvaluatorTestCase( // `t.id` is not optimized -- `t` is an iteration variable
                "SELECT t.id FROM data as t",
                "<< {'id': 1}, {'id': 2}, {'id': 3} >>"
            ),
            EvaluatorTestCase( // optimized for `5` and `COLL_COUNT(...)`
                """
                    SELECT COLL_COUNT('all', SELECT 5 FROM data AS d) as m
                    FROM data as t
                """.trimIndent(),
                "<< {'m': 3}, {'m': 3}, {'m': 3} >>"
            ),
            EvaluatorTestCase(
                // max(x.num) is not optimized -- it depends on the iteration variable x
                // inner SELECT is optimized -- its only free var is external 'data'
                """
                    SELECT (SELECT max(x.num) FROM data as x) as m
                    FROM data as t
                """.trimIndent(),
                "<< {'m': 30}, {'m': 30}, {'m': 30} >>"
            ),
            EvaluatorTestCase(
                // inner SELECT is optimized -- for the same reason as the previous test.
                // even though there is t in the outer query, it is shadowed by the inner t
                """
                    SELECT (SELECT max(t.num) FROM data as t) as m
                    FROM data as t
                """.trimIndent(),
                "<< {'m': 30}, {'m': 30}, {'m': 30} >>"
            ),
            EvaluatorTestCase(
                // inner SELECT is not optimized -- t is an iteration variable from the outer query, not shadowed this time
                """
                    SELECT (SELECT max(t.num) FROM data as x) as m
                    FROM data as t
                """.trimIndent(),
                "<< {'m': 10}, {'m': 20}, {'m': 30} >>"
            ),
            EvaluatorTestCase( // inner SELECT is not optimized -- `num` is looked up among struct fields
                """
                    SELECT (SELECT max(num) FROM data) as m
                    FROM data as t
                """.trimIndent(),
                "<< {'m': 30}, {'m': 30}, {'m': 30} >>"
            ),
        )
    }
}
