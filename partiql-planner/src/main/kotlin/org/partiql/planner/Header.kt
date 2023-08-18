package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Identifier
import org.partiql.planner.typer.TypeLattice
import org.partiql.types.PartiQLValueType
import org.partiql.types.PartiQLValueType.BAG
import org.partiql.types.PartiQLValueType.BINARY
import org.partiql.types.PartiQLValueType.BLOB
import org.partiql.types.PartiQLValueType.BOOL
import org.partiql.types.PartiQLValueType.BYTE
import org.partiql.types.PartiQLValueType.CHAR
import org.partiql.types.PartiQLValueType.CLOB
import org.partiql.types.PartiQLValueType.DATE
import org.partiql.types.PartiQLValueType.DECIMAL
import org.partiql.types.PartiQLValueType.FLOAT32
import org.partiql.types.PartiQLValueType.FLOAT64
import org.partiql.types.PartiQLValueType.GRAPH
import org.partiql.types.PartiQLValueType.INT
import org.partiql.types.PartiQLValueType.INT16
import org.partiql.types.PartiQLValueType.INT32
import org.partiql.types.PartiQLValueType.INT64
import org.partiql.types.PartiQLValueType.INT8
import org.partiql.types.PartiQLValueType.INTERVAL
import org.partiql.types.PartiQLValueType.LIST
import org.partiql.types.PartiQLValueType.MISSING
import org.partiql.types.PartiQLValueType.NULL
import org.partiql.types.PartiQLValueType.NULLABLE_BAG
import org.partiql.types.PartiQLValueType.NULLABLE_BINARY
import org.partiql.types.PartiQLValueType.NULLABLE_BLOB
import org.partiql.types.PartiQLValueType.NULLABLE_BOOL
import org.partiql.types.PartiQLValueType.NULLABLE_BYTE
import org.partiql.types.PartiQLValueType.NULLABLE_CHAR
import org.partiql.types.PartiQLValueType.NULLABLE_CLOB
import org.partiql.types.PartiQLValueType.NULLABLE_DATE
import org.partiql.types.PartiQLValueType.NULLABLE_DECIMAL
import org.partiql.types.PartiQLValueType.NULLABLE_FLOAT32
import org.partiql.types.PartiQLValueType.NULLABLE_FLOAT64
import org.partiql.types.PartiQLValueType.NULLABLE_INT
import org.partiql.types.PartiQLValueType.NULLABLE_INT16
import org.partiql.types.PartiQLValueType.NULLABLE_INT32
import org.partiql.types.PartiQLValueType.NULLABLE_INT64
import org.partiql.types.PartiQLValueType.NULLABLE_INT8
import org.partiql.types.PartiQLValueType.NULLABLE_INTERVAL
import org.partiql.types.PartiQLValueType.NULLABLE_LIST
import org.partiql.types.PartiQLValueType.NULLABLE_SEXP
import org.partiql.types.PartiQLValueType.NULLABLE_STRING
import org.partiql.types.PartiQLValueType.NULLABLE_STRUCT
import org.partiql.types.PartiQLValueType.NULLABLE_SYMBOL
import org.partiql.types.PartiQLValueType.NULLABLE_TIME
import org.partiql.types.PartiQLValueType.NULLABLE_TIMESTAMP
import org.partiql.types.PartiQLValueType.SEXP
import org.partiql.types.PartiQLValueType.STRING
import org.partiql.types.PartiQLValueType.STRUCT
import org.partiql.types.PartiQLValueType.SYMBOL
import org.partiql.types.PartiQLValueType.TIME
import org.partiql.types.PartiQLValueType.TIMESTAMP
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature

/**
 * A structure for function lookup.
 */
private typealias FunctionMap = Map<String, List<FunctionSignature>>

/**
 * A place for type and function definitions. Eventually these will be read from Ion files.
 *
 * @property namespace      Definition namespace e.g. partiql, spark, redshift, ...
 * @property types          Type definitions
 * @property functions      Function definitions
 */
