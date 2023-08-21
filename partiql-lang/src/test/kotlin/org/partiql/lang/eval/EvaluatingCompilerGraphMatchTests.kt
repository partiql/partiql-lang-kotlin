package org.partiql.lang.eval

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.errors.ErrorCode
import org.partiql.lang.eval.evaluatortestframework.EvaluatorErrorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.ArgumentsProviderBase

class EvaluatingCompilerGraphMatchTests : EvaluatorTestBase() {

    // So far, graphs are only supported in COMPILER_PIPELINE
    private val currentPipeline = EvaluatorTestTarget.COMPILER_PIPELINE

    // A "template" for the subsequent parameterized tests.
    fun testGraphQueries(
        session: EvaluationSession,
        qr: Pair<String, String>
    ) {
        val (query, result) = qr
        runEvaluatorTestCase(
            EvaluatorTestCase(
                query = query,
                expectedResult = result,
                targetPipeline = currentPipeline
            ),
            session
        )
    }

    /******** Directionality ********/

    val sessionDirectionality = sessionOf(
        graphs = mapOf(
            "pairs" to """{ nodes: [ {id: a1, labels:["a1"], payload: "A1"}, 
                |                    {id: b1, labels:["b1"], payload: "B1"},
                |                    {id: a2, labels:["a2"], payload: "A2"},
                |                    {id: b2, labels:["b2"], payload: "B2"} ], 
                |           edges: [ {id: d1, labels:["d1"], payload: "D1", ends: (a1 -> b1)},
                |                    {id: u2, labels:["u2"], payload: "U2", ends: (a2 -- b2)}  ] 
                |         }""".trimMargin()
        )
    )

    @ParameterizedTest
    @ArgumentsSource(Directionality::class)
    fun testDirectionality(qr: Pair<String, String>) {
        testGraphQueries(sessionDirectionality, qr)
    }

    class Directionality : ArgumentsProviderBase() {
        override fun getParameters(): List<Pair<String, String>> = listOf(
            "(pairs MATCH (x)-[y]->(z))" to """<< {'x': 'A1', 'y': 'D1', 'z': 'B1'} >>""",
            "(pairs MATCH ()-[]->())" to """<< {} >>""",
            "(pairs MATCH -> )" to """<< {} >>""",

            "(pairs MATCH (x)<-[y]-(z))" to """<< {'x': 'B1', 'y': 'D1', 'z': 'A1'} >>""",
            "(pairs MATCH ()<-[]-())" to """<< {} >>""",
            "(pairs MATCH <- )" to """<< {} >>""",

            "(pairs MATCH (x)<-[y]->(z))" to """<< {'x': 'A1', 'y': 'D1', 'z': 'B1'}, 
                |                                  {'x': 'B1', 'y': 'D1', 'z': 'A1'} >>""".trimMargin(),
            "(pairs MATCH ()<-[]->())" to """<< {}, {} >>""",
            "(pairs MATCH <-> )" to """<< {}, {} >>""",
            "(pairs MATCH (x:a1)<-[y]->(z))" to """<< {'x': 'A1', 'y': 'D1', 'z': 'B1'} >> """,

            "(pairs MATCH (x)~[y]~(z))" to """<< {'x': 'A2', 'y': 'U2', 'z': 'B2'}, 
                |                                {'x': 'B2', 'y': 'U2', 'z': 'A2'} >>""".trimMargin(),
            "(pairs MATCH ()~[]~())" to """<< {}, {} >>""",
            "(pairs MATCH ~ )" to """<< {}, {} >>""",
            "(pairs MATCH (x)~[y]~(z:b2))" to """<< {'x': 'A2', 'y': 'U2', 'z': 'B2'} >> """,

            "(pairs MATCH (x)~[y]~>(z))" to """<< {'x': 'A1', 'y': 'D1', 'z': 'B1'},
                |                                 {'x': 'A2', 'y': 'U2', 'z': 'B2'}, 
                |                                 {'x': 'B2', 'y': 'U2', 'z': 'A2'} >>""".trimMargin(),
            "(pairs MATCH ()~[]~>())" to """<< {}, {}, {} >>""",
            "(pairs MATCH ~> )" to """<< {}, {}, {} >>""",
            "(pairs MATCH (x:a1)~[y]~>(z:a2))" to """<< >>""",

            "(pairs MATCH (x)<~[y]~(z))" to """<< {'x': 'B1', 'y': 'D1', 'z': 'A1'},
                |                                 {'x': 'A2', 'y': 'U2', 'z': 'B2'}, 
                |                                 {'x': 'B2', 'y': 'U2', 'z': 'A2'} >>""".trimMargin(),
            "(pairs MATCH ()<~[]~())" to """<< {}, {}, {} >>""",
            "(pairs MATCH <~ )" to """<< {}, {}, {} >>""",
            "(pairs MATCH (x)<~[y:u2]~(z))" to """<< 
                |                                 {'x': 'A2', 'y': 'U2', 'z': 'B2'}, 
                |                                 {'x': 'B2', 'y': 'U2', 'z': 'A2'} >>""".trimMargin(),

            "(pairs MATCH (x)-[y]-(z))" to """<< {'x': 'A1', 'y': 'D1', 'z': 'B1'}, 
                |                                {'x': 'B1', 'y': 'D1', 'z': 'A1'},
                |                                {'x': 'A2', 'y': 'U2', 'z': 'B2'}, 
                |                                {'x': 'B2', 'y': 'U2', 'z': 'A2'} >>""".trimMargin(),
            "(pairs MATCH ()-[]-())" to """<< {}, {}, {}, {} >>""",
            "(pairs MATCH - )" to """<< {}, {}, {}, {} >>""",
        )
    }

