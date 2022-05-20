package org.partiql.lang.typesystem.interfaces.type

import org.partiql.lang.eval.ExprValue

/**
 * A sql type with actual parameters. If it is a non-parametric type, [parameters] should be an empty list.
 */
data class SqlTypeWithParameters(
    val type: SqlType,
    val parameters: TypeParameters = emptyList()
)

/**
 * A value with a sql type
 */
data class ValueWithType(
    val value: ExprValue,
    val typeWithParameters: SqlTypeWithParameters
)

/**
 * Parameters of a parametric type
 */
typealias TypeParameters = List<ValueWithType>