internal class Header(
    private val namespace: String,
    private val types: TypeLattice,
    private val functions: Map<String, List<FunctionSignature>>,
) {

    /**
     * Return a list of all function signatures matching the given identifier.
     */
    public fun lookup(ref: Fn.Unresolved): List<FunctionSignature> {
        val name = getFnName(ref.identifier)
        return functions.getOrDefault(name, emptyList())
    }

    /**
     * Returns the CAST function if exists, else null.
     */
    public fun lookupCast(t1: PartiQLValueType, t2: PartiQLValueType): FunctionSignature? {
        val casts = functions.getOrDefault("cast", emptyList())
        for (cast in casts) {
            if (cast.parameters.size != 2) {
                break // should be unreachable
            }
            val p1 = cast.parameters[0]
            val p2 = cast.parameters[1]
            if (p1 is FunctionParameter.V || p2 is FunctionParameter.T) {
                if (t1 == p1.type && t2 == p2.type) {
                    return cast
                }
            }
        }
        return null
    }

    /**
     * Return a normalized function identifier for lookup in our list of function definitions.
     */
    private fun getFnName(identifier: Identifier): String = when (identifier) {
        is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
        is Identifier.Symbol -> identifier.symbol.lowercase()
    }

    /**
     * Dump the Header as SQL commands
     *
     * For functions, output CREATE FUNCTION statements.
     */
    override fun toString(): String = buildString {
        functions.forEach {
            appendLine("-- [${it.key}] ---------")
            appendLine()
            it.value.forEach { fn -> appendLine(fn) }
            appendLine()
        }
    }

    companion object {

        /**
         * TEMPORARY — Hardcoded PartiQL Global Catalog
         */
        public fun partiql(): Header {
            val namespace = "partiql"
            val types = TypeLattice.partiql()
            val functions = Functions.combine(
                Functions.casts(types),
                Functions.operators(),
            )
            return Header(namespace, types, functions)
        }
    }

    /**
     * Utilities for building function signatures for the header / symbol table.
     */
    internal object Functions {

        /**
         * Produce a function map (grouping by name) from a list of signatures.
         */
        public fun combine(vararg functions: List<FunctionSignature>): FunctionMap {
            return functions.flatMap { it.sortedWith(functionPrecedence) }.groupBy { it.name }
        }

        /**
         * Generate all "safe" CAST functions from the given lattice.
         */
        public fun casts(lattice: TypeLattice): List<FunctionSignature> = lattice.implicitCasts().map {
            cast(it.first, it.second)
        }

        /**
         * Generate all unary and binary operator signatures.
         */
        public fun operators(): List<FunctionSignature> = listOf(
            not(),
            pos(),
            neg(),
            eq(),
            neq(),
            and(),
            or(),
            lt(),
            lte(),
            gt(),
            gte(),
            plus(),
            minus(),
            times(),
            div(),
            mod(),
            concat(),
        ).flatten()

        private val allTypes = PartiQLValueType.values()

        private val numericTypes = listOf(
            INT8,
            INT16,
            INT32,
            INT64,
            INT,
            DECIMAL,
            FLOAT32,
            FLOAT64,
            NULLABLE_INT8, // null.int8
            NULLABLE_INT16, // null.int16
            NULLABLE_INT32, // null.int32
            NULLABLE_INT64, // null.int64
            NULLABLE_INT, // null.int
            NULLABLE_DECIMAL, // null.decimal
            NULLABLE_FLOAT32, // null.float32
            NULLABLE_FLOAT64, // null.float64
        )

        private val textTypes = listOf(
            STRING,
            SYMBOL,
            NULLABLE_STRING,
            NULLABLE_SYMBOL,
        )

        public fun unary(name: String, returns: PartiQLValueType, value: PartiQLValueType) =
            FunctionSignature(
                name = name,
                returns = returns,
                parameters = listOf(FunctionParameter.V("value", value))
            )

        public fun binary(name: String, returns: PartiQLValueType, lhs: PartiQLValueType, rhs: PartiQLValueType) =
            FunctionSignature(
                name = name,
                returns = returns,
                parameters = listOf(FunctionParameter.V("lhs", lhs), FunctionParameter.V("rhs", rhs))
            )

        public fun cast(value: PartiQLValueType, type: PartiQLValueType) =
            FunctionSignature(
                name = "cast",
                returns = type,
                parameters = listOf(
                    FunctionParameter.V("value", value),
                    FunctionParameter.T("type", type),
                )
            )

        private fun not(): List<FunctionSignature> = listOf(BOOL, NULLABLE_BOOL).map { t ->
            unary("not", BOOL, BOOL)
        }

        private fun pos(): List<FunctionSignature> = numericTypes.map { t ->
            unary("pos", t, t)
        }

        private fun neg(): List<FunctionSignature> = numericTypes.map { t ->
            unary("neg", t, t)
        }

        private fun eq(): List<FunctionSignature> = allTypes.map { t ->
            binary("eq", BOOL, t, t)
        }

        private fun neq(): List<FunctionSignature> = allTypes.map { t ->
            binary("neq", BOOL, t, t)
        }

        private fun and(): List<FunctionSignature> = listOf(BOOL, NULLABLE_BOOL).map { t ->
            binary("and", BOOL, t, t)
        }

        private fun or(): List<FunctionSignature> = listOf(BOOL, NULLABLE_BOOL).map { t ->
            binary("or", BOOL, t, t)
        }

        private fun lt(): List<FunctionSignature> = numericTypes.map { t ->
            binary("lt", BOOL, t, t)
        }

        private fun lte(): List<FunctionSignature> = numericTypes.map { t ->
            binary("lte", BOOL, t, t)
        }

        private fun gt(): List<FunctionSignature> = numericTypes.map { t ->
            binary("gt", BOOL, t, t)
        }

        private fun gte(): List<FunctionSignature> = numericTypes.map { t ->
            binary("gte", BOOL, t, t)
        }

        private fun plus(): List<FunctionSignature> = numericTypes.map { t ->
            binary("plus", t, t, t)
        }

        private fun minus(): List<FunctionSignature> = numericTypes.map { t ->
            binary("minus", t, t, t)
        }

        private fun times(): List<FunctionSignature> = numericTypes.map { t ->
            binary("times", t, t, t)
        }

        private fun div(): List<FunctionSignature> = numericTypes.map { t ->
            binary("div", t, t, t)
        }

        private fun mod(): List<FunctionSignature> = numericTypes.map { t ->
            binary("mod", t, t, t)
        }

        private fun concat(): List<FunctionSignature> = textTypes.map { t ->
            binary("concat", t, t, t)
        }

        // Function precedence comparator
        // 1. Fewest args first
        // 2. Type parameters sort before value parameters
        // 3. Parameters are compared left-to-right
        private val functionPrecedence = Comparator<FunctionSignature> { fn1, fn2 ->
            // Compare number of arguments
            if (fn1.parameters.size != fn2.parameters.size) {
                return@Comparator fn1.parameters.size - fn2.parameters.size
            }
            // Compare operand type precedence
            for (i in fn1.parameters.indices) {
                val p1 = fn1.parameters[i]
                val p2 = fn2.parameters[i]
                val comparison = p1.compareTo(p2)
                if (comparison != 0) return@Comparator comparison
            }
            // unreachable?
            0
        }

        private fun FunctionParameter.compareTo(other: FunctionParameter): Int = when {
            (this is FunctionParameter.T && other is FunctionParameter.V) -> 1
            (this is FunctionParameter.V && other is FunctionParameter.T) -> -1
            (this is FunctionParameter.T && other is FunctionParameter.T) -> 0
            (this is FunctionParameter.V && other is FunctionParameter.V) -> comparePrecedence(this.type, other.type)
            else -> 0 // unreachable
        }

        // This is not explicitly defined in the PartiQL Specification
        private fun comparePrecedence(t1: PartiQLValueType, t2: PartiQLValueType): Int {
            if (t1 == t2) return 0
            val p1 = typePrecedence[t1]!!
            val p2 = typePrecedence[t2]!!
            return p1 - p2
        }

        // This simply describes some precedence for ordering functions.
        // It does not necessarily imply the ability to CAST!
        // This will be replaced by a lattice in the near future!
        private val typePrecedence = mapOf(
            NULL to 0,
            MISSING to 0,
            BOOL to 1,
            NULLABLE_BOOL to 2,
            INT8 to 3,
            NULLABLE_INT8 to 4,
            INT16 to 5,
            NULLABLE_INT16 to 6,
            INT32 to 7,
            NULLABLE_INT32 to 8,
            INT64 to 9,
            NULLABLE_INT64 to 10,
            INT to 11,
            NULLABLE_INT to 12,
            DECIMAL to 13,
            NULLABLE_DECIMAL to 14,
            FLOAT32 to 15,
            NULLABLE_FLOAT32 to 16,
            FLOAT64 to 17,
            NULLABLE_FLOAT64 to 18,
            CHAR to 19,
            NULLABLE_CHAR to 20,
            STRING to 21,
            NULLABLE_STRING to 22,
            SYMBOL to 23,
            NULLABLE_SYMBOL to 24,
            CLOB to 25,
            NULLABLE_CLOB to 26,
            BINARY to 27,
            NULLABLE_BINARY to 28,
            BYTE to 29,
            NULLABLE_BYTE to 30,
            BLOB to 31,
            NULLABLE_BLOB to 32,
            DATE to 33,
            NULLABLE_DATE to 34,
            TIME to 35,
            NULLABLE_TIME to 36,
            TIMESTAMP to 37,
            NULLABLE_TIMESTAMP to 38,
            INTERVAL to 39,
            NULLABLE_INTERVAL to 40,
            LIST to 41,
            NULLABLE_LIST to 42,
            BAG to 43,
            NULLABLE_BAG to 44,
            SEXP to 45,
            NULLABLE_SEXP to 46,
            STRUCT to 47,
            NULLABLE_STRUCT to 48,
            GRAPH to 49,
        )
    }
}
