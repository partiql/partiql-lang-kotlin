package org.partiql.planner.intern

import org.partiql.planner.intern.builtins.SqlOp
import org.partiql.planner.intern.builtins.SqlOpConcat
import org.partiql.planner.intern.builtins.SqlStringBuiltins
import org.partiql.planner.metadata.Routine

/**
 * These are the system-level definitions; that is, these are ALWAYS on the PATH.
 *
 * See https://github.com/apache/calcite/blob/main/core/src/main/java/org/apache/calcite/sql/fun/SqlLibraryOperators.java
 */
internal class SqlBuiltins private constructor(
    private val operators: Map<String, List<SqlOp>>,
    private val functions: Map<String, List<Routine>>,
) {

    companion object {

        @JvmStatic
        fun all(): SqlBuiltins = Builder()
            .define(SqlStringBuiltins.ALL)
            .build()
    }

    class Builder {

        private val operators = mutableMapOf<String, List<SqlOp>>()
        private val functions = mutableMapOf<String, List<Routine>>()

        /**
         * Helper function to add all definitions.
         */
        fun define(definitions: List<SqlDefinition>): Builder {
            for (definition in definitions) {
                when (definition) {
                    is SqlDefinition.SqlOp -> define(definition)
                    is SqlDefinition.Fn -> define(definition)
                }
            }
            return this
        }

        /**
         * Define an operator.
         */
        fun define(definition: SqlDefinition.SqlOp): Builder {
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
