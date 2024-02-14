package org.partiql.eval.internal

import java.util.Stack

internal class Environment {

    private val scopes: Stack<Record> = Stack<Record>()

    internal inline fun <T> scope(record: Record, block: () -> T): T {
        scopes.push(record)
        val result = try {
            block.invoke()
        } catch (t: Throwable) {
            scopes.pop()
            throw t
        }
        scopes.pop()
        return result
    }

    operator fun get(index: Int): Record {
        return scopes[index]
    }
}
