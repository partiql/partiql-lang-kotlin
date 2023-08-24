package org.partiql.transpiler.sql

import org.partiql.ast.Ast
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
public sealed class SqlArg(
    public val expr: Expr,
    public val type: StaticType,
) {

    /**
     * Value argument
     */
    public class V(expr: Expr, type: StaticType) : SqlArg(expr, type)

    /**
     * Type argument
     */
    public class T(expr: Expr, type: StaticType) : SqlArg(expr, type)
}

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
     * List of special form rules. See [org.partiql.planner.Header] for the derivations,.
     */
    private val rules: Map<String, SqlCallFn> = mapOf(
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
}
