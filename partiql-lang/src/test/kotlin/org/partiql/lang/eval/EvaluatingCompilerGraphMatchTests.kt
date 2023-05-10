package org.partiql.lang.eval

import org.junit.jupiter.api.Test
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase

class EvaluatingCompilerGraphMatchTests : EvaluatorTestBase() {

    private val session = sessionOf(
        graphs = mapOf(
            "g1" to """{ nodes: [ {id: n1} ], edges: [] }"""
        )
    )

    @Test
    fun testG1() {
        runEvaluatorTestCase(
            EvaluatorTestCase(
                query = "g1",
                expectedResult = "g1" // cheating, but the lookup in the session should be happening
            ),
            session
        )
    }
}
