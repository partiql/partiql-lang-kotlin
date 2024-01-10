/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.spi.internal.index

import org.partiql.types.function.FunctionSignature

/**
 * We use a tree to represent function definitions within the catalog for path searching. This is a simple index for
 * an [FnRegistry], and perhaps a string-value map with normalized names may perform better.
 */
internal class FnIndexTree<T : FunctionSignature> : FnIndex<T> {

    private val root = Node.Schema<T>(
        symbol = "ROUTINES",
        children = mutableMapOf(),
    )

    /**
     * Search the tree for all function variants at the given path.
     *
     * @param path
     * @return
     */
    override fun search(path: List<String>): List<T> {
        var i = 0
        var curr: Node.Schema<T> = root
        for (step in path) {
            i += 1
            val next = curr.get(step) ?: break
            when (next) {
                is Node.Schema -> curr = next
                is Node.Def -> when (i) {
                    path.size -> return next.variants
                    else -> break
                }
            }
        }
        // not found
        return emptyList()
    }

    /**
     * Insert function variant definitions at the given path.
     *
     * @param path
     * @param variants
     */
    override fun insert(path: List<String>, variants: List<T>) {
        var curr: Node.Schema<T> = root
        for (i in path.indices) {
            val step = path[i]
            if (i == path.size - 1) {
                curr.insert(
                    Node.Def(
                        symbol = step,
                        variants = variants.toMutableList(),
                    )
                )
                return
            }
            val next = curr.get(step)
            curr = when (next) {
                null -> {
                    val child =
                        Node.Schema<T>(step, mutableMapOf())
                    curr.insert(child)
                    child
                }
                is Node.Def -> {
                    // ERROR!
                    error("Routine definition found, expected a schema.")
                }
                is Node.Schema -> next
            }
        }
    }

    sealed interface Node<T : FunctionSignature> {

        val symbol: String

        /**
         * A schema within the catalog.
         *
         * @property symbol
         * @property children
         */
        class Schema<T : FunctionSignature>(
            override val symbol: String,
            private val children: MutableMap<String, Node<T>>,
        ) : Node<T> {

            fun get(symbol: String): Node<T>? =
                children[symbol]

            fun insert(child: Node<T>) {
                children[child.symbol] = child
            }
        }

        /**
         * An entry of DEFINITION_SCHEMA.ROUTINES with all function variants.
         *
         * @property symbol
         * @property variants
         */
        class Def<T : FunctionSignature>(
            override val symbol: String,
            internal val variants: MutableList<T>,
        ) : Node<T>
    }
}
