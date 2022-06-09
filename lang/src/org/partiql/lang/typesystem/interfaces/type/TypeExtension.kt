package org.partiql.lang.typesystem.interfaces.type

import org.partiql.lang.eval.ExprValue

/**
 * A sql type at compile time, during which type parameters are known.
 *
 * [parameters] should be an empty list for non-parametric types.
 */
data class CompileTimeType(
    val type: SqlType,
    val parameters: TypeParameters = emptyList()
)

/**
 * A type parameter. The value of type parameter must be known at compile time.
 */
data class TypeParameter(
    val value: ExprValue,
    val typeWithParameters: CompileTimeType
)

/**
 * Parameters of a parametric type
 */
typealias TypeParameters = List<TypeParameter>
