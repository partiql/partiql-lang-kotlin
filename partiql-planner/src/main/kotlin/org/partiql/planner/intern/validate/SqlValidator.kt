package org.partiql.planner.intern.validate

import org.partiql.planner.intern.SqlTypes
import org.partiql.planner.intern.validate.validators.FnValidator
import org.partiql.planner.intern.validate.validators.OpValidator
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.metadata.Session
import org.partiql.planner.metadata.System
import org.partiql.types.StaticType

/**
 * SqlAnalyzer is responsible for semantic analysis/validation of the internal, algebraic IR.
 *
 * Responsibilities:
 *  - name resolution (variables, tables, routines)
 *  - type-checking
 *  - type-coercion
 */
internal class SqlValidator(
    private val system: System,
    private val session: Session,
    private val types: SqlTypes<StaticType>,
) {

    fun validate(statement: Statement): Statement {
        // Placeholder for the SQL analysis phase.
        return statement
    }

    /**
     * TODO
     */
    fun getOpValidator(symbol: String): OpValidator {
        val variants = system.getOperators(symbol)
        return OpValidator(variants)
    }

    fun getFnValidator(name: String): FnValidator? {
        return null
    }
}
