package org.partiql.lang.eval

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.propertyValueMapOf

class EvaluatingCompilerLimitTests : EvaluatorTestBase() {

    private val session = mapOf("foo" to "[ { a: 1 }, { a: 2 }, { a: 3 }, { a: 4 }, { a: 5 } ]").toSession()

    @Test
    fun `LIMIT 0 should return no results`() =
        runEvaluatorTestCase(
            "SELECT * FROM foo LIMIT 0",
            session,
            "$BAG_ANNOTATION::[]"
        )

    @Test
    fun `LIMIT 1 should return first result`() =
        runEvaluatorTestCase(
            "SELECT * FROM foo LIMIT 1",
            session,
            "$BAG_ANNOTATION::[{a:1}]"
        )

    @Test
    fun `LIMIT 2 should return first two results`() =
        runEvaluatorTestCase(
            "SELECT * FROM foo LIMIT 2",
            session,
            "$BAG_ANNOTATION::[{a:1},{a:2}]"
        )

    @Test
    fun `LIMIT 2^31 should return all results`() =
        runEvaluatorTestCase(
            "SELECT * FROM foo LIMIT ${Integer.MAX_VALUE}",
            session,
            "$BAG_ANNOTATION::[{a:1},{a:2},{a:3},{a:4},{a:5}]"
        )

    @Test
    fun `LIMIT 2^63 should return all results`() =
        runEvaluatorTestCase(
            "SELECT * FROM foo LIMIT ${Long.MAX_VALUE}",
            session,
            "$BAG_ANNOTATION::[{a:1},{a:2},{a:3},{a:4},{a:5}]"
        )

    @Test
    fun `LIMIT -1 should throw exception`() =
        runEvaluatorErrorTestCase(
            """ select * from <<1>> limit -1 """,
            ErrorCode.EVALUATOR_NEGATIVE_LIMIT,
            propertyValueMapOf(1, 29),
            target = EvaluatorTestTarget.COMPILER_PIPELINE, // planner & physical plan have no support LIMIT (yet)
        )

    @Test
    fun `non-integer value should throw exception`() =
        runEvaluatorErrorTestCase(
            """ select * from <<1>> limit 'this won''t work' """,
            ErrorCode.EVALUATOR_NON_INT_LIMIT_VALUE,
            propertyValueMapOf(1, 28, Property.ACTUAL_TYPE to "STRING")
        )

    @Test
    fun `LIMIT applied after GROUP BY`() =
        runEvaluatorTestCase(
            "SELECT g FROM `[{foo: 1, bar: 10}, {foo: 1, bar: 11}]` AS f GROUP BY f.foo GROUP AS g LIMIT 1",
            expectedResult = "$BAG_ANNOTATION::[{g:$BAG_ANNOTATION::[{f:{foo:1,bar:10}},{f:{foo:1,bar:11}}]}]",
            target = EvaluatorTestTarget.COMPILER_PIPELINE // planner & physical plan have no support for GROUP BY (yet)
        )
}
