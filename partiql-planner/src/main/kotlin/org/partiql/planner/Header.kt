package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Identifier
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.function.FunctionSignature

/**
 * A place for type and function definitions. Eventually these will be read from Ion files.
 *
 * @property namespace      Definition namespace e.g. partiql, spark, redshift, ...
 * @property types          Type definitions
 * @property functions      Function definitions
 */
internal class Header(
    private val namespace: String,
    private val types: List<StaticType>,
    private val functions: List<FunctionSignature>,
) {

    /**
     * Return a list of all function signatures matching the given identifier.
     */
    public fun lookup(ref: Fn.Unresolved): List<FunctionSignature> {
        val name = getFnName(ref.identifier)
        return functions.filter { it.name == name }
    }

    /**
     * Return a normalized function identifier for lookup in our list of function definitions.
     */
    private fun getFnName(identifier: Identifier): String = when (identifier) {
        is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
        is Identifier.Symbol -> identifier.symbol.lowercase()
    }

    companion object {

        /**
         * TEMPORARY — Hardcoded PartiQL Global Catalog
         *
         * TODO: Define non-atomic types
         * TODO: Define INT8
         * TODO: Define FLOAT32 vs FLOAT64
         * TODO: Define BIT, BINARY, BYTE
         * TODO: Define INTERVAL
         */
        public fun partiql() = Header(
            namespace = "partiql",
            types = listOf(
                StaticType.ANY, // 0
                StaticType.NULL, // 1
                StaticType.MISSING, // 2
                StaticType.BOOL, // 3
                StaticType.INT2, // 4
                StaticType.INT2, // 5
                StaticType.INT4, // 6
                StaticType.INT8, // 7
                StaticType.INT, // 8
                StaticType.DECIMAL, // 9
                StaticType.FLOAT, // 10
                StaticType.FLOAT, // 11
                char(), // 12
                StaticType.STRING, // 13
                StaticType.SYMBOL, // 14
                // typeAtomic("bit"),       // 15
                // typeAtomic("binary"),    // 16
                // typeAtomic("byte"),      // 17
                StaticType.BLOB, // 18
                StaticType.CLOB, // 19
                StaticType.DATE, // 20
                StaticType.TIME, // 21
                StaticType.TIMESTAMP, // 22
                // typeAtomic("interval"),  // 23
                StaticType.BAG, // 24
                StaticType.LIST, // 25
                StaticType.SEXP, // 26
                StaticType.STRUCT, // 27
            ),
            functions = emptyList(),
            // functions = listOf(
            //     fn(
            //         id = "plus",
            //         params = listOf(v(intT), v(intT)),
            //         returns = intT
            //     ),
            // ),
        )

        private fun char(): StaticType {
            val constraint = StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(1))
            return StringType(constraint)
        }
    }
}
