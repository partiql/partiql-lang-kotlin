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
     */
    data class Result(
        val source: String,
        val root: AstNode,
        val locations: SourceLocations,
    )

    /**
     * Each node is hashable, and has a unique identifier. Metadata is kept externally.
     * Delegate once we are on Kotlin 1.7
     */
    class SourceLocations private constructor(private val delegate: Map<Int, SourceLocation>) :
        Map<Int, SourceLocation> {

        override val entries: Set<Map.Entry<Int, SourceLocation>> = delegate.entries

        override val keys: Set<Int> = delegate.keys

        override val size: Int = delegate.size

        override val values: Collection<SourceLocation> = delegate.values

        override fun containsKey(key: Int): Boolean = delegate.containsKey(key)

        override fun containsValue(value: SourceLocation): Boolean = delegate.containsValue(value)

        override fun get(key: Int): SourceLocation? = delegate[key]

        override fun isEmpty(): Boolean = delegate.isEmpty()

        fun isSynthetic(id: Int, offset: Int = 0): Boolean {
            return delegate[syntheticId(id, offset)] == SourceLocation.SYNTHETIC
        }

        internal class Mutable {

            private val delegate = mutableMapOf<Int, SourceLocation>()

            operator fun set(id: Int, value: SourceLocation) = delegate.put(id, value)

            /**
             * We wish to indicate that the parser has inserted a default value.
             *
             * If offset is zero, this node is itself synthetic
             * If offset is non-zero, this node's n-th parameter (1-indexed) is synthetic
             *
             * Create a unique id that's a function of the id and offset.
             */
            fun markSynthetic(id: Int, offset: Int = 0) {
                delegate[syntheticId(id, offset)] = SourceLocation.SYNTHETIC
            }

            fun toMap() = SourceLocations(delegate)
        }

        private companion object {
            fun syntheticId(id: Int, offset: Int) = -(id * 1_000_000_000 + offset)
        }
    }

    data class SourceLocation(val line: Int, val offset: Int, val length: Int) {

        companion object {

            /**
             * This is a flag for backwards compatibility when converting to the legacy AST.
             */
            val SYNTHETIC = SourceLocation(-1, -1, -1)
        }
    }
}
