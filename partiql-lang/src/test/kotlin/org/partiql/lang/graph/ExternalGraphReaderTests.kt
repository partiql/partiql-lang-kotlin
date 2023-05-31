package org.partiql.lang.graph

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.longValue

class ExternalGraphReaderTests {

    private val classloader = ExternalGraphReaderTests::class.java.classLoader
    private fun readResource(resourcePath: String): String {
        val url = classloader.getResource(resourcePath)
            ?: error("Resource path not found: $resourcePath")
        return url.readText()
    }

    @Test
    fun readGraphSchemaFail() {
        val src = """ { nodes: 45, frills: 33 } """
        assertThrows<GraphValidationException> {
            ExternalGraphReader.read(src)
        }
    }
    @Test
    fun readEmptyGraph() {
        val src = """ graph::{ nodes: [], edges: [] } """
        assertDoesNotThrow {
            val g = ExternalGraphReader.read(src)
            assertEquals(0, g.nodes.size)
            assertEquals(0, g.directed.size)
            assertEquals(0, g.undir.size)
        }
    }

    @Test
    fun readThreeNodeGraph() {
        val src = """{ nodes: [ {id: n1}, {id: n2}, {id: n3} ], edges: [] }"""
        val g = ExternalGraphReader.read(src)
        assertEquals(3, g.nodes.size)
        assertTrue(g.nodes.all { it.labels.isEmpty() })
        assertTrue(g.nodes.all { it.payload.type == ExprValueType.NULL })
    }

    @Test
    fun readPopulatedNodes() {
        val src = """{ nodes: [ 
            |{id: n1, labels: [] }, 
            |{id: n2, labels: ["a"] }, 
            |{id: n3, labels: ["a", "b", "c"] }, 
            |{id: n4, labels: ["b"], payload: 42 }, 
            |{id: n5, labels: ["c"], payload: { meaningOf: "Life", is: 42 }}, 
            |], edges: [] }""".trimMargin()
        val g = ExternalGraphReader.read(src)
        assertEquals(5, g.nodes.size)
        val allLabels = g.nodes.flatMap { it.labels }
        assertEquals(6, allLabels.size)
        assertEquals(setOf("a", "b", "c"), allLabels.toSet())

        val nB = g.nodes.find { it.labels == setOf("b") }
        if (nB == null) assertTrue(false, "Did not find a node labeled with b alone.")
        else assertEquals(42, nB.payload.longValue())

        val nC = g.nodes.find { it.labels == setOf("c") }
        if (nC == null) assertTrue(false, "Did not find a node labeled with c alone.")
        else assertTrue(nC.payload.type == ExprValueType.STRUCT)
    }

    @Test
    fun dontUndefinedNodes() {
        val src = """{ nodes: [ {id: n1}, {id: n2} ], edges: [ {id:e1, ends: (z1 -> z2)} ] }"""
        assertThrows<GraphReadException> { ExternalGraphReader.read(src) }
    }

    @Test
    fun dontRepeatedNodeId() {
        val src = """{ nodes: [ {id: n1}, {id: n1} ], edges: [ ] }"""
        assertThrows<GraphReadException> { ExternalGraphReader.read(src) }
    }

    @Test
    fun readTwoAndTwo() {
        val src = """{ nodes: [ {id: n1}, {id: n2}], 
                    |  edges: [ {id: e1, ends: (n1 -- n2)},
                    |           {id: e2, ends: (n1 -> n2)}, ] }""".trimMargin()
        val g = ExternalGraphReader.read(src)
        assertEquals(2, g.nodes.size)
        assertEquals(1, g.directed.size)
        assertEquals(1, g.undir.size)
        val allNodes = g.nodes.toSet()
        val e1Nodes = g.undir.flatMap { setOf(it.first, it.third) }.toSet()
        val e2Nodes = g.directed.flatMap { setOf(it.first, it.third) }.toSet()
        assertEquals(allNodes, e1Nodes)
        assertEquals(allNodes, e2Nodes)
    }

    @Test
    fun readFourAndTwoPartitioned() {
        val src = """
            { nodes: [ {id: n1, labels: ["a"]}, {id: n2, labels: ["b"]},
                       {id: m1, labels: ["a"]}, {id: m2, labels: ["b"]}, ],
              edges: [ {id: e1, labels: ["a"], ends: (n1 -> m1) },  
                       {id: e2, labels: ["b"], ends: (n2 -- m2) }, ] }
        """.trimIndent()
        val g = ExternalGraphReader.read(src)
        assertEquals(4, g.nodes.size)
        assertEquals(1, g.directed.size)
        assertEquals(1, g.undir.size)
        val aNodes = g.nodes.filter { it.labels.contains("a") }.toSet()
        val bNodes = g.nodes.filter { it.labels.contains("b") }.toSet()
        val dirNodes = g.directed.flatMap { setOf(it.first, it.third) }.toSet()
        val undirNodes = g.undir.flatMap { setOf(it.first, it.third) }.toSet()
        assertTrue((dirNodes intersect undirNodes).isEmpty())
        assertEquals(aNodes, dirNodes)
        assertEquals(bNodes, undirNodes)
    }

    fun readRfc0025Example() {
        val graphStr = readResource("graphs/rfc0025-example.ion")
        val g = ExternalGraphReader.read(graphStr)
        assertEquals(3, g.nodes.size)
        assertEquals(0, g.undir.size)
        assertEquals(3, g.directed.size)
    }
    @Test
    fun readGpmlPaperExample() {
        val graphStr = readResource("graphs/gpml-paper-example.ion")
        val g = ExternalGraphReader.read(graphStr)
        assertEquals(14, g.nodes.size)
        assertEquals(8, g.undir.size)
        assertEquals(13, g.directed.size)
    }
}
