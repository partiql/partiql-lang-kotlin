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
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ion.UnknownSymbolException
import java.util.LinkedList
import java.util.Stack

/**
 * Called in the context of parsing a type definition.
 */
internal fun IonValue.id(): String {
    assert(typeAnnotations.size == 1) { "A type definition requires a single identifier annotation, $this" }
    return typeAnnotations[0]
}

/**
 * True iff every value is a Symbol matching [A-Z][A-Z0-9_]*
 */
internal fun IonValue.isEnum(): Boolean {
    if (this !is IonList) return false
    val pattern = Regex("[A-Z][A-Z0-9_]*")
    this.forEach {
        if (it !is IonSymbol || !pattern.matches(it.stringValue())) {
            return false
        }
    }
    return true
}

/**
 * These are all the inline forms; where the inline type identifiers are not "list", "set", or "map".
 *
 * foo::{
 *    a: [...],               // inline sum foo.a
 *    b: v::[...],            // inline sum foo.v
 *    c: optional::[...],     // inline sum foo.c, optional field of foo
 *    d: optional::x::[...],  // inline sum foo.v, optional field of foo
 *    e: {...},               // inline product foo.e
 *    f: y::{...},            // inline product foo.y
 *    g: optional::{...},     // inline product foo.g, optional field of foo
 *    h: optional::z::{...},  // inline product foo.z, optional field of foo
 * }
 */
internal fun IonValue.isInline(): Boolean {
    if (fieldName == null) {
        return false
    }
    if (this is IonList || this is IonStruct) {
        return when (typeAnnotations.size) {
            0 -> true
            1 -> typeAnnotations[0] !in IonSymbols.COLLECTIONS
            2 -> typeAnnotations[1] !in IonSymbols.COLLECTIONS
            else -> false
        }
    }
    return false
}

/**
 * Returns true if this value is of the form
 *   - _: [ ... ]
 *   - _::[ ... ]
 *
 *  Which represent nested definitions in product and sum types respectively.
 */
internal fun IonValue.isContainer(): Boolean {
    if (type != IonType.LIST) {
        return false
    }
    var symbol = try {
        // _: [ ]
        fieldName
    } catch (_: UnknownSymbolException) {
        null
    }
    if (symbol == null && typeAnnotations.isNotEmpty()) {
        // _::[]
        symbol = typeAnnotations[0]
    }
    return symbol == "_"
}

/**
 * Called in the context of parsing a type reference; these are all the type reference forms.
 *
 * foo::{
 *    a: x,                     // ref to type x
 *    b: optional::x,           // ref to type x, optional field of foo
 *    c: [...],                 // ref to inline sum foo.c
 *    d: t::[...],              // ref to inline sum foo.t or collection (depends on value of t)
 *    e: optional::t::[...],    // ref to inline sum foo.t or collection (depends on value of t), optional field of foo
 *    f: {...},                 // ref to inline product foo.c
 *    g: s::{...},              // ref to inline product foo.s
 *    h: optional::s::{...},    // ref to inline product foo.s, optional field of foo
 * }
 *
 * This method intentionally does not make a distinction between refs to inline sum defs and collection type refs.
 */
internal fun IonValue.ref(): Pair<String, Boolean> = when (this) {
    is IonSymbol -> {
        val prefix = typeAnnotations.joinToString("::")
        val suffix = stringValue()
        when (typeAnnotations.size) {
            0 -> Pair(suffix, false)
            1 -> {
                if (typeAnnotations[0] != "optional") {
                    err("optional::$suffix", "$prefix::$suffix")
                }
                Pair(suffix, true)
            }
            else -> err(
                expected = "optional::$suffix or $suffix",
                found = "$prefix::$suffix",
            )
        }
    }
    is IonList, is IonStruct -> {
        val prefix = typeAnnotations.joinToString("::")
        val suffix = if (this is IonList) "[...]" else "{...}"
        when (typeAnnotations.size) {
            0 -> Pair(fieldName, false)
            1 -> when (val n = typeAnnotations[0]) {
                "optional" -> Pair(fieldName, true)
                else -> Pair(n, false)
            }
            2 -> {
                val o = typeAnnotations[0]
                val n = typeAnnotations[1]
                if (o != "optional") err("optional::$n::$suffix", "$prefix::$suffix")
                Pair(n, true)
            }
            else -> err(
                expected = "optional::$suffix or $suffix",
                found = "$prefix::$suffix",
            )
        }
    }
    else -> err("symbol, list, or struct", "$type")
}

private fun err(expected: String, found: String): Nothing {
    error("invalid type reference, must be of the form $expected, but found `$found`")
}

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
