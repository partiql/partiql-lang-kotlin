package org.partiql.lang.planner

import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue

/** A query plan that has been compiled and is ready to be evaluated. */
fun interface QueryPlan {
    /**
     * Evaluates the query plan with the given Session.
     */
    fun eval(session: EvaluationSession): QueryResult
}

sealed class QueryResult {
    /**
     * The result of an SFW query, arbitrary expression, or `EXEC` stored procedure call.
     */
    class Value(val value: ExprValue) : QueryResult()

    /**
     * The result of a INSERT or DELETE statement.  (UPDATE is out of scope for now.)
     *
     * Each instance of a DML command denotes an operation to be performed by the embedding PartiQL application
     * to effect the writes specified by a DML operation.
     *
     * The primary benefit of this class is that it ensures that the [rows] property is evaluated lazily.  It also
     * provides a cleaner API that is easier to work with for PartiQL embedders.  Without this, the user would have to
     * consume the [ExprValue] directly and convert it to Ion.  Neither
     * option is particularly developer friendly, efficient or maintainable.
     *
     * This is currently only factored to support `INSERT INTO` and `DELETE FROM` as `UPDATE` and `FROM ... UPDATE` is
     * out of scope for the current effort.
     */
    data class DmlCommand(
        /** Identifies the action to take. */
        val action: DmlAction,
        /** The unique identifier of the table targed by the DML statement. */
        val targetUniqueId: String,
        /**
         * The rows to be inserted or deleted.
         *
         * In the case of delete, the rows must contain at least the fields which comprise the table's primary key.
         */
        val rows: Iterable<ExprValue>
    ) : QueryResult()
}

/**
 * Identifies the action to take.
 * TODO This should be represented in the IR grammar - https://github.com/partiql/partiql-lang-kotlin/issues/756
 */
enum class DmlAction {
    INSERT,
    DELETE,
    REPLACE;

    companion object {
        fun safeValueOf(v: String): DmlAction? = try {
            valueOf(v.toUpperCase())
        } catch (ex: IllegalArgumentException) {
            null
        }
    }
}

// wVG Not used? Or the use site needs to be adjusted first before IDE can find it?
// private operator fun Bindings<ExprValue>.get(fieldName: String): ExprValue? =
//    this[BindingName(fieldName, BindingCase.SENSITIVE)]
