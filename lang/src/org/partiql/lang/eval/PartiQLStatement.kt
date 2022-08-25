package org.partiql.lang.eval

/**
 * A compiled PartiQL statement
 */
fun interface PartiQLStatement {

    fun eval(session: EvaluationSession): PartiQLResult
}
