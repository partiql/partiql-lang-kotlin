package org.partiql.spi.connector.sql

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BAG
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.CHAR
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.LIST
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.PartiQLValueType.NULL
import org.partiql.value.PartiQLValueType.SEXP
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

/**
 * This is a temporary internal object for generating all SQL-99 function signatures.
 */
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object SqlHeader {

    private val all = PartiQLValueType.values()

    private val nullable = listOf(
        NULL, // null.null
        MISSING, // missing
    )

    private val integer = listOf(
        INT8,
        INT16,
        INT32,
        INT64,
        INT,
    )

    private val numeric = listOf(
        INT8,
        INT16,
        INT32,
        INT64,
        INT,
        DECIMAL_ARBITRARY,
        FLOAT32,
        FLOAT64,
    )

    private val text = listOf(
        STRING,
        SYMBOL,
        CLOB,
    )

    private val collections = listOf(
        BAG,
        LIST,
        SEXP,
    )

    private val datetime = listOf(
        DATE,
        TIME,
        TIMESTAMP,
    )

    private enum class DatetimeField {
        YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, TIMEZONE_HOUR, TIMEZONE_MINUTE,
    }
    /**
     * PartiQL Scalar Functions
     */
    val functions: List<FnSignature.Scalar> = listOf(
        builtins(),
        logical(),
        predicate(),
        operators(),
        special(),
        system(),
    ).flatten()

    /**
     * PartiQL Aggregation Functions accessible via
     */
    val aggregations: List<FnSignature.Aggregation> = aggBuiltins()

    /**
     * Generate all unary and binary operator signatures.
     */
    private fun operators(): List<FnSignature.Scalar> = listOf(
        pos(),
        neg(),
        plus(),
        minus(),
        times(),
        div(),
        mod(),
        concat(),
        bitwiseAnd(),
    ).flatten()

    /**
     * Predicate function -- Condition that can be evaluated to a boolean value.
     *
     * Predicate function IS NULL, IS MISSING, `=`(Equal) does not propagate `MISSING`.
     */
    private fun predicate(): List<FnSignature.Scalar> = listOf(
        // SQL
        // 8.2 - comparison predicate
        lt(),
        lte(),
        gt(),
        gte(),
        eq(),

        // 8.3 - between predicate
        between(),
        // 8.4 - in predicate
        inCollection(),
        // 8.5 - like predicate
        like(),
        // 8.7 - null predicate
        isNull(),

        // PartiQL
        isMissing(), // missing predication
        isType(), // type predicate
        isTypeSingleArg(),
        isTypeDoubleArgsInt(),
        isTypeTime(),
    ).flatten()

    /**
     * Logical functions follows the three-valued logic truth table:
     *
     * |A   |B   |A AND B|A OR B |NOT A |
     * |----|----|-------|-------|------|
     * |T   |T   |T      |T      |F     |
     * |T   |F   |F      |T      |F     |
     * |T   |U   |U      |T      |F     |
     * |F   |T   |F      |T      |T     |
     * |F   |F   |F      |F      |T     |
     * |F   |U   |F      |U      |T     |
     * |U   |T   |U      |T      |U     |
     * |U   |F   |F      |U      |U     |
     * |U   |U   |U      |U      |U     |
     *
     * 1.  The `MISSING` value, when convert to a truth value, becomes a `UNKNOWN`.
     * 2. `UNKNOWN` truth value, when converting to PartiQL Value, becomes NULL of boolean type.
     */
    private fun logical(): List<FnSignature.Scalar> = listOf(
        not(),
        and(),
        or(),
    ).flatten()

    /**
     * SQL Builtins (not special forms)
     */
    private fun builtins(): List<FnSignature.Scalar> = listOf(
        upper(),
        lower(),
        position(),
        substring(),
        trim(),
        utcNow(),
    ).flatten()

    /**
     * SQL and PartiQL special forms
     */
    private fun special(): List<FnSignature.Scalar> = listOf(
        position(),
        substring(),
        trimSpecial(),
        overlay(),
        extract(),
        dateAdd(),
        dateDiff(),
    ).flatten()

    /**
     * System functions (for now, CURRENT_USER and CURRENT_DATE)
     *
     * @return
     */
    private fun system(): List<FnSignature.Scalar> = listOf(
        currentUser(),
        currentDate(),
    )

    // OPERATORS

    private fun not(): List<FnSignature.Scalar> = listOf(
        FnSignature.Scalar(
            name = "not",
            returns = BOOL,
            isNullCall = true,
            isNullable = false,
            parameters = listOf(FnParameter("value", BOOL)),
        ),
        FnSignature.Scalar(
            name = "not",
            returns = BOOL,
            isNullCall = true,
            isNullable = false,
            parameters = listOf(FnParameter("value", MISSING)),
        ),
    )

    private fun pos(): List<FnSignature.Scalar> = numeric.map { t ->
        unary("pos", t, t)
    }

    private fun neg(): List<FnSignature.Scalar> = numeric.map { t ->
        unary("neg", t, t)
    }

    private fun eq(): List<FnSignature.Scalar> = all.map { t ->
        FnSignature.Scalar(
            name = "eq",
            returns = BOOL,
            parameters = listOf(FnParameter("lhs", t), FnParameter("rhs", t)),
            isNullable = false,
            isNullCall = true,
        )
    }

    private fun and(): List<FnSignature.Scalar> = listOf(
        FnSignature.Scalar(
            name = "and",
            returns = BOOL,
            isNullCall = false,
            isNullable = true,
            parameters = listOf(FnParameter("lhs", BOOL), FnParameter("rhs", BOOL)),
        ),
        FnSignature.Scalar(
            name = "and",
            returns = BOOL,
            isNullCall = false,
            isNullable = true,
            parameters = listOf(FnParameter("lhs", MISSING), FnParameter("rhs", BOOL)),
        ),
        FnSignature.Scalar(
            name = "and",
            returns = BOOL,
            isNullCall = false,
            isNullable = true,
            parameters = listOf(FnParameter("lhs", BOOL), FnParameter("rhs", MISSING)),
        ),
        FnSignature.Scalar(
            name = "and",
            returns = BOOL,
            isNullCall = false,
            isNullable = true,
            parameters = listOf(FnParameter("lhs", MISSING), FnParameter("rhs", MISSING)),
        ),
    )

    private fun or(): List<FnSignature.Scalar> = listOf(
        FnSignature.Scalar(
            name = "or",
            returns = BOOL,
            isNullCall = false,
            isNullable = true,
            parameters = listOf(FnParameter("lhs", BOOL), FnParameter("rhs", BOOL)),
        ),
        FnSignature.Scalar(
            name = "or",
            returns = BOOL,
            isNullCall = false,
            isNullable = true,
            parameters = listOf(FnParameter("lhs", MISSING), FnParameter("rhs", BOOL)),
        ),
        FnSignature.Scalar(
            name = "or",
            returns = BOOL,
            isNullCall = false,
            isNullable = true,
            parameters = listOf(FnParameter("lhs", BOOL), FnParameter("rhs", MISSING)),
        ),
        FnSignature.Scalar(
            name = "or",
            returns = BOOL,
            isNullCall = false,
            isNullable = true,
            parameters = listOf(FnParameter("lhs", MISSING), FnParameter("rhs", MISSING)),
        ),
    )

    private fun lt(): List<FnSignature.Scalar> = (numeric + text + datetime + BOOL).map { t ->
        binary("lt", BOOL, t, t)
    }

    private fun lte(): List<FnSignature.Scalar> = (numeric + text + datetime + BOOL).map { t ->
        binary("lte", BOOL, t, t)
    }

    private fun gt(): List<FnSignature.Scalar> = (numeric + text + datetime + BOOL).map { t ->
        binary("gt", BOOL, t, t)
    }

    private fun gte(): List<FnSignature.Scalar> = (numeric + text + datetime + BOOL).map { t ->
        binary("gte", BOOL, t, t)
    }

    private fun plus(): List<FnSignature.Scalar> = numeric.map { t ->
        binary("plus", t, t, t)
    }

    private fun minus(): List<FnSignature.Scalar> = numeric.map { t ->
        binary("minus", t, t, t)
    }

    private fun times(): List<FnSignature.Scalar> = numeric.map { t ->
        binary("times", t, t, t)
    }

    private fun div(): List<FnSignature.Scalar> = numeric.map { t ->
        binary("divide", t, t, t)
    }

    private fun mod(): List<FnSignature.Scalar> = numeric.map { t ->
        binary("modulo", t, t, t)
    }

    private fun concat(): List<FnSignature.Scalar> = text.map { t ->
        binary("concat", t, t, t)
    }

    private fun bitwiseAnd(): List<FnSignature.Scalar> = integer.map { t ->
        binary("bitwise_and", t, t, t)
    }

    // BUILT INS
    private fun upper(): List<FnSignature.Scalar> = text.map { t ->
        FnSignature.Scalar(
            name = "upper",
            returns = t,
            parameters = listOf(FnParameter("value", t)),
            isNullable = false,
            isNullCall = true,
        )
    }

    private fun lower(): List<FnSignature.Scalar> = text.map { t ->
        FnSignature.Scalar(
            name = "lower",
            returns = t,
            parameters = listOf(FnParameter("value", t)),
            isNullable = false,
            isNullCall = true,
        )
    }

    // SPECIAL FORMS

    private fun like(): List<FnSignature.Scalar> = text.flatMap { t ->
        listOf(
            FnSignature.Scalar(
                name = "like",
                returns = BOOL,
                parameters = listOf(
                    FnParameter("value", t),
                    FnParameter("pattern", t),
                ),
                isNullCall = true,
                isNullable = false,
            ),
            FnSignature.Scalar(
                name = "like_escape",
                returns = BOOL,
                parameters = listOf(
                    FnParameter("value", t),
                    FnParameter("pattern", t),
                    FnParameter("escape", t),
                ),
                isNullCall = true,
                isNullable = false,
            ),
        )
    }

    private fun between(): List<FnSignature.Scalar> = (numeric + text + datetime).map { t ->
        FnSignature.Scalar(
            name = "between",
            returns = BOOL,
            parameters = listOf(
                FnParameter("value", t),
                FnParameter("lower", t),
                FnParameter("upper", t),
            ),
            isNullable = false,
            isNullCall = true,
        )
    }

    private fun inCollection(): List<FnSignature.Scalar> = all.map { element ->
        collections.map { collection ->
            FnSignature.Scalar(
                name = "in_collection",
                returns = BOOL,
                parameters = listOf(
                    FnParameter("value", element),
                    FnParameter("collection", collection),
                ),
                isNullable = false,
                isNullCall = true,
            )
        }
    }.flatten()

    private fun isNull(): List<FnSignature.Scalar> = listOf(
        FnSignature.Scalar(
            name = "is_null", returns = BOOL, parameters = listOf(
                FnParameter("value", ANY) // TODO: Decide if we need to further segment this
            ), isNullCall = false, isNullable = false
        )
    )

    private fun isMissing(): List<FnSignature.Scalar> = listOf(
        FnSignature.Scalar(
            name = "is_missing", returns = BOOL, parameters = listOf(
                FnParameter("value", ANY) // TODO: Decide if we need to further segment this
            ), isNullCall = false, isNullable = false
        )
    )

    // To model type assertion, generating a list of assertion function based on the type,
    // and the parameter will be the value entered.
    //  i.e., 1 is INT2  => is_int16(1)
    private fun isType(): List<FnSignature.Scalar> = all.filterNot { it == NULL || it == MISSING }.map { element ->
        FnSignature.Scalar(
            name = "is_${element.name.lowercase()}",
            returns = BOOL,
            parameters = listOf(
                FnParameter("value", ANY)
            ),
            isNullable = false,
            isNullCall = false,
        )
    }

    // In type assertion, it is possible for types to have args
    // i.e., 'a' is CHAR(2)
    // we put type parameter before value.
    private fun isTypeSingleArg(): List<FnSignature.Scalar> = listOf(CHAR, STRING).map { element ->
        FnSignature.Scalar(
            name = "is_${element.name.lowercase()}", returns = BOOL, parameters = listOf(
                FnParameter("type_parameter_1", INT32), FnParameter("value", ANY)
            ), isNullable = false, isNullCall = false
        )
    }

    private fun isTypeDoubleArgsInt(): List<FnSignature.Scalar> = listOf(DECIMAL).map { element ->
        FnSignature.Scalar(
            name = "is_${element.name.lowercase()}", returns = BOOL, parameters = listOf(
                FnParameter("type_parameter_1", INT32),
                FnParameter("type_parameter_2", INT32),
                FnParameter("value", ANY)
            ), isNullable = false, isNullCall = false
        )
    }

    private fun isTypeTime(): List<FnSignature.Scalar> = listOf(TIME, TIMESTAMP).map { element ->
        FnSignature.Scalar(
            name = "is_${element.name.lowercase()}", returns = BOOL, parameters = listOf(
                FnParameter("type_parameter_1", BOOL),
                FnParameter("type_parameter_2", INT32),
                FnParameter("value", ANY) // TODO: Decide if we need to further segment this
            ), isNullable = false, isNullCall = false
        )
    }

    // SUBSTRING (expression, start[, length]?)
    // SUBSTRINGG(expression from start [FOR length]? )
    private fun substring(): List<FnSignature.Scalar> = text.map { t ->
        listOf(
            FnSignature.Scalar(
                name = "substring",
                returns = t,
                parameters = listOf(
                    FnParameter("value", t),
                    FnParameter("start", INT64),
                ),
                isNullable = false,
                isNullCall = true,
            ), FnSignature.Scalar(
                name = "substring",
                returns = t,
                parameters = listOf(
                    FnParameter("value", t),
                    FnParameter("start", INT64),
                    FnParameter("end", INT64),
                ),
                isNullable = false,
                isNullCall = true,
            )
        )
    }.flatten()

    // position (str1, str2)
    // position (str1 in str2)
    private fun position(): List<FnSignature.Scalar> = text.map { t ->
        FnSignature.Scalar(
            name = "position",
            returns = INT64,
            parameters = listOf(
                FnParameter("probe", t),
                FnParameter("value", t),
            ),
            isNullable = false,
            isNullCall = true,
        )
    }

    // trim(str)
    private fun trim(): List<FnSignature.Scalar> = text.map { t ->
        FnSignature.Scalar(
            name = "trim",
            returns = t,
            parameters = listOf(
                FnParameter("value", t),
            ),
            isNullable = false,
            isNullCall = true,
        )
    }

    // TODO: We need to add a special form function for TRIM(BOTH FROM value)
    private fun trimSpecial(): List<FnSignature.Scalar> = text.map { t ->
        listOf(
            // TRIM(chars FROM value)
            // TRIM(both chars from value)
            FnSignature.Scalar(
                name = "trim_chars",
                returns = t,
                parameters = listOf(
                    FnParameter("value", t),
                    FnParameter("chars", t),
                ),
                isNullable = false,
                isNullCall = true,
            ),
            // TRIM(LEADING FROM value)
            FnSignature.Scalar(
                name = "trim_leading",
                returns = t,
                parameters = listOf(
                    FnParameter("value", t),
                ),
                isNullable = false,
                isNullCall = true,
            ),
            // TRIM(LEADING chars FROM value)
            FnSignature.Scalar(
                name = "trim_leading_chars",
                returns = t,
                parameters = listOf(
                    FnParameter("value", t),
                    FnParameter("chars", t),
                ),
                isNullable = false,
                isNullCall = true,
            ),
            // TRIM(TRAILING FROM value)
            FnSignature.Scalar(
                name = "trim_trailing",
                returns = t,
                parameters = listOf(
                    FnParameter("value", t),
                ),
                isNullable = false,
                isNullCall = true,
            ),
            // TRIM(TRAILING chars FROM value)
            FnSignature.Scalar(
                name = "trim_trailing_chars",
                returns = t,
                parameters = listOf(
                    FnParameter("value", t),
                    FnParameter("chars", t),
                ),
                isNullable = false,
                isNullCall = true,
            ),
        )
    }.flatten()

    // TODO
    private fun overlay(): List<FnSignature.Scalar> = emptyList()

    // TODO
    private fun extract(): List<FnSignature.Scalar> = emptyList()

    private fun dateAdd(): List<FnSignature.Scalar> {
        val intervals = listOf(INT32, INT64, INT)
        val operators = mutableListOf<FnSignature.Scalar>()
        for (field in DatetimeField.values()) {
            for (type in datetime) {
                if (field == DatetimeField.TIMEZONE_HOUR || field == DatetimeField.TIMEZONE_MINUTE) {
                    continue
                }
                for (interval in intervals) {
                    val signature = FnSignature.Scalar(
                        name = "date_add_${field.name.lowercase()}",
                        returns = type,
                        parameters = listOf(
                            FnParameter("interval", interval),
                            FnParameter("datetime", type),
                        ),
                        isNullable = false,
                        isNullCall = true,
                    )
                    operators.add(signature)
                }
            }
        }
        return operators
    }

    private fun dateDiff(): List<FnSignature.Scalar> {
        val operators = mutableListOf<FnSignature.Scalar>()
        for (field in DatetimeField.values()) {
            for (type in datetime) {
                if (field == DatetimeField.TIMEZONE_HOUR || field == DatetimeField.TIMEZONE_MINUTE) {
                    continue
                }
                val signature = FnSignature.Scalar(
                    name = "date_diff_${field.name.lowercase()}",
                    returns = INT64,
                    parameters = listOf(
                        FnParameter("datetime1", type),
                        FnParameter("datetime2", type),
                    ),
                    isNullable = false,
                    isNullCall = true,
                )
                operators.add(signature)
            }
        }
        return operators
    }

    private fun utcNow(): List<FnSignature.Scalar> = listOf(
        FnSignature.Scalar(
            name = "utcnow",
            returns = TIMESTAMP,
            parameters = emptyList(),
            isNullable = false,
        )
    )

    private fun currentUser() = FnSignature.Scalar(
        name = "current_user",
        returns = STRING,
        parameters = emptyList(),
        isNullable = true,
    )

    private fun currentDate() = FnSignature.Scalar(
        name = "current_date",
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
    private fun aggBuiltins(): List<FnSignature.Aggregation> = listOf(
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
        FnSignature.Aggregation(
            name = "every",
            returns = BOOL,
            parameters = listOf(FnParameter("value", BOOL)),
            isNullable = true,
        ),
    )

    private fun any() = listOf(
        FnSignature.Aggregation(
            name = "any",
            returns = BOOL,
            parameters = listOf(FnParameter("value", BOOL)),
            isNullable = true,
        ),
    )

    private fun some() = listOf(
        FnSignature.Aggregation(
            name = "some",
            returns = BOOL,
            parameters = listOf(FnParameter("value", BOOL)),
            isNullable = true,
        ),
    )

    private fun count() = listOf(
        FnSignature.Aggregation(
            name = "count",
            returns = INT32,
            parameters = listOf(FnParameter("value", ANY)),
            isNullable = false,
        ),
        FnSignature.Aggregation(
            name = "count_star",
            returns = INT32,
            parameters = listOf(),
            isNullable = false,
        ),
    )

    private fun min() = numeric.map {
        FnSignature.Aggregation(
            name = "min",
            returns = it,
            parameters = listOf(FnParameter("value", it)),
            isNullable = true,
        )
    }

    private fun max() = numeric.map {
        FnSignature.Aggregation(
            name = "max",
            returns = it,
            parameters = listOf(FnParameter("value", it)),
            isNullable = true,
        )
    }

    private fun sum() = numeric.map {
        FnSignature.Aggregation(
            name = "sum",
            returns = it,
            parameters = listOf(FnParameter("value", it)),
            isNullable = true,
        )
    }

    private fun avg() = numeric.map {
        FnSignature.Aggregation(
            name = "avg",
            returns = it,
            parameters = listOf(FnParameter("value", it)),
            isNullable = true,
        )
    }

    // ====================================
    //  HELPERS
    // ====================================

    @JvmStatic
    internal fun unary(name: String, returns: PartiQLValueType, value: PartiQLValueType) = FnSignature.Scalar(
        name = name,
        returns = returns,
        parameters = listOf(FnParameter("value", value)),
        isNullable = false,
        isNullCall = true
    )

    @JvmStatic
    internal fun binary(name: String, returns: PartiQLValueType, lhs: PartiQLValueType, rhs: PartiQLValueType) =
        FnSignature.Scalar(
            name = name,
            returns = returns,
            parameters = listOf(FnParameter("lhs", lhs), FnParameter("rhs", rhs)),
            isNullable = false,
            isNullCall = true
        )

    /**
     * Dump the Header as SQL commands
     *
     * For functions, output CREATE FUNCTION statements.
     */
    override fun toString(): String = buildString {
        (functions + aggregations).groupBy { it.name }.forEach {
            appendLine("-- [${it.key}] ---------")
            appendLine()
            it.value.forEach { fn -> appendLine(fn) }
            appendLine()
        }
    }
}
