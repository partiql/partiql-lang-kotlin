package org.partiql.transpiler.sql

import org.partiql.ast.Ast
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * Transform the call args to the special form.
 */
typealias SqlCallFn = (SqlArgs) -> Expr

/**
 * List of arguments.
 */
typealias SqlArgs = List<SqlArg>

/**
 * Pair an [Expr] with its resolved type.
 */
public class SqlArg(
    public val expr: Expr,
    public val type: StaticType,
)

/**
 * Maps a function name to basic rewrite logic.
 *
 * For target implementors, extend this and leverage the type-annotated function arguments to perform desired rewrite.
 */
@OptIn(PartiQLValueExperimental::class)
public abstract class SqlCalls {

    companion object {

        public val DEFAULT = object : SqlCalls() {}
    }

    /**
     * List of special form rules. See [org.partiql.planner.Header] for the derivations.
     */
    public val rules: Map<String, SqlCallFn> = mapOf(
        "not" to ::notFn,
        "pos" to ::posFn,
        "neg" to ::negFn,
        "eq" to ::eqFn,
        "ne" to ::neFn,
        "and" to ::andFn,
        "or" to ::orFn,
        "lt" to ::ltFn,
        "lte" to ::lteFn,
        "gt" to ::gtFn,
        "gte" to ::gteFn,
        "plus" to ::plusFn,
        "minus" to ::minusFn,
        "times" to ::timesFn,
        "div" to ::divFn,
        "mod" to ::modFn,
        "concat" to ::concatFn,
        // CASTS
        "cast_bool" to { args -> rewriteCast(PartiQLValueType.BOOL, args) },
        "cast_int8" to { args -> rewriteCast(PartiQLValueType.INT8, args) },
        "cast_int16" to { args -> rewriteCast(PartiQLValueType.INT16, args) },
        "cast_int32" to { args -> rewriteCast(PartiQLValueType.INT32, args) },
        "cast_int64" to { args -> rewriteCast(PartiQLValueType.INT64, args) },
        "cast_int" to { args -> rewriteCast(PartiQLValueType.INT, args) },
        "cast_decimal" to { args -> rewriteCast(PartiQLValueType.DECIMAL, args) },
        "cast_float32" to { args -> rewriteCast(PartiQLValueType.FLOAT32, args) },
        "cast_float64" to { args -> rewriteCast(PartiQLValueType.FLOAT64, args) },
        "cast_char" to { args -> rewriteCast(PartiQLValueType.CHAR, args) },
        "cast_string" to { args -> rewriteCast(PartiQLValueType.STRING, args) },
        "cast_symbol" to { args -> rewriteCast(PartiQLValueType.SYMBOL, args) },
        "cast_binary" to { args -> rewriteCast(PartiQLValueType.BINARY, args) },
        "cast_byte" to { args -> rewriteCast(PartiQLValueType.BYTE, args) },
        "cast_blob" to { args -> rewriteCast(PartiQLValueType.BLOB, args) },
        "cast_clob" to { args -> rewriteCast(PartiQLValueType.CLOB, args) },
        "cast_date" to { args -> rewriteCast(PartiQLValueType.DATE, args) },
        "cast_time" to { args -> rewriteCast(PartiQLValueType.TIME, args) },
        "cast_timestamp" to { args -> rewriteCast(PartiQLValueType.TIMESTAMP, args) },
        "cast_interval" to { args -> rewriteCast(PartiQLValueType.INTERVAL, args) },
        "cast_bag" to { args -> rewriteCast(PartiQLValueType.BAG, args) },
        "cast_list" to { args -> rewriteCast(PartiQLValueType.LIST, args) },
        "cast_sexp" to { args -> rewriteCast(PartiQLValueType.SEXP, args) },
        "cast_struct" to { args -> rewriteCast(PartiQLValueType.STRUCT, args) },
        "cast_null" to { args -> rewriteCast(PartiQLValueType.NULL, args) },
        "cast_missing" to { args -> rewriteCast(PartiQLValueType.MISSING, args) },
        // DATE_ADD
        "date_add_year" to { args -> dateAdd(DatetimeField.YEAR, args) },
        "date_add_month" to { args -> dateAdd(DatetimeField.MONTH, args) },
        "date_add_day" to { args -> dateAdd(DatetimeField.DAY, args) },
        "date_add_hour" to { args -> dateAdd(DatetimeField.HOUR, args) },
        "date_add_minute" to { args -> dateAdd(DatetimeField.MINUTE, args) },
        "date_add_second" to { args -> dateAdd(DatetimeField.SECOND, args) },
        // DATE_DIFF
        "date_diff_year" to { args -> dateDiff(DatetimeField.YEAR, args) },
        "date_diff_month" to { args -> dateDiff(DatetimeField.MONTH, args) },
        "date_diff_day" to { args -> dateDiff(DatetimeField.DAY, args) },
        "date_diff_hour" to { args -> dateDiff(DatetimeField.HOUR, args) },
        "date_diff_minute" to { args -> dateDiff(DatetimeField.MINUTE, args) },
        "date_diff_second" to { args -> dateDiff(DatetimeField.SECOND, args) },
    )

