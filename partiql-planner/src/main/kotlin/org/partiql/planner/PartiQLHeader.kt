package org.partiql.planner

import org.partiql.ast.DatetimeField
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.CHAR
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

/**
 * A header which uses the PartiQL Lang Kotlin default standard library. All functions exist in a global namespace.
 * Once we have catalogs with information_schema, the PartiQL Header will be fixed on a specification version and
 * user defined functions will be defined within their own schema.
 *
 */
@OptIn(PartiQLValueExperimental::class)
object PartiQLHeader : Header() {

    override val namespace: String = "partiql"

    /**
     * PartiQL Scalar Functions accessible via call syntax.
     */
    override val functions = scalarBuiltins()

    /**
     * PartiQL Scalar Functions accessible via special form syntax (unary, binary, infix keywords, etc).
     */
    override val operators = listOf(
        operators(),
        special(),
        system(),
    ).flatten()

    /**
     * PartiQL Aggregation Functions accessible via
     */
    override val aggregations = aggBuiltins()

    /**
     * Generate all unary and binary operator signatures.
     */
    private fun operators(): List<FunctionSignature.Scalar> = listOf(
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
     * SQL Builtins (not special forms)
     */
    private fun scalarBuiltins(): List<FunctionSignature.Scalar> = listOf(
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
    private fun special(): List<FunctionSignature.Scalar> = listOf(
        like(),
        between(),
        inCollection(),
        isType(),
        isTypeSingleArg(),
        isTypeDoubleArgsInt(),
        isTypeTime(),
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
    private fun system(): List<FunctionSignature.Scalar> = listOf(
        currentUser(),
        currentDate(),
    )

    // OPERATORS

    private fun not(): List<FunctionSignature.Scalar> = listOf(unary("not", BOOL, BOOL))

    private fun pos(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        unary("pos", t, t)
    }

    private fun neg(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        unary("neg", t, t)
    }

    private fun eq(): List<FunctionSignature.Scalar> = types.all.map { t ->
        FunctionSignature.Scalar(
            name = "eq",
            returns = BOOL,
            parameters = listOf(FunctionParameter("lhs", t), FunctionParameter("rhs", t)),
            isNullable = false,
            isNullCall = false,
        )
    }

    private fun ne(): List<FunctionSignature.Scalar> = types.all.map { t ->
        binary("ne", BOOL, t, t)
    }

    private fun and(): List<FunctionSignature.Scalar> = listOf(
        binary("and", BOOL, BOOL, BOOL),
    )

    private fun or(): List<FunctionSignature.Scalar> = listOf(
        binary("or", BOOL, BOOL, BOOL),
    )

    private fun lt(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        binary("lt", BOOL, t, t)
    }

    private fun lte(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        binary("lte", BOOL, t, t)
    }

    private fun gt(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        binary("gt", BOOL, t, t)
    }

    private fun gte(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        binary("gte", BOOL, t, t)
    }

    private fun plus(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        binary("plus", t, t, t)
    }

    private fun minus(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        binary("minus", t, t, t)
    }

    private fun times(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        binary("times", t, t, t)
    }

    private fun div(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        binary("divide", t, t, t)
    }

    private fun mod(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        binary("modulo", t, t, t)
    }

    private fun concat(): List<FunctionSignature.Scalar> = types.text.map { t ->
        binary("concat", t, t, t)
    }

    private fun bitwiseAnd(): List<FunctionSignature.Scalar> = types.integer.map { t ->
        binary("bitwise_and", t, t, t)
    }

    // BUILT INS
    private fun upper(): List<FunctionSignature.Scalar> = types.text.map { t ->
        FunctionSignature.Scalar(
            name = "upper",
            returns = t,
            parameters = listOf(FunctionParameter("value", t)),
            isNullable = false,
            isNullCall = true,
        )
    }

    private fun lower(): List<FunctionSignature.Scalar> = types.text.map { t ->
        FunctionSignature.Scalar(
            name = "lower",
            returns = t,
            parameters = listOf(FunctionParameter("value", t)),
            isNullable = false,
            isNullCall = true,
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
            isNullable = false,
            isNullCall = true,
        ),
        FunctionSignature.Scalar(
            name = "like_escape",
            returns = BOOL,
            parameters = listOf(
                FunctionParameter("value", STRING),
                FunctionParameter("pattern", STRING),
                FunctionParameter("escape", STRING),
            ),
            isNullable = false,
            isNullCall = true,
        ),
    )

    private fun between(): List<FunctionSignature.Scalar> = types.numeric.map { t ->
        FunctionSignature.Scalar(
            name = "between",
            returns = BOOL,
            parameters = listOf(
                FunctionParameter("value", t),
                FunctionParameter("lower", t),
                FunctionParameter("upper", t),
            ),
            isNullable = false,
            isNullCall = true,
        )
    }

    private fun inCollection(): List<FunctionSignature.Scalar> = types.all.map { element ->
        types.collections.map { collection ->
            FunctionSignature.Scalar(
                name = "in_collection",
                returns = BOOL,
                parameters = listOf(
                    FunctionParameter("value", element),
                    FunctionParameter("collection", collection),
                ),
                isNullable = false,
                isNullCall = true,
            )
        }
    }.flatten()

    // To model type assertion, generating a list of assertion function based on the type,
    // and the parameter will be the value entered.
    //  i.e., 1 is INT2  => is_int16(1)
    // TODO: We can remove the types with parameter in this function.
    //  but, leaving out the decision to have, for example:
    //  is_decimal(null, null, value) vs is_decimal(value) later....
    private fun isType(): List<FunctionSignature.Scalar> = types.all.map { element ->
        FunctionSignature.Scalar(
            name = "is_${element.name.lowercase()}",
            returns = BOOL,
            parameters = listOf(
                FunctionParameter("value", ANY) // TODO: Decide if we need to further segment this
            ),
            isNullable = false,
            isNullCall = false
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
            isNullable = false,
            isNullCall = false
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
            isNullable = false,
            isNullCall = false
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
            isNullable = false,
            isNullCall = false
        )
    }

    // SUBSTRING (expression, start[, length]?)
    // SUBSTRINGG(expression from start [FOR length]? )
    private fun substring(): List<FunctionSignature.Scalar> = types.text.map { t ->
        listOf(
            FunctionSignature.Scalar(
                name = "substring",
                returns = t,
                parameters = listOf(
                    FunctionParameter("value", t),
                    FunctionParameter("start", INT64),
                ),
                isNullable = false,
                isNullCall = true,
            ),
            FunctionSignature.Scalar(
                name = "substring",
                returns = t,
                parameters = listOf(
                    FunctionParameter("value", t),
                    FunctionParameter("start", INT64),
                    FunctionParameter("end", INT64),
                ),
                isNullable = false,
                isNullCall = true,
            )
        )
    }.flatten()

    // position (str1, str2)
    // position (str1 in str2)
    private fun position(): List<FunctionSignature.Scalar> = types.text.map { t ->
        FunctionSignature.Scalar(
            name = "position",
            returns = INT64,
            parameters = listOf(
                FunctionParameter("probe", t),
                FunctionParameter("value", t),
            ),
            isNullable = false,
            isNullCall = true,
        )
    }

    // trim(str)
    private fun trim(): List<FunctionSignature.Scalar> = types.text.map { t ->
        FunctionSignature.Scalar(
            name = "trim",
            returns = t,
            parameters = listOf(
                FunctionParameter("value", t),
            ),
            isNullable = false,
            isNullCall = true,
        )
    }

    // TODO: We need to add a special form function for TRIM(BOTH FROM value)
    private fun trimSpecial(): List<FunctionSignature.Scalar> = types.text.map { t ->
        listOf(
            // TRIM(chars FROM value)
            // TRIM(both chars from value)
            FunctionSignature.Scalar(
                name = "trim_chars",
                returns = t,
                parameters = listOf(
                    FunctionParameter("value", t),
                    FunctionParameter("chars", t),
                ),
                isNullable = false,
                isNullCall = true,
            ),
            // TRIM(LEADING FROM value)
            FunctionSignature.Scalar(
                name = "trim_leading",
                returns = t,
                parameters = listOf(
                    FunctionParameter("value", t),
                ),
                isNullable = false,
                isNullCall = true,
            ),
            // TRIM(LEADING chars FROM value)
            FunctionSignature.Scalar(
                name = "trim_leading_chars",
                returns = t,
                parameters = listOf(
                    FunctionParameter("value", t),
                    FunctionParameter("chars", t),
                ),
                isNullable = false,
                isNullCall = true,
            ),
            // TRIM(TRAILING FROM value)
            FunctionSignature.Scalar(
                name = "trim_trailing",
                returns = t,
                parameters = listOf(
                    FunctionParameter("value", t),
                ),
                isNullable = false,
                isNullCall = true,
            ),
            // TRIM(TRAILING chars FROM value)
            FunctionSignature.Scalar(
                name = "trim_trailing_chars",
                returns = t,
                parameters = listOf(
                    FunctionParameter("value", t),
                    FunctionParameter("chars", t),
                ),
                isNullable = false,
                isNullCall = true,
            ),
        )
    }.flatten()

    // TODO
    private fun overlay(): List<FunctionSignature.Scalar> = emptyList()

    // TODO
    private fun extract(): List<FunctionSignature.Scalar> = emptyList()

    private fun dateAdd(): List<FunctionSignature.Scalar> {
        val operators = mutableListOf<FunctionSignature.Scalar>()
        for (field in DatetimeField.values()) {
            for (type in types.datetime) {
                if (field == DatetimeField.TIMEZONE_HOUR || field == DatetimeField.TIMEZONE_MINUTE) {
                    continue
                }
                val signature = FunctionSignature.Scalar(
                    name = "date_add_${field.name.lowercase()}",
                    returns = type,
                    parameters = listOf(
                        FunctionParameter("interval", INT),
                        FunctionParameter("datetime", type),
                    ),
                    isNullable = false,
                    isNullCall = true,
                )
                operators.add(signature)
            }
        }
        return operators
    }

    private fun dateDiff(): List<FunctionSignature.Scalar> {
        val operators = mutableListOf<FunctionSignature.Scalar>()
        for (field in DatetimeField.values()) {
            for (type in types.datetime) {
                if (field == DatetimeField.TIMEZONE_HOUR || field == DatetimeField.TIMEZONE_MINUTE) {
                    continue
                }
                val signature = FunctionSignature.Scalar(
                    name = "date_diff_${field.name.lowercase()}",
                    returns = INT64,
                    parameters = listOf(
                        FunctionParameter("datetime1", type),
                        FunctionParameter("datetime2", type),
                    ),
                    isNullable = false,
                    isNullCall = true,
                )
                operators.add(signature)
            }
        }
        return operators
    }

    private fun utcNow(): List<FunctionSignature.Scalar> = listOf(
        FunctionSignature.Scalar(
            name = "utcnow",
            returns = TIMESTAMP,
            parameters = emptyList(),
            isNullable = false,
        )
    )

    private fun currentUser() = FunctionSignature.Scalar(
        name = "current_user",
        returns = STRING,
        parameters = emptyList(),
        isNullable = true,
    )

    private fun currentDate() = FunctionSignature.Scalar(
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
    private fun aggBuiltins(): List<FunctionSignature.Aggregation> = listOf(
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
        ),
    )

    private fun any() = listOf(
        FunctionSignature.Aggregation(
            name = "any",
            returns = BOOL,
            parameters = listOf(FunctionParameter("value", BOOL)),
            isNullable = true,
        ),
    )

    private fun some() = listOf(
        FunctionSignature.Aggregation(
            name = "some",
            returns = BOOL,
            parameters = listOf(FunctionParameter("value", BOOL)),
            isNullable = true,
        ),
    )

    private fun count() = listOf(
        FunctionSignature.Aggregation(
            name = "count",
            returns = INT32,
            parameters = listOf(FunctionParameter("value", ANY)),
            isNullable = false,
        ),
        FunctionSignature.Aggregation(
            name = "count_star",
            returns = INT32,
            parameters = listOf(),
            isNullable = false,
        ),
    )

    private fun min() = types.numeric.map {
        FunctionSignature.Aggregation(
            name = "min",
            returns = it,
            parameters = listOf(FunctionParameter("value", it)),
            isNullable = true,
        )
    }

    private fun max() = types.numeric.map {
        FunctionSignature.Aggregation(
            name = "max",
            returns = it,
            parameters = listOf(FunctionParameter("value", it)),
            isNullable = true,
        )
    }

    private fun sum() = types.numeric.map {
        FunctionSignature.Aggregation(
            name = "sum",
            returns = it,
            parameters = listOf(FunctionParameter("value", it)),
            isNullable = true,
        )
    }

    private fun avg() = types.numeric.map {
        FunctionSignature.Aggregation(
            name = "avg",
            returns = it,
            parameters = listOf(FunctionParameter("value", it)),
            isNullable = true,
        )
    }
}
