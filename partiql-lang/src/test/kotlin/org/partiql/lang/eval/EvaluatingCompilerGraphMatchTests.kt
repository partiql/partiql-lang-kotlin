package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.ArgumentsProviderBase

class EvaluatingCompilerGraphMatchTests : EvaluatorTestBase() {

    private val session = sessionOf(
        graphs = mapOf(
            //      n1(a) ---e12[e]-->  n2(b) ---e23[d]--> n3(a)
            "g3aba" to """{ 
                 nodes: [ {id: n1, labels: ["a"], payload: 1}, 
                          {id: n2, labels: ["b"], payload: 2},
                          {id: n3, labels: ["a"], payload: 3} ],
                 edges: [ {id: e12, labels: ["e"], payload: 1.2, ends: (n1 -> n2) },
                          {id: e23, labels: ["d"], payload: 2.3, ends: (n2 -> n3) } ]
                 }""".trimMargin(),
        )
    )

    @ParameterizedTest
    @ArgumentsSource(GraphQueries::class)
    fun testGraphQueries(qr: Pair<String, String>) {
        val (query, result) = qr
        runEvaluatorTestCase(
            EvaluatorTestCase(
                query = query,
                expectedResult = result,
                targetPipeline = EvaluatorTestTarget.COMPILER_PIPELINE
            ),
            session
        )
    }

    class GraphQueries : ArgumentsProviderBase() {
        override fun getParameters(): List<Pair<String, String>> = listOf(
            "(g3aba MATCH (x:a))" to
                "<< {'x': 1}, {'x': 3} >>",
            " (g3aba MATCH (n:b))" to
                "<< {'n': 2} >>",
            "(g3aba MATCH -> )" to
                "<< {}, {} >>",
            "(g3aba MATCH <-[z]-> )" to
                "<< {'z': 1.2}, {'z': 1.2}, {'z': 2.3}, {'z': 2.3} >>",
            "(g3aba MATCH -[z:e]- )" to
                "<< {'z': 1.2}, {'z': 1.2} >>",
            "(g3aba MATCH ~[z:e]~ )" to
                "<<  >>",
            "(g3aba MATCH -[z:e]-> )" to
                "<< {'z': 1.2} >>",
            "(g3aba MATCH <-[z:e]- )" to
                "<< {'z': 1.2} >>",
            "(g3aba MATCH (x)-[z:e]->(y) )" to
                "<< {'x': 1, 'z': 1.2, 'y': 2} >>",
            "(g3aba MATCH (x)<-[z:e]-(y) )" to
                "<< {'x': 2, 'z': 1.2, 'y': 1} >>",
            "(g3aba MATCH (x:b)-[z1]-(y1:a)-[z2]-(y2:b) )" to
                """<< {'x': 2, 'z1': 1.2, 'y1': 1, 'z2': 1.2, 'y2': 2},
                          {'x': 2, 'z1': 2.3, 'y1': 3, 'z2': 2.3, 'y2': 2} >>""",
            "(g3aba MATCH (x1)-[z1]->(x2)-[z2]->(x3) )" to
                "<< { 'x1': 1, 'z1': 1.2, 'x2': 2, 'z2': 2.3, 'x3': 3} >>",
        )
    }
}