    /******** SmallGraphs ********/

    val sessionSmallGraphs = sessionOf(
        graphs = mapOf(
            // The empty graph
            "N0E0" to """{ nodes: [], edges: [] }""",
            // n1  -- one solitary node
            "N1E0" to """{ nodes: [ {id: n1, payload: 1} ], edges: [] }""",
            // n1  -- a single node with a directed self-loop edge
            "N1D1" to """{ nodes: [ {id: n1, payload: 1} ], 
                |          edges: [ {id: d1, payload: 1.1, ends: (n1 -> n1) } ] }""".trimMargin(),
            // n1 -- a single node with an undirected self-loop
            "N1U1" to """{ nodes: [ {id: n1, payload: 1} ], 
                |          edges: [ {id: u1, payload: 1.1, ends: (n1 -- n1)} ] }""".trimMargin(),
            // n1  -- a single node with two self-loop edges
            "N1D2" to """{ nodes: [ {id: n1, payload: 1} ], 
                |          edges: [ {id: d1, payload: 1.1, ends: (n1 -> n1) },
                |                   {id: d2, payload: 11.11, ends: (n1 -> n1)} ] }""".trimMargin(),
            // n1  n2  -- two disconnected nodes
            "N2E0" to """{ nodes: [ {id: n1, payload: 1}, {id: n2, payload: 2} ], edges: [] }""",
            // n1 -[d1]-> n2
            "N2D1" to """{ nodes: [ {id: n1, payload: 1}, {id: n2, payload: 2} ], 
                |          edges: [ {id: d1, payload: 1.2, ends: (n1 -> n2) } ] }""".trimMargin(),
            // n1 -[u1]- n2
            "N2U1" to """{ nodes: [ {id: n1, payload: 1}, {id: n2, payload: 2} ], 
                |          edges: [ {id: u1, payload: 1.2, ends: (n1 -- n2) } ] }""".trimMargin(),
            // n1 -[d1]-> n2   --- two parallel edges
            //    -[d2]->
            "N2D2" to """{ nodes: [ {id: n1, payload: 1}, {id: n2, payload: 2} ], 
                |          edges: [ {id: d1, payload: 1.2,   ends: (n1 -> n2) }, 
                |                   {id: d2, payload: 11.22, ends: (n1 -> n2) } ] }""".trimMargin(),
            // n1 -[d1]-> n2   --- two cycling edges
            //    <-[d2]-
            "N2D2c" to """{ nodes: [ {id: n1, payload: 1}, {id: n2, payload: 2} ], 
                |           edges: [ {id: d1, payload: 1.2, ends: (n1 -> n2) },
                |                    {id: d2, payload: 2.1, ends: (n2 -> n1) } ] }""".trimMargin(),
            // n1 -[u1]- n2   --- two parallel undirected edges
            //    -[u2]-
            "N2U2" to """{ nodes: [ {id: n1, payload: 1}, {id: n2, payload: 2} ], 
                |          edges: [ {id: u1, payload: 1.2, ends: (n1 -- n2) },
                |                   {id: u2, payload: 2.1, ends: (n2 -- n1) } ] }""".trimMargin(),
        )
    )

    @ParameterizedTest
    @ArgumentsSource(SmallGraphs::class)
    fun testSmallGraphs(qr: Pair<String, String>) {
        testGraphQueries(sessionSmallGraphs, qr)
    }

