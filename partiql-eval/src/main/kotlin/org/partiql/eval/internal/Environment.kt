package org.partiql.eval.internal

import java.util.Stack

internal class Environment {

    private val scopes: Stack<Record> = Stack<Record>()

    internal inline fun scope(record: Record, block: () -> Unit) {
        scopes.push(record)
        block.invoke()
        scopes.pop()
    }
}
