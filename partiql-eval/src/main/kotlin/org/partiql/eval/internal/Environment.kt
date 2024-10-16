package org.partiql.eval.internal

import org.partiql.spi.value.Datum
import java.util.EmptyStackException
import java.util.Stack

/**
 * This class holds the evaluation environment i.e. the parameters and
 */
internal class Environment(parameters: Parameters) {

    /**
     * Parameters are internalized and may be revised.
     */
    private var parameters: Parameters = parameters

    /**
     * This stack holds the interpreter state.
     */
    private var scope: Stack<Row> = Stack()

    /**
     * Empty parameters.
     */
    constructor() : this(Parameters.EMPTY)

    /**
     * Inline helper for introducing a scope for an expression.
     *
     * @param row
     * @param block
     */
    inline fun <T> scope(row: Row, block: () -> T): T {
        push(row)
        return try {
            block()
        } finally {
            pop()
        }
    }

    /**
     * Push a new row onto the stack; this can be thought of like a "frame".
     *
     * @param row
     */
    fun push(row: Row) {
        scope.push(row)
    }

    /**
     * Pop the top row off the stack.
     *
     * @throws EmptyStackException if the stack is empty.
     * @see Stack.pop
     */
    @Throws(EmptyStackException::class)
    fun pop() {
        scope.pop()
    }

    /**
     * Get a variable
     *
     * @param depth
     * @param offset
     * @return
     */
    fun get(depth: Int, offset: Int): Datum = try {
        scope[depth][offset]
    } catch (ex: IndexOutOfBoundsException) {
        throw RuntimeException("Invalid variable reference [$depth:$offset]\n$this")
    }

    /**
     * Print the stack.
     */
    override fun toString(): String = buildString {
        appendLine("[stack]--------------")
        for ((i, row) in scope.withIndex()) {
            appendLine("$i: $row")
            appendLine("---------------------")
        }
        if (scope.isEmpty()) {
            appendLine("empty")
            appendLine("---------------------")
        }
    }
}
