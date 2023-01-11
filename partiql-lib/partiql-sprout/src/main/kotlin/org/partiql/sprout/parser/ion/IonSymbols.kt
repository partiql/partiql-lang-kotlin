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

import com.amazon.ion.IonList
import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import io.github.rchowell.dotlin.graph

/**
 * Produce a graph of type identifiers from a list of type definitions.
 */
internal class IonSymbols private constructor(val root: Node) {

    companion object {

        fun build(definitions: List<IonValue>) = IonSymbols(
            root = Node(id = "_root").apply {
                definitions.forEach { type ->
                    val child = Visitor().visit(type, this)!!
                    children.add(child)
                }
            }
        )
    }

    private class Visitor : IonVisitor<Node, Node?> {

        override fun visit(v: IonList, ctx: Node?): Node {
            val node = Node(
                id = v.id(),
                parent = ctx
            )
            if (!v.isEnum()) {
                v.forEach { node.children.add(visit(it, node)!!) }
            }
            return node
        }

        override fun visit(v: IonStruct, ctx: Node?): Node {
            val node = Node(
                id = v.id(),
                parent = ctx
            )
            v.filter { it.isInlineEnum() }.forEach {
                node.children.add(
                    Node(
                        id = it.fieldName,
                        parent = node,
                    )
                )
            }
            return node
        }

        override fun defaultVisit(v: IonValue, ctx: Node?) = error("cannot parse value $v")
    }

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
