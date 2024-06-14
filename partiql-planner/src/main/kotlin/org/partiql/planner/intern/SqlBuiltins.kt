package org.partiql.planner.intern

import org.partiql.planner.intern.builtins.SqlDefinition
import org.partiql.planner.intern.builtins.SqlStringBuiltins
import org.partiql.planner.metadata.Fn
import org.partiql.planner.metadata.Operator

/**
 * These are the system-level definitions; that is, these are ALWAYS on the PATH.
 */
internal class SqlBuiltins private constructor(
    private val operators: Map<String, List<Operator>>,
    private val functions: Map<String, List<Fn>>,
) {

    /**
     * Lookup an operator by its symbol.
     */
    internal fun getOperators(symbol: String): List<Operator> = operators[symbol] ?: emptyList()

    /**
     * Lookup functions by name.
     */
    internal fun getFunctions(name: String): List<Fn> = functions[name] ?: emptyList()

    companion object {

        @JvmStatic
        fun all(): SqlBuiltins = Builder()
            .define(SqlStringBuiltins.ALL)
            .build()
    }

    class Builder {

        private val operators = mutableMapOf<String, List<Operator>>()
        private val functions = mutableMapOf<String, List<Fn>>()

        /**
         * Helper function to add all definitions.
         */
        fun define(definitions: List<SqlDefinition>): Builder {
            for (definition in definitions) {
                when (definition) {
                    is SqlDefinition.Operator -> define(definition)
                    is SqlDefinition.Fn -> define(definition)
                }
            }
            return this
        }

        /**
         * Define an operator.
         */
        fun define(definition: SqlDefinition.Operator): Builder {
            val variants = definition.getVariants().associateBy { it.getSymbol() }
            for (v in variants) {
                val symbol = v.key
                val definitions = v.value
                val current = operators.getOrDefault(symbol, emptyList())
                operators[symbol] = current + definitions
            }
            return this
        }

        /**
         * Define a function.
         */
        fun define(definition: SqlDefinition.Fn): Builder {
            val variants = definition.getVariants().associateBy { it.getName() }
            for (v in variants) {
                val name = v.key
                val definitions = v.value
                val current = functions.getOrDefault(name, emptyList())
                functions[name] = current + definitions
            }
            return this
        }

        fun build(): SqlBuiltins = SqlBuiltins(operators, functions)
    }
}
