package org.partiql.transpiler.sql

import org.partiql.ast.Ast
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.types.StaticType

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
}