    class SmallGraphs : ArgumentsProviderBase() {
        override fun getParameters(): List<Pair<String, String>> = listOf(
            "(N0E0 MATCH (x))" to "<< >>",
            "(N0E0 MATCH -[y]-> )" to "<< >>",
            "(N0E0 MATCH (x)-[y]->(z) )" to "<< >>",

            "(N1E0 MATCH (x))" to "<< {'x':1} >>",
            "(N1E0 MATCH -[y]-> )" to "<< >>",
            "(N1E0 MATCH (x)-[y]->(z) )" to "<< >>",
            "(N1E0 MATCH (x)-[y]->(x) )" to "<< >>",

            "(N1D1 MATCH (x))" to "<< {'x':1} >>",
            "(N1D1 MATCH -[y]-> )" to "<< {'y':1.1} >>",
            "(N1D1 MATCH (x)-[y]->(z) )" to "<< {'x':1, 'y':1.1, 'z':1} >>",
            "(N1D1 MATCH (x)-[y]->(x) )" to "<< {'x':1, 'y':1.1} >>",
            "(N1D1 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )" to "<< {'x1':1, 'y1':1.1, 'x2':1, 'y2':1.1, 'x3':1} >>",

            "(N1U1 MATCH (x))" to "<< {'x':1} >>",
            "(N1U1 MATCH ~[y]~ )" to "<< {'y':1.1} >>",
            "(N1U1 MATCH (x)~[y]~(z) )" to "<< {'x':1, 'y':1.1, 'z':1} >>",
            "(N1U1 MATCH (x)~[y]~(x) )" to "<< {'x':1, 'y':1.1} >>",
            "(N1U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )" to "<< {'x1':1, 'y1':1.1, 'x2':1, 'y2':1.1, 'x3':1} >>",

            "(N1D2 MATCH (x))" to "<< {'x':1} >>",
            "(N1D2 MATCH -[y]-> )" to "<< {'y':1.1}, {'y':11.11} >>",
            "(N1D2 MATCH (x)-[y]->(z) )" to "<< {'x':1, 'y':1.1, 'z':1}, {'x':1, 'y':11.11, 'z':1} >>",
            "(N1D2 MATCH (x)-[y]->(x) )" to "<< {'x':1, 'y':1.1}, {'x':1, 'y':11.11} >>",
            "(N1D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )" to
                """<< {'x1':1, 'y1':1.1,   'x2':1, 'y2':1.1,   'x3':1}, 
                    | {'x1':1, 'y1':1.1,   'x2':1, 'y2':11.11, 'x3':1},
                    | {'x1':1, 'y1':11.11, 'x2':1, 'y2':1.1,   'x3':1},
                    | {'x1':1, 'y1':11.11, 'x2':1, 'y2':11.11, 'x3':1} >>""".trimMargin(),

            "(N2E0 MATCH (x))" to "<< {'x':1}, {'x':2} >>",
            "(N2E0 MATCH -[y]-> )" to "<< >>",
            "(N2E0 MATCH (x)-[y]->(z) )" to "<< >>",
            "(N2E0 MATCH (x)-[y]->(x) )" to "<< >>",

            "(N2D1 MATCH (x))" to "<< {'x':1}, {'x':2} >>",
            "(N2D1 MATCH -[y]-> )" to "<< {'y':1.2} >>",
            "(N2D1 MATCH (x)-[y]->(z) )" to "<< {'x':1, 'y':1.2, 'z':2} >>",
            "(N2D1 MATCH (x)-[y]->(x) )" to "<< >>",
            "(N2D1 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )" to "<<  >>",
            "(N2D1 MATCH (x1)-[y1]->(x2)-[y2]-(x3) )" to "<< {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1} >>",
            "(N2D1 MATCH (x1)-[y1]-(x2)-[y2]->(x3) )" to "<< {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2} >>",
            "(N2D1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )" to
                """<< {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1},
                    | {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2} >>""".trimMargin(),

            "(N2U1 MATCH (x))" to "<< {'x':1}, {'x':2} >>",
            "(N2U1 MATCH ~[y]~ )" to "<< {'y':1.2}, {'y':1.2} >>", // duplicated! -- erasure of the next test
            "(N2U1 MATCH (x)~[y]~(z) )" to "<< {'x':1, 'y':1.2, 'z':2}, {'x':2, 'y':1.2, 'z':1} >>",
            "(N2U1 MATCH (x)~[y]~(x) )" to "<< >>",
            "(N2U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )" to
                """<< {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1},
                        | {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2}>>""".trimMargin(),
            "(N2U1 MATCH (x1)~[y1]~(x2)-[y2]-(x3) )" to
                """<< {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1},
                        | {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2}>>""".trimMargin(),
            "(N2U1 MATCH (x1)-[y1]-(x2)~[y2]~(x3) )" to
                """<< {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1},
                        | {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2}>>""".trimMargin(),
            "(N2U1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )" to
                """<< {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1},
                        | {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2}>>""".trimMargin(),

            "(N2D2 MATCH (x))" to "<< {'x':1}, {'x':2} >>",
            "(N2D2 MATCH -[y]-> )" to "<< {'y':1.2}, {'y':11.22} >>",
            "(N2D2 MATCH (x)-[y]->(z) )" to "<< {'x':1, 'y':1.2, 'z':2}, {'x':1, 'y':11.22, 'z':2} >>",
            "(N2D2 MATCH (x)-[y]->(x) )" to "<< >>",
            "(N2D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) )" to "<<  >>",
            "(N2D2 MATCH (x1)-[y1]->(x2)-[y2]-(x3) )" to
                """<< {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1}, 
                    | {'x1':1, 'y1':1.2, 'x2':2, 'y2':11.22, 'x3':1}, 
                    | {'x1':1, 'y1':11.22, 'x2':2, 'y2':1.2, 'x3':1},
                    | {'x1':1, 'y1':11.22, 'x2':2, 'y2':11.22, 'x3':1} >>""".trimMargin(),
            "(N2D2 MATCH (x1)-[y1]-(x2)-[y2]->(x3) )" to
                """<< {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2}, 
                    | {'x1':2, 'y1':1.2, 'x2':1, 'y2':11.22, 'x3':2}, 
                    | {'x1':2, 'y1':11.22, 'x2':1, 'y2':1.2, 'x3':2},
                    | {'x1':2, 'y1':11.22, 'x2':1, 'y2':11.22, 'x3':2} >>""".trimMargin(),
            "(N2D2 MATCH (x1)-[y1]-(x2)-[y2]-(x3) )" to
                """<< {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1}, 
                      | {'x1':1, 'y1':1.2, 'x2':2, 'y2':11.22, 'x3':1}, 
                      | {'x1':1, 'y1':11.22, 'x2':2, 'y2':1.2, 'x3':1},
                      | {'x1':1, 'y1':11.22, 'x2':2, 'y2':11.22, 'x3':1},
                      | {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2}, 
                      | {'x1':2, 'y1':1.2, 'x2':1, 'y2':11.22, 'x3':2}, 
                      | {'x1':2, 'y1':11.22, 'x2':1, 'y2':1.2, 'x3':2},
                      | {'x1':2, 'y1':11.22, 'x2':1, 'y2':11.22, 'x3':2} >>""".trimMargin(),

            "(N2D2c MATCH (x))" to "<< {'x':1}, {'x':2} >>",
            "(N2D2c MATCH -[y]-> )" to "<< {'y':1.2}, {'y':2.1} >>",
            "(N2D2c MATCH (x)-[y]->(z) )" to "<< {'x':1, 'y':1.2, 'z':2}, {'x':2, 'y':2.1, 'z':1} >>",
            "(N2D2c MATCH (x)-[y]->(x) )" to "<< >>",
            "(N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x3) )" to
                """<<     {'x1':1, 'y1':1.2, 'x2':2, 'y2':2.1, 'x3':1},
                        | {'x1':2, 'y1':2.1, 'x2':1, 'y2':1.2, 'x3':2} >>""".trimMargin(),
            "(N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x1) )" to
                """<<     {'x1':1, 'y1':1.2, 'x2':2, 'y2':2.1 },
                        | {'x1':2, 'y1':2.1, 'x2':1, 'y2':1.2 } >>""".trimMargin(),
            "(N2D2c MATCH (x1)-[y1]->(x2)-[y2]-(x3) )" to
                """<<     {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1},
                        | {'x1':1, 'y1':1.2, 'x2':2, 'y2':2.1, 'x3':1},
                        | {'x1':2, 'y1':2.1, 'x2':1, 'y2':1.2, 'x3':2},
                        | {'x1':2, 'y1':2.1, 'x2':1, 'y2':2.1, 'x3':2} >>""".trimMargin(),
            "(N2D2c MATCH (x1)-[y1]-(x2)-[y2]->(x3) )" to
                """<<     {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2},
                        | {'x1':2, 'y1':2.1, 'x2':1, 'y2':1.2, 'x3':2},
                        | {'x1':1, 'y1':1.2, 'x2':2, 'y2':2.1, 'x3':1},
                        | {'x1':1, 'y1':2.1, 'x2':2, 'y2':2.1, 'x3':1} >>""".trimMargin(),
            "(N2D2c MATCH (x1)-[y1]-(x2)-[y2]-(x3) )" to
                """<<     {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1},
                        | {'x1':1, 'y1':1.2, 'x2':2, 'y2':2.1, 'x3':1},
                        | {'x1':1, 'y1':2.1, 'x2':2, 'y2':1.2, 'x3':1},
                        | {'x1':1, 'y1':2.1, 'x2':2, 'y2':2.1, 'x3':1},
                        | {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2},
                        | {'x1':2, 'y1':1.2, 'x2':1, 'y2':2.1, 'x3':2},
                        | {'x1':2, 'y1':2.1, 'x2':1, 'y2':1.2, 'x3':2},
                        | {'x1':2, 'y1':2.1, 'x2':1, 'y2':2.1, 'x3':2} >>""".trimMargin(),

            "(N2U2 MATCH (x))" to "<< {'x':1}, {'x':2} >>",
            "(N2U2 MATCH ~[y]~ )" to "<< {'y':1.2}, {'y':2.1}, {'y':1.2}, {'y':2.1} >>", // duplicated, being an erasure of the next test
            "(N2U2 MATCH (x)~[y]~(z) )" to
                """<< {'x':1, 'y':1.2, 'z':2}, {'x':2, 'y':2.1, 'z':1}, 
                    | {'x':2, 'y':1.2, 'z':1}, {'x':1, 'y':2.1, 'z':2} >>""".trimMargin(),
            "(N2U2 MATCH (x)~[y]~(x) )" to "<< >>",
            "(N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x3) )" to
                """<< {'x1':1, 'y1':1.2, 'x2':2, 'y2':2.1, 'x3':1},
                    | {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2, 'x3':1},
                    | {'x1':1, 'y1':2.1, 'x2':2, 'y2':1.2, 'x3':1},
                    | {'x1':1, 'y1':2.1, 'x2':2, 'y2':2.1, 'x3':1},
                    | {'x1':2, 'y1':1.2, 'x2':1, 'y2':2.1, 'x3':2},
                    | {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2, 'x3':2},
                    | {'x1':2, 'y1':2.1, 'x2':1, 'y2':1.2, 'x3':2},
                    | {'x1':2, 'y1':2.1, 'x2':1, 'y2':2.1, 'x3':2} >>""".trimMargin(),
            "(N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x1) )" to
                """<< {'x1':1, 'y1':1.2, 'x2':2, 'y2':2.1},
                    | {'x1':1, 'y1':1.2, 'x2':2, 'y2':1.2},
                    | {'x1':1, 'y1':2.1, 'x2':2, 'y2':1.2},
                    | {'x1':1, 'y1':2.1, 'x2':2, 'y2':2.1},
                    | {'x1':2, 'y1':1.2, 'x2':1, 'y2':2.1},
                    | {'x1':2, 'y1':1.2, 'x2':1, 'y2':1.2},
                    | {'x1':2, 'y1':2.1, 'x2':1, 'y2':1.2},
                    | {'x1':2, 'y1':2.1, 'x2':1, 'y2':2.1} >>""".trimMargin(),
        )
    }

    /******** GraphN3D2 ********/

    val sessionGraphN3D2 = sessionOf(
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
    @ArgumentsSource(GraphN3D2::class)
    fun testGraphN3D2(qr: Pair<String, String>) {
        testGraphQueries(sessionGraphN3D2, qr)
    }

    class GraphN3D2 : ArgumentsProviderBase() {
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
            "(g3aba MATCH (x1) -> (x2) -> (x2))" to
                "<< >>",
            "(g3aba MATCH (x1) -> (x2) -> (x1))" to
                "<< >>",
            "(g3aba MATCH (x1) - (x2) - (x1))" to
                "<< {'x1': 1, 'x2': 2}, {'x1': 2, 'x2': 1}, {'x1': 2, 'x2': 3}, {'x1': 3, 'x2': 2} >>"
        )
    }

    @Test
    fun testMatchNonGraph() {
        runEvaluatorErrorTestCase(
            EvaluatorErrorTestCase(
                query = "(42 MATCH (x) -> (y))",
                expectedErrorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE,
                expectedPermissiveModeResult = "MISSING",
                targetPipeline = currentPipeline
            ),
            sessionGraphN3D2
        )
    }
}