    public fun retarget(name: String, args: SqlArgs): Expr {
        val rule = rules[name]
        return if (rule == null) {
            // use default translations
            default(name, args)
        } else {
            // use special rule
            rule(args)
        }
    }

    private fun default(name: String, args: SqlArgs) = Ast.create {
        exprCall(
            function = identifierSymbol(name, Identifier.CaseSensitivity.INSENSITIVE),
            args = args.map { it.expr },
        )
    }

    public open fun unary(op: Expr.Unary.Op, args: SqlArgs): Expr {
        assert(args.size == 1) { "Unary operator $op requires exactly 1 argument" }
        return Ast.exprUnary(op, args[0].expr)
    }

    public open fun binary(op: Expr.Binary.Op, args: SqlArgs): Expr {
        assert(args.size == 2) { "Binary operator $op requires exactly 2 arguments" }
        return Ast.exprBinary(op, args[0].expr, args[1].expr)
    }

    public open fun notFn(args: SqlArgs): Expr = unary(Expr.Unary.Op.NOT, args)

    public open fun posFn(args: SqlArgs): Expr = unary(Expr.Unary.Op.POS, args)

    public open fun negFn(args: SqlArgs): Expr = unary(Expr.Unary.Op.NEG, args)

    public open fun eqFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.EQ, args)

    public open fun neFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.NE, args)

    public open fun andFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.AND, args)

    public open fun orFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.OR, args)

    public open fun ltFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.LT, args)

    public open fun lteFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.LTE, args)

    public open fun gtFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.GT, args)

    public open fun gteFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.GTE, args)

    public open fun plusFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.PLUS, args)

    public open fun minusFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.MINUS, args)

    public open fun timesFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.TIMES, args)

    public open fun divFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.DIVIDE, args)

    public open fun modFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.MODULO, args)

    public open fun concatFn(args: SqlArgs): Expr = binary(Expr.Binary.Op.CONCAT, args)

    public open fun dateAdd(part: DatetimeField, args: SqlArgs): Expr {
        TODO()
    }

    public open fun dateDiff(part: DatetimeField, args: SqlArgs): Expr {
        TODO()
    }

    public open fun rewriteCast(type: PartiQLValueType, args: SqlArgs): Expr = with(Ast) {
        assert(args.size == 1) { "CAST should only have 1 argument" }
        val value = args[0].expr
        val asType = when (type) {
            PartiQLValueType.ANY -> typeAny()
            PartiQLValueType.BOOL -> typeBool()
            PartiQLValueType.INT8 -> typeInt()
            PartiQLValueType.INT16 -> typeInt2()
            PartiQLValueType.INT32 -> typeInt4()
            PartiQLValueType.INT64 -> typeInt8()
            PartiQLValueType.INT -> typeInt()
            PartiQLValueType.DECIMAL -> typeDecimal(null, null)
            PartiQLValueType.FLOAT32 -> typeFloat32()
            PartiQLValueType.FLOAT64 -> typeFloat64()
            PartiQLValueType.CHAR -> typeChar(null)
            PartiQLValueType.STRING -> typeString(null)
            PartiQLValueType.SYMBOL -> typeSymbol()
            PartiQLValueType.BINARY -> error("Unsupported")
            PartiQLValueType.BYTE -> error("Unsupported")
            PartiQLValueType.BLOB -> typeBlob(null)
            PartiQLValueType.CLOB -> typeClob(null)
            PartiQLValueType.DATE -> typeDate()
            PartiQLValueType.TIME -> typeTime(null)
            PartiQLValueType.TIMESTAMP -> typeTimestamp(null)
            PartiQLValueType.INTERVAL -> typeInterval(null)
            PartiQLValueType.BAG -> typeBag()
            PartiQLValueType.LIST -> typeList()
            PartiQLValueType.SEXP -> typeSexp()
            PartiQLValueType.STRUCT -> typeStruct()
            PartiQLValueType.NULL -> typeNullType()
            PartiQLValueType.MISSING -> typeMissing()
        }
        exprCast(value, asType)
    }
}
