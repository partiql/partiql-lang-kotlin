package org.partiql.lang.planner

import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType

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
     * consume the [ExprValue] directly and use code similar to that in [toDmlCommand] or convert it to Ion.  Neither
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
         * In the case of delete, the rows must contain at least the fields which comprise the primary key.
         */
        val rows: Iterable<ExprValue>
    ) : QueryResult()
}

/** Identifies the action to take. */
enum class DmlAction { INSERT, DELETE }

internal const val DML_COMMAND_FIELD_ACTION = "action"
internal const val DML_COMMAND_FIELD_TARGET_UNIQUE_ID = "target_unique_id"
internal const val DML_COMMAND_FIELD_ROWS = "rows"

private operator fun Bindings<ExprValue>.get(fieldName: String): ExprValue? =
    this[BindingName(fieldName, BindingCase.SENSITIVE)]

private fun errMissing(fieldName: String): Nothing =
    error("'$fieldName' missing from DML command struct or has incorrect Ion type")

/**
 * Converts an [ExprValue] which is the result of a DML query to an instance of [DmlCommand].
 *
 * Format of a DML command:
 *
 * ```
 * {
 *     'action': <action>,
 *     'target_unique_id': <unique_id>
 *     'rows': <rows>
 * }
 * ```
 *
 * Where:
 *  - `<action>` is either `insert` or `delete`
 *  - `<target_unique_id>` is a string or symbol containing the unique identifier of the table to be effected
 *  by the DML statement.
 *  - `<rows>` is a list of bag containing the rows (structs) effected by the DML statement.
 *      - When `<action>` is `insert` this is the rows to be inserted.
 *      - When `<action>` is `delete` this is the rows to be deleted.  Non-primary key fields may be elided, but the
 *      default behavior is to include all fields because PartiQL does not yet know about primary keys.
 */
internal fun ExprValue.toDmlCommand(): QueryResult.DmlCommand {
    require(this.type == ExprValueType.STRUCT) { "'row' must be a struct" }

    val actionString = this.bindings[DML_COMMAND_FIELD_ACTION]?.scalar?.stringValue()?.toUpperCase()
        ?: errMissing(DML_COMMAND_FIELD_ACTION)

    val dmlAction = DmlAction.values().firstOrNull { it.name == actionString }
        ?: error("Unknown DmlAction in DML command struct: '$actionString'")

    val targetUniqueId = this.bindings[DML_COMMAND_FIELD_TARGET_UNIQUE_ID]?.scalar?.stringValue()
        ?: errMissing(DML_COMMAND_FIELD_TARGET_UNIQUE_ID)

    val rows = this.bindings[DML_COMMAND_FIELD_ROWS] ?: errMissing(DML_COMMAND_FIELD_ROWS)
    if (!rows.type.isSequence) {
        error("DML command struct '$DML_COMMAND_FIELD_ROWS' field must be a bag or list")
    }

    return QueryResult.DmlCommand(dmlAction, targetUniqueId, rows)
}
