package org.partiql.spi.fn

import org.partiql.spi.connector.ConnectorPath

/**
 * Utility class for an optimized function lookup data structure. Right now this is read only.
 */
@OptIn(FnExperimental::class)
public interface Index<T> {

    /**
     * Search for all functions matching the normalized path.
     *
     * @param path
     * @return
     */
    public fun get(path: List<String>): List<T>

    /**
     * Lookup a function signature by its specific name.
     *
     * @param specific
     * @return
     */
    public fun get(path: ConnectorPath, specific: String): T?

    public abstract class Builder<T> {

        /**
         * A catalog's builtins exposed via INFORMATION_SCHEMA.
         */
        internal val builtins: MutableList<T> = mutableListOf()

        public fun add(fn: T): Builder<T> = this.apply {
            builtins.add(fn)
        }

        public fun addAll(fns: List<T>): Builder<T> = this.apply {
            builtins.addAll(fns)
        }

        /**
         * Creates a map of function name to variants; variants are keyed by their specific.
         *
         * @return
         */
        public abstract fun build(): Index<T>

        public class Fn : Builder<org.partiql.spi.fn.Fn>() {
            override fun build(): Index<org.partiql.spi.fn.Fn> {
                val fns = builtins
                    .groupBy { it.signature.name.uppercase() }
                    .mapValues { e -> e.value.associateBy { f -> f.signature.specific } }
                return IndexMap(fns)
            }
        }

        public class Agg : Builder<org.partiql.spi.fn.Agg>() {
            override fun build(): Index<org.partiql.spi.fn.Agg> {
                val fns = builtins
                    .groupBy { it.signature.name.uppercase() }
                    .mapValues { e -> e.value.associateBy { f -> f.signature.specific } }
                return IndexMap(fns)
            }
        }
    }

    public companion object {

        @JvmStatic
        public fun fnBuilder(): Builder<Fn> = Builder.Fn()

        @JvmStatic
        public fun aggBuilder(): Builder<Agg> = Builder.Agg()
    }
}
