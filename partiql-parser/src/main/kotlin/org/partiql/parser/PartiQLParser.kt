package org.partiql.parser

import org.partiql.ast.AstNode

/**
 * PartiQL Parser interface.
 */
interface PartiQLParser {

    @Throws(PartiQLParserException::class, InterruptedException::class)
    fun parse(source: String): Result

    /**
     * PartiQL Parser Result
     *
     * @property root
     * @property locations
     */
    data class Result(
        val source: String,
        val root: AstNode,
        val locations: SourceLocations,
    )

    // Delegate once we are on Kotlin 1.7
    class SourceLocations(
        private val source: String,
        private val locations: Map<Int, SourceLocation>,
    ) : Map<Int, SourceLocation> {

        // TODO
        fun getSource(node: AstNode): String {
            return source
        }

        override val entries: Set<Map.Entry<Int, SourceLocation>> = locations.entries

        override val keys: Set<Int> = locations.keys

        override val size: Int = locations.size

        override val values: Collection<SourceLocation> = locations.values

        override fun containsKey(key: Int): Boolean = locations.containsKey(key)

        override fun containsValue(value: SourceLocation): Boolean = locations.containsValue(value)

        override fun get(key: Int): SourceLocation? = locations[key]

        override fun isEmpty(): Boolean = locations.isEmpty()
    }

    data class SourceLocation(val line: Int, val offset: Int, val length: Int) {
        companion object {
            val UNKNOWN = SourceLocation(-1, -1, -1)
        }
    }
}
