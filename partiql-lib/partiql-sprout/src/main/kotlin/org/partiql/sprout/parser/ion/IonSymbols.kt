/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.sprout.parser.ion

import com.amazon.ion.IonContainer
import com.amazon.ion.IonList
import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import io.github.rchowell.dotlin.graph

/**
 * Produce a graph of type identifiers from a list of type definitions.
 */
internal class IonSymbols private constructor(val root: Node) {

    companion object {

        val COLLECTIONS = setOf("map", "list", "set")

        fun build(definitions: List<IonValue>) = IonSymbols(
            root = Node(id = "_root").apply {
                definitions.forEach { type ->
                    val child = Visitor().visit(type, this)
                    children.add(child)
                }
            }
        )

        /**
         * Consider asserting more thorough type definition naming rules ie enforce lower snake case
         */
        fun assertNonReserved(id: String, context: String) = assert(!COLLECTIONS.contains(id)) {
            "Cannot used reserved name `$id` for a type definition, $context."
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private class Visitor : IonVisitor<Node, Node?> {

        override fun visit(v: IonList, parent: Node?): Node {
            val id = v.id()
            assertNonReserved(id, if (parent != null) "child of $parent" else "top-level type")
            var node = Node(
                id = id,
                parent = parent
            )
            // Skip this node by linking parent; add all symbols in special container syntax _::[ ... ]
            if (id == "_") {
                node = parent ?: error("unexpected container _ at top-level")
            }
            if (!v.isEnum()) {
                v.forEach { node.children.add(visit(it, node)) }
            }
            return node
        }

        override fun visit(v: IonStruct, parent: Node?): Node {
            val id = v.id()
            assertNonReserved(id, if (parent != null) "child of $parent" else "top-level type")
            val node = Node(
                id = id,
                parent = parent,
            )
            v.forEach { field ->
                when {
                    field.isContainer() -> {
                        val children = (field as IonContainer).map { visit(it, node) }
                        node.children.addAll(children)
                    }
                    field.isInline() -> {
                        val (symbol, nullable) = field.ref()
                        // DANGER! Mutate annotations to set the definition id as if it weren't an inline
                        field.setTypeAnnotations(symbol)
                        // Parse as any other definition
                        val child = visit(field, node)
                        // DANGER! Add back the dropped "optional" annotation
                        if (nullable) field.setTypeAnnotations("optional", symbol)
                        // Include the inline definition as a child of this node
                        node.children.add(child)
                    }
                }
            }
            return node
        }

        override fun defaultVisit(v: IonValue, parent: Node?) = error("cannot parse value $v")
    }

    /**
     * For debugging, consider prefixing node names since names must be globally unique in DOT
     */
    override fun toString() = graph {
        root.children.forEach {
            +subgraph(it.id) {
                label = it.id
                it.walk { parent, child ->
                    // connection
                    parent.id - child.id
                }
            }
        }
    }.dot()

    internal class Node(
        val id: String,
        val parent: Node? = null,
        val children: MutableList<Node> = mutableListOf()
    ) {

        val path: List<String>
            get() {
                val path = mutableListOf<String>()
                var node: Node? = this
                while (node != null) {
                    path.add(node.id)
                    node = node.parent
                }
                // Use [1, path.size) so that `_root` is excluded in the path
                return path.reversed().subList(1, path.size)
            }

        override fun toString() = path.joinToString(".")

        override fun hashCode() = path.hashCode()
        override fun equals(other: Any?) = when (other) {
            is Node -> this.path == other.path
            else -> false
        }
    }
}
