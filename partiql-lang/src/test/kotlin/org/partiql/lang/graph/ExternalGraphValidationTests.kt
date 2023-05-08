package org.partiql.lang.graph

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.util.ArgumentsProviderBase

class ExternalGraphValidationTests {

    private val classloader = ExternalGraphValidationTests::class.java.classLoader
    private fun readResource(resourcePath: String): String {
        val url = classloader.getResource(resourcePath)
            ?: error("Resource path not found: $resourcePath")
        return url.readText()
    }

    @Test
    fun testRfc0025Graph() {
        assertDoesNotThrow {
            val graphStr = readResource("graphs/rfc0025-example.ion")
            ExternalGraphReader.validate(graphStr)
        }
    }

    @Test
    fun testGpmlPaperGraph() {
        assertDoesNotThrow {
            val graphStr = readResource("graphs/gpml-paper-example.ion")
            ExternalGraphReader.validate(graphStr)
        }
    }

    abstract class GraphValidationTestCase() {
        abstract val descr: String
        abstract val graphStr: String
        private val len: Int = 30
        fun longDescr(): String {
            val d = descr.take(len).padEnd(len, ' ')
            val g = graphStr.replace('\n', ' ').take(len)
            return "[$d]: $g"
        }
    }
    class Valid(override val descr: String, override val graphStr: String) : GraphValidationTestCase() {
        override fun toString(): String = "Valid${longDescr()}"
    }
    class Invalid(override val descr: String, override val graphStr: String) : GraphValidationTestCase() {
        override fun toString(): String = "Invalid${longDescr()}"
    }

    @ParameterizedTest
    @ArgumentsSource(GraphTestCases::class)
    fun testValidation(tc: GraphValidationTestCase) {
        when (tc) {
            is Valid -> assertDoesNotThrow { ExternalGraphReader.validate(tc.graphStr) }
            is Invalid -> assertThrows<GraphValidationException> { ExternalGraphReader.validate(tc.graphStr) }
        }
    }

    class GraphTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<GraphValidationTestCase> = listOf(
            Valid("Empty graph", "graph::{ nodes: [], edges: [] }"),
            Valid("Graph without annotation", "{ nodes: [], edges: [] }"),
            Invalid("Empty graph with forgotten edges", "graph::{ nodes: [] }"),

            Valid("Node with id, label, payload", """{ nodes: [ { id: n1, labels: ["a"], payload: 33 } ], edges: [] }"""),
            Valid("Node with multiple labels", """{ nodes: [ { id: n1, labels: ["a", "b", "c"] } ], edges: [] }"""),
            Valid("Node with no labels", """{ nodes: [ { id: n1, labels: [] } ], edges: [] }"""),
            Valid("Node with properties", """{ nodes: [ { id: n1, payload: {height: 12, weight: 152} } ], edges: [] }"""),
            Invalid("Node without id", """{ nodes: [ { labels: ["a"], payload: "must have the id!" } ], edges: [] }"""),

            Valid(
                "Edge with a label",
                """{ nodes: [{id: n1}, {id: n2}], 
                    |edges: [ {id: e1, labels: ["go"], ends: (n1 -> n2)} ] }""".trimMargin()
            ),
            Valid(
                "Edge with multiple labels",
                """{ nodes: [{id: n1}, {id: n2}], 
                    |edges: [ {id: e1, labels: ["go", "went", "gone"], ends: (n1 -> n2)} ] }""".trimMargin()
            ),
            Valid(
                "Undirected edge with no labels",
                """{ nodes: [{id: n1}, {id: n2}], 
                    |edges: [ {id: e1, labels: [], ends: (n1 -- n2)} ] }""".trimMargin()
            ),
            Valid(
                "Undirected edge with properties",
                """{ nodes: [{id: n1}, {id: n2}], 
                    |edges: [ {id: e1, ends: (n1 -- n2),
                    |          payload: {length: 23, thickness: 3} }] }""".trimMargin()
            ),
            Valid(
                "Edges from a node to itself",
                """{ nodes: [{id: n1}, {id: n2}], 
                    |edges: [ {id: e1, ends: (n1 -- n1)},
                    |         {id: e2, ends: (n2 <- n2)}, ] }""".trimMargin()
            ),
            Invalid(
                "Edge without id",
                """{ nodes: [{id: n1}, {id: n2}], 
                    |edges: [ {labels: ["a"], ends: (n2 <- n1)} ] }""".trimMargin()
            ),
            Invalid(
                "Edge without ends",
                """{ nodes: [{id: n1}, {id: n2}], 
                    |edges: [ { id: e1, labels: ["a"] } ] }""".trimMargin()
            ),

            Valid(
                "2-node 3-edge graph",
                """{ 
                |nodes: [ {id: n1}, {id: n2} ], 
                |edges: [ {id: e1, ends: (n1 -> n2)}, 
                |         {id: e2, ends: (n1 <- n2)}, 
                |         {id: e3, ends: (n1 -- n2)} ] }""".trimMargin()
            ),
            Valid(
                "With some annotations",
                """graph::{ 
                |nodes: [ {id: node::n1}, {id: n2} ], 
                |edges: [ {id: e1, ends: (n1 -> node::n2)}, 
                |         {id: edge::e2, ends: (node::n2 -> node::n1)}, 
                |         {id: e3, ends: (n1 -- n2)} ] }""".trimMargin()
            ),
            Invalid(
                "With wrong annotations",
                """GRAPH::{ 
                |nodes: [ {id: n1}, {id: edge::n2} ], 
                |edges: [ {id: e1, ends: (n1 -> NODE::n2)}, 
                |         {id: e2, ends: (n2 -> n1)}, 
                |         {id: node::e3, ends: (n1 -- n2)} ] }""".trimMargin()
            ),

            Valid(
                "Repeated identifiers across nodes and edges",
                """{ 
                |nodes: [ {id: x}, {id: y} ], edges: [ {id:x, ends: (x -> y)} ] }""".trimMargin()
            ),

            // The following examples are valid per ISL, but should be rejected by a processor.
            Valid(
                "Edge between non-existent nodes",
                """{ 
                |nodes: [ {id: n1}, {id: n2} ], edges: [ {id:e1, ends: (z1 -> z2)} ] }""".trimMargin()
            ),
            Valid(
                "Repeated identifiers within nodes or edges",
                """{ 
                |nodes: [ {id: n1}, {id: n1} ], 
                |edges: [ {id:e1, ends: (n1 -> n1)},
                |         {id:e1, ends: (n1 -- n1)} ] }""".trimMargin()
            ),

        )
    }
}
