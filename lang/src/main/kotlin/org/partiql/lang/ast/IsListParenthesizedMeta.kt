package org.partiql.lang.ast

/**
 * The [IsListParenthesizedMeta] is used to distinguish the two types of [org.partiql.lang.domains.PartiqlAst.Expr.List]
 * syntax's:
 *  1. [[ a, b, c ]]
 *  2. ( a, b, c )
 *
 * The existence of this meta on a [org.partiql.lang.domains.PartiqlAst.Expr.List] node indicates that the list uses
 * the second syntax above (using parenthesis).
 */
public class IsListParenthesizedMeta private constructor() : Meta {

    override val tag = TAG

    companion object {
        const val TAG = "\$is_list_parenthesized"
        val instance = IsListParenthesizedMeta()
    }
}
