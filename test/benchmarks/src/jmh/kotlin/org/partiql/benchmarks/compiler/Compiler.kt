package org.partiql.benchmarks.compiler

interface Compiler {
    fun compile(query: String): Iterable<Any>
}
