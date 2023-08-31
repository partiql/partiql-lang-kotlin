package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Identifier
import org.partiql.planner.typer.TypeLattice
import org.partiql.types.TypingMode
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BAG
import org.partiql.value.PartiQLValueType.BINARY
import org.partiql.value.PartiQLValueType.BLOB
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.BYTE
import org.partiql.value.PartiQLValueType.CHAR
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.INTERVAL
import org.partiql.value.PartiQLValueType.LIST
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.PartiQLValueType.NULL
import org.partiql.value.PartiQLValueType.SEXP
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.STRUCT
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

/**
 * A structure for function lookup.
 */
private typealias FunctionMap = Map<String, List<FunctionSignature>>

/**
 * Map session attributes to underlying function name.
 */
internal val ATTRIBUTES: Map<String, String> = mapOf(
    "CURRENT_USER" to "\$__current_user"
)

/**
 * A place for type and function definitions. Eventually these will be read from Ion files.
 *
 * @property namespace      Definition namespace e.g. partiql, spark, redshift, ...
 * @property types          Type definitions
 * @property functions      Function definitions
 */
@OptIn(PartiQLValueExperimental::class)
internal class Header(
    private val namespace: String,
    private val types: TypeLattice,
    private val functions: FunctionMap,
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
    public fun lookupCast(valueType: PartiQLValueType, targetType: PartiQLValueType): FunctionSignature? {
        val name = castName(targetType)
        val casts = functions.getOrDefault(name, emptyList())
        for (cast in casts) {
            if (cast.parameters.isEmpty()) {
                break // should be unreachable
            }
            if (valueType == cast.parameters[0].type) return cast
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
         * TODO TEMPORARY — Hardcoded PartiQL Global Catalog
         *
         * TODO BUG — We don't validate function overloads
         */
        public fun partiql(mode: TypingMode = TypingMode.STRICT): Header {
            val namespace = "partiql"
            val types = TypeLattice.partiql()
            val functions = Functions.combine(
                Functions.casts(types),
                Functions.operators(),
                Functions.special(),
                Functions.system(),
            ).withAnyArgs()
            return Header(namespace, types, functions)
        }

        /**
         * Define CASTS with some mangled name; CAST(x AS T) -> cast_t(x)
         *
         * CAST(x AS INT8) -> cast_int8(x)
         *
         * But what about parameterized types? Are the parameters dropped in casts, or do parameters become arguments?
         */
        private fun castName(type: PartiQLValueType) = "cast_${type.name.lowercase()}"

        /**
         * For each f(arg_0, ..., arg_n), add an operator f(ANY_0, ..., ANY_n).
         *
         * This isn't entirely correct because we actually want to constraint the possible values of ANY.
         */
        private fun FunctionMap.withAnyArgs(): FunctionMap {
            return entries.associate {
                val name = it.key
                val signatures = it.value
                val variants = signatures.associate { fn -> fn.parameters.size to fn.returns }
                val additions = variants.map { e ->
                    val returns = e.value
                    val params = (0 until e.key).map { i -> FunctionParameter("arg_$i", ANY) }
                    FunctionSignature(name, returns, params)
                }
                // Append the additional ANY signatures
                name to (signatures + additions)
            }
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
            ne(),
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

        /**
         * SQL and PartiQL special forms
         */
        public fun special(): List<FunctionSignature> = listOf(
            like(),
            between(),
            inCollection(),
            isType(),
            coalesce(),
            nullIf(),
            substring(),
            position(),
            trim(),
            overlay(),
            extract(),
            dateAdd(),
            dateDiff(),
        ).flatten()

        public fun system(): List<FunctionSignature> = listOf(
            currentUser(),
        )

        private val allTypes = PartiQLValueType.values()

        private val nullableTypes = listOf(
            NULL, // null.null
            MISSING, // missing
        )

        private val numericTypes = listOf(
            INT8,
            INT16,
            INT32,
            INT64,
            INT,
            DECIMAL,
            FLOAT32,
            FLOAT64,
        )

        // CLOB?
        private val textTypes = listOf(
            STRING,
            SYMBOL,
            CLOB,
        )

        private val collectionTypes = listOf(
            BAG,
            LIST,
            SEXP,
        )

        public fun unary(name: String, returns: PartiQLValueType, value: PartiQLValueType) =
            FunctionSignature(
                name = name,
                returns = returns,
                parameters = listOf(FunctionParameter("value", value)),
                isNullCall = true,
                isNullable = false,
            )

        public fun binary(name: String, returns: PartiQLValueType, lhs: PartiQLValueType, rhs: PartiQLValueType) =
            FunctionSignature(
                name = name,
                returns = returns,
                parameters = listOf(FunctionParameter("lhs", lhs), FunctionParameter("rhs", rhs)),
                isNullCall = true,
                isNullable = false,
            )

        public fun cast(value: PartiQLValueType, type: PartiQLValueType) =
            FunctionSignature(
                name = castName(type),
                returns = type,
                parameters = listOf(
                    FunctionParameter("value", value),
                )
            )

        // OPERATORS

        private fun not(): List<FunctionSignature> = listOf(unary("not", BOOL, BOOL))

        private fun pos(): List<FunctionSignature> = numericTypes.map { t ->
            unary("pos", t, t)
        }

        private fun neg(): List<FunctionSignature> = numericTypes.map { t ->
            unary("neg", t, t)
        }

        private fun eq(): List<FunctionSignature> = allTypes.map { t ->
            binary("eq", BOOL, t, t)
        }

        private fun ne(): List<FunctionSignature> = allTypes.map { t ->
            binary("ne", BOOL, t, t)
        }

        private fun and(): List<FunctionSignature> = listOf(binary("and", BOOL, BOOL, BOOL))

        private fun or(): List<FunctionSignature> = listOf(binary("or", BOOL, BOOL, BOOL))

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

        // SPECIAL FORMS

        private fun like(): List<FunctionSignature> = listOf(
            FunctionSignature(
                name = "like",
                returns = BOOL,
                parameters = listOf(
                    FunctionParameter("value", STRING),
                    FunctionParameter("pattern", STRING),
                ),
                isNullCall = true,
                isNullable = false,
            ),
            FunctionSignature(
                name = "like_escape",
                returns = BOOL,
                parameters = listOf(
                    FunctionParameter("value", STRING),
                    FunctionParameter("pattern", STRING),
                    FunctionParameter("escape", STRING),
                ),
                isNullCall = true,
                isNullable = false,
            ),
        )

        private fun between(): List<FunctionSignature> = numericTypes.map { t ->
            FunctionSignature(
                name = "between",
                returns = BOOL,
                parameters = listOf(
                    FunctionParameter("value", t),
                    FunctionParameter("lower", t),
                    FunctionParameter("upper", t),
                ),
                isNullCall = true,
                isNullable = false,
            )
        }

        private fun inCollection(): List<FunctionSignature> = allTypes.map { element ->
            collectionTypes.map { collection ->
                FunctionSignature(
                    name = "in_collection",
                    returns = BOOL,
                    parameters = listOf(
                        FunctionParameter("value", element),
                        FunctionParameter("collection", collection),
                    ),
                    isNullCall = true,
                    isNullable = false,
                )
            }
        }.flatten()

        // TODO
        private fun isType(): List<FunctionSignature> = emptyList()

        // TODO
        private fun coalesce(): List<FunctionSignature> = emptyList()

        private fun nullIf(): List<FunctionSignature> = nullableTypes.map { t ->
            FunctionSignature(
                name = "null_if",
                returns = t,
                parameters = listOf(
                    FunctionParameter("value", t),
                    FunctionParameter("nullifier", BOOL),
                ),
                isNullCall = true,
                isNullable = true,
            )
        }

        private fun substring(): List<FunctionSignature> = textTypes.map { t ->
            listOf(
                FunctionSignature(
                    name = "substring",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                        FunctionParameter("start", INT64),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature(
                    name = "substring_length",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                        FunctionParameter("start", INT64),
                        FunctionParameter("end", INT64),
                    ),
                    isNullCall = true,
                    isNullable = false,
                )
            )
        }.flatten()

        private fun position(): List<FunctionSignature> = textTypes.map { t ->
            FunctionSignature(
                name = "position",
                returns = INT64,
                parameters = listOf(
                    FunctionParameter("probe", t),
                    FunctionParameter("value", t),
                ),
                isNullCall = true,
                isNullable = false,
            )
        }

        private fun trim(): List<FunctionSignature> = textTypes.map { t ->
            listOf(
                FunctionSignature(
                    name = "trim",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature(
                    name = "trim_chars",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                        FunctionParameter("chars", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature(
                    name = "trim_leading",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature(
                    name = "trim_leading_chars",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                        FunctionParameter("chars", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature(
                    name = "trim_trailing",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature(
                    name = "trim_trailing_chars",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                        FunctionParameter("chars", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
            )
        }.flatten()

        // TODO
        private fun overlay(): List<FunctionSignature> = emptyList()

        // TODO
        private fun extract(): List<FunctionSignature> = emptyList()

        // TODO
        private fun dateAdd(): List<FunctionSignature> = emptyList()

        // TODO
        private fun dateDiff(): List<FunctionSignature> = emptyList()

        private fun currentUser() = FunctionSignature(
            name = "\$__current_user",
            returns = STRING,
            parameters = emptyList(),
            isNullable = true,
        )

        // Function precedence comparator
        // 1. Fewest args first
        // 2. Parameters are compared left-to-right
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

        private fun FunctionParameter.compareTo(other: FunctionParameter): Int =
            comparePrecedence(this.type, other.type)

        private fun comparePrecedence(t1: PartiQLValueType, t2: PartiQLValueType): Int {
            if (t1 == t2) return 0
            val p1 = typePrecedence[t1]!!
            val p2 = typePrecedence[t2]!!
            return p1 - p2
        }

        // This simply describes some precedence for ordering functions.
        // This is not explicitly defined in the PartiQL Specification
        // This does not imply the ability to CAST.
        private val typePrecedence: Map<PartiQLValueType, Int> = listOf(
            NULL,
            MISSING,
            BOOL,
            INT8,
            INT16,
            INT32,
            INT64,
            INT,
            DECIMAL,
            FLOAT32,
            FLOAT64,
            CHAR,
            STRING,
            CLOB,
            SYMBOL,
            BINARY,
            BYTE,
            BLOB,
            DATE,
            TIME,
            TIMESTAMP,
            INTERVAL,
            BAG,
            LIST,
            SEXP,
            STRUCT,
            ANY,
        ).mapIndexed { precedence, type -> type to precedence }.toMap()
    }
}
