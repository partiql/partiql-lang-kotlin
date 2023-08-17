package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Identifier
import org.partiql.types.PartiQLValueType
import org.partiql.types.PartiQLValueType.BOOL
import org.partiql.types.PartiQLValueType.DECIMAL
import org.partiql.types.PartiQLValueType.FLOAT32
import org.partiql.types.PartiQLValueType.FLOAT64
import org.partiql.types.PartiQLValueType.INT
import org.partiql.types.PartiQLValueType.INT16
import org.partiql.types.PartiQLValueType.INT32
import org.partiql.types.PartiQLValueType.INT64
import org.partiql.types.PartiQLValueType.INT8
import org.partiql.types.PartiQLValueType.NULLABLE_BOOL
import org.partiql.types.PartiQLValueType.NULLABLE_DECIMAL
import org.partiql.types.PartiQLValueType.NULLABLE_FLOAT32
import org.partiql.types.PartiQLValueType.NULLABLE_FLOAT64
import org.partiql.types.PartiQLValueType.NULLABLE_INT
import org.partiql.types.PartiQLValueType.NULLABLE_INT16
import org.partiql.types.PartiQLValueType.NULLABLE_INT32
import org.partiql.types.PartiQLValueType.NULLABLE_INT64
import org.partiql.types.PartiQLValueType.NULLABLE_INT8
import org.partiql.types.PartiQLValueType.NULLABLE_STRING
import org.partiql.types.PartiQLValueType.NULLABLE_SYMBOL
import org.partiql.types.PartiQLValueType.STRING
import org.partiql.types.PartiQLValueType.SYMBOL
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionParameter
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
                StaticType.CHAR, // 12
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
            functions = Functions.all()
        )
    }

    internal object Functions {

        fun all(): List<FunctionSignature> = listOf(
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

        private fun unary(name: String, returns: PartiQLValueType, value: PartiQLValueType) =
            FunctionSignature(
                name = name,
                returns = returns,
                parameters = listOf(FunctionParameter.V("value", value))
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

        private fun binary(name: String, returns: PartiQLValueType, lhs: PartiQLValueType, rhs: PartiQLValueType) =
            FunctionSignature(
                name = name,
                returns = returns,
                parameters = listOf(FunctionParameter.V("lhs", lhs), FunctionParameter.V("rhs", rhs))
            )

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
    }
}
