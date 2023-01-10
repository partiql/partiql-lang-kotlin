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
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonValue
import java.util.LinkedList
import java.util.Stack

internal fun IonValue.id(): String {
    if (typeAnnotations.size != 1) {
        error("Ion value requires a single identifier annotation")
    }
    return typeAnnotations[0]
}

/**
 * True iff every value is a Symbol matching [A-Z][A-Z_]*
 */
private val enumValueRegex = Regex("[A-Z][A-Z_]*")

internal fun IonValue.isEnum(): Boolean = when {
    (this is IonList) && this.isNotEmpty() -> {
        this.forEach {
            if (it !is IonSymbol || !enumValueRegex.matches(it.stringValue())) {
                return false
            }
        }
        true
    }
    else -> false
}

internal fun IonValue.isInlineEnum(): Boolean = isEnum() && typeAnnotations.isEmpty()

/**
 * Depth-first tree walk
 */
internal inline fun IonSymbols.Node.walk(action: (parent: IonSymbols.Node, child: IonSymbols.Node) -> Unit) {
    val seen = mutableSetOf<IonSymbols.Node>()
    val stack = Stack<IonSymbols.Node>()
    stack.push(this)
    while (stack.isNotEmpty()) {
        val parent = stack.pop()
        if (seen.contains(parent)) {
            continue
        }
        seen.add(parent)
        parent.children.forEach { child ->
            action.invoke(parent, child)
            stack.push(child)
        }
    }
}

/**
 * Breadth-first search
 */
internal fun IonSymbols.Node.search(id: String): IonSymbols.Node? {
    val seen = mutableSetOf<IonSymbols.Node>()
    val queue = LinkedList<IonSymbols.Node>()
    queue.add(this)
    while (queue.isNotEmpty()) {
        val node = queue.pop()
        if (node.id == id) {
            return node
        }
        if (seen.contains(node)) {
            continue
        }
        seen.add(node)
        if (node.parent != null) {
            queue.add(node.parent)
        }
        queue.addAll(node.children)
    }
    return null
}

/**
 * Search path starting from this
 */
internal fun IonSymbols.Node.search(path: List<String>): IonSymbols.Node? {
    var i = 0
    var node: IonSymbols.Node? = this
    while (node != null && i < path.size) {
        node = node.children.find { it.id == path[i] }
        i += 1
    }
    return node
}
