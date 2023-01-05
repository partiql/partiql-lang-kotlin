/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.cli.format

import org.partiql.cli.format.dot.DotGraph
import org.partiql.cli.format.dot.DotNodeId
import org.partiql.cli.format.dot.DotNodeShape
import org.partiql.cli.format.dot.DotNodeStmt
import org.partiql.cli.format.dot.digraph
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.pig.runtime.DomainNode
import kotlin.reflect.KClass
import kotlin.reflect.jvm.isAccessible

internal object DotFormatter : NodeFormatter {

    private val NODE_MAP = mutableMapOf<KClass<*>, Int>()
    private var CLUSTER_COUNT = 0
    private const val START_NODE_ID = "START"
    private val START_NODE: DotNodeStmt = DotNodeStmt(DotNodeId(START_NODE_ID)).also { node ->
        node + {
            shape = DotNodeShape.DIAMOND
            style = "bold,filled"
            fillcolor = "yellow"
            fontcolor = "black"
        }
    }

    override fun format(input: DomainNode): String {
        NODE_MAP.clear()
        CLUSTER_COUNT = 0
        val graph = digraph("DotGraph") {
            node {
                shape = DotNodeShape.RECTANGLE
            }
            +START_NODE
            val uniqueName = addToGraph(this, input)
            START_NODE_ID - uniqueName
        }
        return buildString {
            appendLine(graph.dot())
            appendLine("/**")
            appendLine(" * To receive a URL to view the graph online, please specify DOT_URL as the target FORMAT.")
            appendLine(" */")
        }
    }

    /**
     * The top-most recursive function on an input of ANY type. Decides which recursive function to call.
     */
    private fun addToGraph(root: DotGraph, input: Any, inputName: String? = null): String {
        return when (input) {
            is List<*> -> addList(root, input, inputName)
            is PartiqlLogical.Bexpr,
            is PartiqlLogicalResolved.Bexpr,
            is PartiqlPhysical.Bexpr -> addLogical(root, input as DomainNode, inputName)
            is DomainNode -> addDomainNode(root, input, inputName)
            else -> addBaseNode(root, input, inputName)
        }
    }

    /**
     * Used for nodes that aren't of type DomainNode (AKA don't recurse)
     */
    private fun addBaseNode(root: DotGraph, input: Any, inputName: String? = null): String {
        val uniqueName = inputName ?: getUniqueName(input::class)
        val actualName = "\"$uniqueName\nValue: $input\""
        root.apply { +actualName }
        return actualName
    }

    /**
     * Used for Domain Nodes
     */
    private fun addDomainNode(root: DotGraph, input: DomainNode, inputName: String? = null): String {
        val uniqueName = inputName ?: getUniqueName(input::class)

        // Print non-relational algebra operators and exit
        if (input !is PartiqlAst.PartiqlAstNode && containsAlgebraOperators(input).not()) {
            val tree = TreeFormatter.formatSupport(input, lineSeparator = "\\l").replace("\"", "\\\"")
            return "\"$uniqueName\n$tree\""
        }

        // Add children (recursively), add self, and add edges between self and children
        root.apply {
            input.properties().forEach {
                it.isAccessible = true
                val child = it.get(input)
                val childName = addToGraph(root, child ?: "null")
                uniqueName - childName + {
                    if (isRelationalOperator(child)) { color = "blue" }
                    label = it.name
                }
            }
            if (isBindingsToValues(input)) {
                +uniqueName + {
                    shape = DotNodeShape.CIRCLE
                    style = "filled,bold"
                    color = "red"
                    fontcolor = "white"
                    label = uniqueName.toUpperCase()
                }
            }
        }
        return uniqueName
    }

    /**
     * Create a subgraph (box), add children, add self, and add edges between children and self
     */
    private fun addLogical(root: DotGraph, input: DomainNode, inputName: String? = null): String {
        val uniqueName = inputName ?: getUniqueName(input::class)
        val clusterName = "cluster_${CLUSTER_COUNT++}"
        root.apply {
            // Create Box for Non-Algebra Operators
            +subgraph(clusterName) {
                node {
                    style = "filled"
                    color = "white"
                    input.properties().forEach { property ->
                        property.isAccessible = true
                        val child = property.get(input) ?: "null"
                        val scope = when (isRelationalOperator(child)) {
                            true -> root
                            else -> this@subgraph
                        }
                        val childName = addToGraph(scope, child)
                        uniqueName - childName + {
                            if (isRelationalOperator(child)) { color = "blue" }
                            label = property.name
                        }
                    }
                    +uniqueName + {
                        shape = DotNodeShape.CIRCLE
                        fillcolor = "blue"
                        color = "blue"
                        fontcolor = "white"
                        style = "bold,filled"
                        label = uniqueName.toUpperCase()
                    }
                }
                color = "blue"
            }
            return uniqueName
        }
    }

    /**
     * Adds a box to the graph containing a list of nodes
     */
    private fun addList(root: DotGraph, input: List<*>, inputName: String? = null): String {
        val uniqueName = inputName ?: getUniqueName(input::class)

        // Print non-relational algebra operators and exit
        if (containsAlgebraOperators(input).not()) {
            val tree = TreeFormatter.formatSupport(input, lineSeparator = "\\l").replace("\"", "\\\"")
            return "\"$uniqueName\n$tree\""
        }

        // Add box, add children, add self, and add edges
        val clusterName = "cluster_${CLUSTER_COUNT++}"
        root.apply {
            +subgraph(clusterName) {
                node {
                    style = "filled"
                    color = "white"
                    input.forEachIndexed { index, child ->
                        val childName = addToGraph(this@subgraph, child!!)
                        uniqueName - childName + {
                            label = "Index $index"
                        }
                    }
                }
                color = "green"
            }
            return uniqueName
        }
    }

    private fun getUniqueName(clazz: KClass<*>): String {
        val index = NODE_MAP.getOrPut(clazz) { 0 }
        NODE_MAP[clazz] = index + 1
        return "${clazz.simpleName}_$index"
    }

    private fun isRelationalOperator(input: Any?) = when (input) {
        null -> false
        is PartiqlLogical.Bexpr, is PartiqlLogicalResolved.Bexpr, is PartiqlPhysical.Bexpr -> true
        else -> false
    }

    private fun isBindingsToValues(input: Any?) = when (input) {
        null -> false
        is PartiqlLogical.Expr.BindingsToValues,
        is PartiqlLogicalResolved.Expr.BindingsToValues,
        is PartiqlPhysical.Expr.BindingsToValues -> true
        else -> false
    }

    private fun containsAlgebraOperators(input: Any?): Boolean = when (input) {
        null -> false
        is PartiqlLogical.Bexpr, is PartiqlLogicalResolved.Bexpr, is PartiqlPhysical.Bexpr -> true
        is List<*> -> input.any { containsAlgebraOperators(it) }
        is DomainNode -> {
            input.properties().any { property ->
                property.isAccessible = true
                val child = property.get(input)
                containsAlgebraOperators(child)
            }
        }
        else -> false
    }
}
