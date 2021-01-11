package org.partiql.lang.eval.builtins.storedprocedure

import org.partiql.lang.eval.EvaluatingCompiler
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.Expression

/**
 * A typed version of a stored procedure signature. This signature includes the stored procedure's [name] and [arity].
 */
data class StoredProcedureSignature(val name: String, val arity: IntRange) {
    constructor(name: String, arity: Int) : this(name, (arity..arity))
}

/**
 * Represents a stored procedure that can be invoked.
 *
 * Stored procedures differ from functions (i.e. [ExprFunction]) in that:
 * 1. stored procedures are allowed to have side-effects
 * 2. stored procedures are only allowed at the top level of a query and cannot be used as an [Expression] (i.e. stored
 *    procedures can only be called using `EXEC sproc 'arg1', 'arg2'` and cannot be called from queries such as
 *    `SELECT * FROM (EXEC sproc 'arg1', 'arg2')`
 */
interface StoredProcedure {
    /**
     * [StoredProcedureSignature] representing the stored procedure's name and arity to be referenced in stored
     * procedure calls.
     */
    val signature: StoredProcedureSignature

    /**
     * Invokes the stored procedure. Proper arity is checked by the [EvaluatingCompiler], but argument type checking
     * is left to the implementation.
     *
     * @param session the calling environment session
     * @param args argument list supplied to the stored procedure
     */
    fun call(session: EvaluationSession, args: List<ExprValue>): ExprValue
}