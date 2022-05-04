package org.partiql.lang.typesystem.interfaces.type

import com.amazon.ion.IonValue

/**
 * A sql type with actual parameters. If it is a non-parametric type, [parameters] should be an empty list.
 */
data class TypeWithParameters (
    val type: SqlType,
    val parameters: TypeParameters = emptyList()
)

/**
 * A value with a sql type
 */
data class ValueWithType(
    val typeWithParameters: TypeWithParameters,
    val value: IonValue
)

/**
 * Parameters of a parametric type
 */
typealias TypeParameters = List<ValueWithType>