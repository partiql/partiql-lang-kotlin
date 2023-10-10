package org.partiql.planner

import org.partiql.ast.DatetimeField
import org.partiql.plan.Agg
import org.partiql.plan.Fn
import org.partiql.plan.Identifier
import org.partiql.planner.typer.CastType
import org.partiql.planner.typer.TypeLattice
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
 * A structure for scalar function lookup.
 */
private typealias FnMap<T> = Map<String, List<T>>

/**
 * Map session attributes to underlying function name.
 */
internal val ATTRIBUTES: Map<String, String> = mapOf(
    "CURRENT_USER" to "\$__current_user",
    "CURRENT_DATE" to "\$__current_date"
)

/**
 * A place for type and function definitions. Eventually these will be read from Ion files.
 *
 * @property namespace      Definition namespace e.g. partiql, spark, redshift, ...
 * @property types          Type definitions
 * @property functions      Scalar function definitions
 * @property aggregations   Aggregation function definitions
 */
@OptIn(PartiQLValueExperimental::class)
internal class Header(
    private val namespace: String,
    private val types: TypeLattice,
    private val functions: FnMap<FunctionSignature.Scalar>,
    private val aggregations: FnMap<FunctionSignature.Aggregation>,
    private val unsafeCastSet: Set<String>,
) {

    /**
     * Return a list of all scalar function signatures matching the given identifier.
     */
    public fun lookup(ref: Fn.Unresolved): List<FunctionSignature.Scalar> {
        val name = getFnName(ref.identifier)
        return functions.getOrDefault(name, emptyList())
    }

    /**
     * Return a list of all aggregation function signatures matching the given identifier.
     */
    public fun lookup(ref: Agg.Unresolved): List<FunctionSignature.Aggregation> {
        val name = getFnName(ref.identifier)
        return aggregations.getOrDefault(name, emptyList())
    }

    /**
     * Returns the CAST function if exists, else null.
     */
    public fun lookupCoercion(valueType: PartiQLValueType, targetType: PartiQLValueType): FunctionSignature.Scalar? {
        if (!types.canCoerce(valueType, targetType)) {
            return null
        }
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
     * Easy lookup of whether this CAST can return MISSING.
     */
    public fun isUnsafeCast(specific: String): Boolean = unsafeCastSet.contains(specific)

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
         */
        public fun partiql(): Header {
            val namespace = "partiql"
            val types = TypeLattice.partiql()
            val (casts, unsafeCastSet) = Functions.casts(types)
            val functions = Functions.combine(
                casts,
                Functions.operators(),
                Functions.builtins(),
                Functions.system(),
            )
            val aggregations = Functions.combine(
                Functions.aggregations(),
            )
            return Header(namespace, types, functions, aggregations, unsafeCastSet)
        }

        /**
         * Define CASTS with some mangled name; CAST(x AS T) -> cast_t(x)
         *
         * CAST(x AS INT8) -> cast_int64(x)
         *
         * But what about parameterized types? Are the parameters dropped in casts, or do parameters become arguments?
         */
        private fun castName(type: PartiQLValueType) = "cast_${type.name.lowercase()}"
    }

    /**
     * Utilities for building function signatures for the header / symbol table.
     */
    internal object Functions {

        /**
         * Group list of [FunctionSignature.Scalar] by name.
         */
        public fun <T : FunctionSignature> combine(vararg functions: List<T>): FnMap<T> {
            return functions.flatMap { it.sortedWith(functionPrecedence) }.groupBy { it.name }
        }

        // ====================================
        //  TYPES
        // ====================================

        private val allTypes = PartiQLValueType.values()

        private val nullableTypes = listOf(
            NULL, // null.null
            MISSING, // missing
        )

        private val intTypes = listOf(
            INT8,
            INT16,
            INT32,
            INT64,
            INT,
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

        private val datetimeTypes = listOf(
            DATE,
            TIME,
            TIMESTAMP,
        )

        // ====================================
        //  SCALAR FUNCTIONS
        // ====================================

        /**
         * Generate all CAST functions from the given lattice.
         *
         * @param lattice
         * @return Pair(0) is the function list, Pair(1) represents the unsafe cast specifics
         */
        public fun casts(lattice: TypeLattice): Pair<List<FunctionSignature.Scalar>, Set<String>> {
            val casts = mutableListOf<FunctionSignature.Scalar>()
            val unsafeCastSet = mutableSetOf<String>()
            for (t1 in lattice.types) {
                for (t2 in lattice.types) {
                    val r = lattice.graph[t1.ordinal][t2.ordinal]
                    if (r != null) {
                        val fn = cast(t1, t2)
                        casts.add(fn)
                        if (r.cast == CastType.UNSAFE) unsafeCastSet.add(fn.specific)
                    }
                }
            }
            return casts to unsafeCastSet
        }

        /**
         * Generate all unary and binary operator signatures.
         */
        public fun operators(): List<FunctionSignature.Scalar> = listOf(
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
            bitwiseAnd(),
        ).flatten()

        /**
         * SQL and PartiQL Scalar Builtins
         */
        public fun builtins(): List<FunctionSignature.Scalar> = listOf(
            upper(),
            lower(),
            like(),
            between(),
            inCollection(),
            isType(),
            isTypeSingleArg(),
            isTypeDoubleArgsInt(),
            isTypeTime(),
            coalesce(),
            nullIf(),
            substring(),
            position(),
            trim(),
            overlay(),
            extract(),
            dateAdd(),
            dateDiff(),
            utcNow(),
        ).flatten()

        /**
         * System functions (for now, CURRENT_USER and CURRENT_DATE)
         *
         * @return
         */
        public fun system(): List<FunctionSignature.Scalar> = listOf(
            currentUser(),
            currentDate(),
        )

        // OPERATORS

        private fun not(): List<FunctionSignature.Scalar> = listOf(unary("not", BOOL, BOOL))

        private fun pos(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            unary("pos", t, t)
        }

        private fun neg(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            unary("neg", t, t)
        }

        private fun eq(): List<FunctionSignature.Scalar> = allTypes.map { t ->
            FunctionSignature.Scalar(
                name = "eq",
                returns = BOOL,
                isNullCall = false,
                isNullable = false,
                parameters = listOf(FunctionParameter("lhs", t), FunctionParameter("rhs", t)),
            )
        }

        private fun ne(): List<FunctionSignature.Scalar> = allTypes.map { t ->
            binary("ne", BOOL, t, t)
        }

        private fun and(): List<FunctionSignature.Scalar> = listOf(binary("and", BOOL, BOOL, BOOL))

        private fun or(): List<FunctionSignature.Scalar> = listOf(binary("or", BOOL, BOOL, BOOL))

        private fun lt(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            binary("lt", BOOL, t, t)
        }

        private fun lte(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            binary("lte", BOOL, t, t)
        }

        private fun gt(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            binary("gt", BOOL, t, t)
        }

        private fun gte(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            binary("gte", BOOL, t, t)
        }

        private fun plus(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            binary("plus", t, t, t)
        }

        private fun minus(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            binary("minus", t, t, t)
        }

        private fun times(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            binary("times", t, t, t)
        }

        private fun div(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            binary("divide", t, t, t)
        }

        private fun mod(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            binary("modulo", t, t, t)
        }

        private fun concat(): List<FunctionSignature.Scalar> = textTypes.map { t ->
            binary("concat", t, t, t)
        }

        private fun bitwiseAnd(): List<FunctionSignature.Scalar> = intTypes.map { t ->
            binary("bitwise_and", t, t, t)
        }

        // BUILT INTS

        private fun upper(): List<FunctionSignature.Scalar> = textTypes.map { t ->
            FunctionSignature.Scalar(
                name = "upper",
                returns = t,
                parameters = listOf(FunctionParameter("value", t)),
                isNullCall = true,
                isNullable = false,
            )
        }

        private fun lower(): List<FunctionSignature.Scalar> = textTypes.map { t ->
            FunctionSignature.Scalar(
                name = "lower",
                returns = t,
                parameters = listOf(FunctionParameter("value", t)),
                isNullCall = true,
                isNullable = false,
            )
        }

        // SPECIAL FORMS

        private fun like(): List<FunctionSignature.Scalar> = listOf(
            FunctionSignature.Scalar(
                name = "like",
                returns = BOOL,
                parameters = listOf(
                    FunctionParameter("value", STRING),
                    FunctionParameter("pattern", STRING),
                ),
                isNullCall = true,
                isNullable = false,
            ),
            FunctionSignature.Scalar(
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

        private fun between(): List<FunctionSignature.Scalar> = numericTypes.map { t ->
            FunctionSignature.Scalar(
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

        private fun inCollection(): List<FunctionSignature.Scalar> = allTypes.map { element ->
            collectionTypes.map { collection ->
                FunctionSignature.Scalar(
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

        // To model type assertion, generating a list of assertion function based on the type,
        // and the parameter will be the value entered.
        //  i.e., 1 is INT2  => is_int16(1)
        // TODO: We can remove the types with parameter in this function.
        //  but, leaving out the decision to have, for example:
        //  is_decimal(null, null, value) vs is_decimal(value) later....
        private fun isType(): List<FunctionSignature.Scalar> = allTypes.map { element ->
            FunctionSignature.Scalar(
                name = "is_${element.name.lowercase()}",
                returns = BOOL,
                parameters = listOf(
                    FunctionParameter("value", ANY) // TODO: Decide if we need to further segment this
                ),
                isNullCall = false,
                isNullable = false
            )
        }

        // In type assertion, it is possible for types to have args
        // i.e., 'a' is CHAR(2)
        // we put type parameter before value.
        private fun isTypeSingleArg(): List<FunctionSignature.Scalar> = listOf(CHAR, STRING).map { element ->
            FunctionSignature.Scalar(
                name = "is_${element.name.lowercase()}",
                returns = BOOL,
                parameters = listOf(
                    FunctionParameter("type_parameter_1", INT32),
                    FunctionParameter("value", ANY) // TODO: Decide if we need to further segment this
                ),
                isNullCall = false,
                isNullable = false
            )
        }

        private fun isTypeDoubleArgsInt(): List<FunctionSignature.Scalar> = listOf(DECIMAL).map { element ->
            FunctionSignature.Scalar(
                name = "is_${element.name.lowercase()}",
                returns = BOOL,
                parameters = listOf(
                    FunctionParameter("type_parameter_1", INT32),
                    FunctionParameter("type_parameter_2", INT32),
                    FunctionParameter("value", ANY) // TODO: Decide if we need to further segment this
                ),
                isNullCall = false,
                isNullable = false
            )
        }

        private fun isTypeTime(): List<FunctionSignature.Scalar> = listOf(TIME, TIMESTAMP).map { element ->
            FunctionSignature.Scalar(
                name = "is_${element.name.lowercase()}",
                returns = BOOL,
                parameters = listOf(
                    FunctionParameter("type_parameter_1", BOOL),
                    FunctionParameter("type_parameter_2", INT32),
                    FunctionParameter("value", ANY) // TODO: Decide if we need to further segment this
                ),
                isNullCall = false,
                isNullable = false
            )
        }

        // TODO
        private fun coalesce(): List<FunctionSignature.Scalar> = emptyList()

        private fun nullIf(): List<FunctionSignature.Scalar> = nullableTypes.map { t ->
            FunctionSignature.Scalar(
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

        private fun substring(): List<FunctionSignature.Scalar> = textTypes.map { t ->
            listOf(
                FunctionSignature.Scalar(
                    name = "substring",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                        FunctionParameter("start", INT64),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature.Scalar(
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

        private fun position(): List<FunctionSignature.Scalar> = textTypes.map { t ->
            FunctionSignature.Scalar(
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

        private fun trim(): List<FunctionSignature.Scalar> = textTypes.map { t ->
            listOf(
                FunctionSignature.Scalar(
                    name = "trim",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature.Scalar(
                    name = "trim_chars",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                        FunctionParameter("chars", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature.Scalar(
                    name = "trim_leading",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature.Scalar(
                    name = "trim_leading_chars",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                        FunctionParameter("chars", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature.Scalar(
                    name = "trim_trailing",
                    returns = t,
                    parameters = listOf(
                        FunctionParameter("value", t),
                    ),
                    isNullCall = true,
                    isNullable = false,
                ),
                FunctionSignature.Scalar(
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
        private fun overlay(): List<FunctionSignature.Scalar> = emptyList()

        // TODO
        private fun extract(): List<FunctionSignature.Scalar> = emptyList()

        private fun dateArithmetic(prefix: String): List<FunctionSignature.Scalar> {
            val operators = mutableListOf<FunctionSignature.Scalar>()
            for (type in datetimeTypes) {
                for (field in DatetimeField.values()) {
                    if (field == DatetimeField.TIMEZONE_HOUR || field == DatetimeField.TIMEZONE_MINUTE) {
                        continue
                    }
                    val signature = FunctionSignature.Scalar(
                        name = "${prefix}_${field.name.lowercase()}",
                        returns = type,
                        parameters = listOf(
                            FunctionParameter("interval", INT),
                            FunctionParameter("datetime", type),
                        ),
                        isNullCall = true,
                        isNullable = false,
                    )
                    operators.add(signature)
                }
            }
            return operators
        }

        private fun dateAdd(): List<FunctionSignature.Scalar> = dateArithmetic("date_add")

        private fun dateDiff(): List<FunctionSignature.Scalar> = dateArithmetic("date_diff")

        private fun utcNow(): List<FunctionSignature.Scalar> = listOf(
            FunctionSignature.Scalar(
                name = "utcnow",
                returns = TIMESTAMP,
                parameters = emptyList(),
                isNullable = false,
            )
        )

        private fun currentUser() = FunctionSignature.Scalar(
            name = "\$__current_user",
            returns = STRING,
            parameters = emptyList(),
            isNullable = true,
        )

        private fun currentDate() = FunctionSignature.Scalar(
            name = "\$__current_date",
            returns = DATE,
            parameters = emptyList(),
            isNullable = false,
        )

        // ====================================
        //  AGGREGATIONS
        // ====================================

        /**
         * SQL and PartiQL Aggregation Builtins
         */
        public fun aggregations(): List<FunctionSignature.Aggregation> = listOf(
            every(),
            any(),
            some(),
            count(),
            min(),
            max(),
            sum(),
            avg(),
        ).flatten()

        private fun every() = listOf(
            FunctionSignature.Aggregation(
                name = "every",
                returns = BOOL,
                parameters = listOf(FunctionParameter("value", BOOL)),
                isNullable = true,
            )
        )

        private fun any() = listOf(
            FunctionSignature.Aggregation(
                name = "any",
                returns = BOOL,
                parameters = listOf(FunctionParameter("value", BOOL)),
                isNullable = true,
            )
        )

        private fun some() = listOf(
            FunctionSignature.Aggregation(
                name = "some",
                returns = BOOL,
                parameters = listOf(FunctionParameter("value", BOOL)),
                isNullable = true,
            )
        )

        private fun count() = listOf(
            FunctionSignature.Aggregation(
                name = "count",
                returns = INT,
                parameters = listOf(FunctionParameter("value", ANY)),
                isNullable = false,
            )
        )

        private fun min() = numericTypes.map {
            FunctionSignature.Aggregation(
                name = "min",
                returns = it,
                parameters = listOf(FunctionParameter("value", it)),
                isNullable = true,
            )
        }

        private fun max() = numericTypes.map {
            FunctionSignature.Aggregation(
                name = "max",
                returns = it,
                parameters = listOf(FunctionParameter("value", it)),
                isNullable = true,
            )
        }

        private fun sum() = numericTypes.map {
            FunctionSignature.Aggregation(
                name = "sum",
                returns = it,
                parameters = listOf(FunctionParameter("value", it)),
                isNullable = true,
            )
        }

        private fun avg() = numericTypes.map {
            FunctionSignature.Aggregation(
                name = "avg",
                returns = it,
                parameters = listOf(FunctionParameter("value", it)),
                isNullable = true,
            )
        }

        // ====================================
        //  SORTING
        // ====================================

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
            LIST,
            SEXP,
            BAG,
            STRUCT,
            ANY,
        ).mapIndexed { precedence, type -> type to precedence }.toMap()

        // ====================================
        //  HELPERS
        // ====================================

        public fun unary(name: String, returns: PartiQLValueType, value: PartiQLValueType) =
            FunctionSignature.Scalar(
                name = name,
                returns = returns,
                parameters = listOf(FunctionParameter("value", value)),
                isNullCall = true,
                isNullable = false,
            )

        public fun binary(name: String, returns: PartiQLValueType, lhs: PartiQLValueType, rhs: PartiQLValueType) =
            FunctionSignature.Scalar(
                name = name,
                returns = returns,
                parameters = listOf(FunctionParameter("lhs", lhs), FunctionParameter("rhs", rhs)),
                isNullCall = true,
                isNullable = false,
            )

        public fun cast(operand: PartiQLValueType, target: PartiQLValueType) =
            FunctionSignature.Scalar(
                name = castName(target),
                returns = target,
                isNullCall = true,
                isNullable = false,
                parameters = listOf(
                    FunctionParameter("value", operand),
                )
            )
    }
}
