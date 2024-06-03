package org.partiql.planner.intern.validate

import org.partiql.planner.intern.SqlTypes
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.metadata.Context
import org.partiql.planner.metadata.Session
import org.partiql.types.StaticType

/**
 * SqlAnalyzer is responsible for semantic analysis/validation of the internal, algebraic IR.
 *
 * Responsibilities:
 *  - name resolution (variables, tables, routines)
 *  - type-checking
 *  - type-coercion
 *
 * https://github.com/apache/calcite/tree/main/core/src/main/java/org/apache/calcite/sql/validate
 */
internal class SqlAnalyzer(
    private val context: Context,
    private val session: Session,
    private val types: SqlTypes<StaticType>
) {

    fun analyze(statement: Statement): Statement {
        // Placeholder for the SQL analysis phase.
        return statement
    }
}
