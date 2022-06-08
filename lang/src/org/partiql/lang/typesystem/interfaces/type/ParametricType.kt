package org.partiql.lang.typesystem.interfaces.type

import org.partiql.lang.eval.ExprValue

/**
 * Used to define a parametric type
 */
interface ParametricType {
    /**
     * Define type of each required parameter
     */
    val requiredParameters: List<SqlType>

    /**
     * Define type & default value of each optional parameter
     */
    val optionalParameters: List<Pair<SqlType, ExprValue?>>

    // TODO: support defining variadic parameter

    /**
     * Which kind of parameters are considered as valid. e.g. We can check:
     * 1. value of each parameter
     * 2. constraints across parameters
     *
     * Throw exceptions if any check is not passed
     *
     * Note that there is no need to check total number and types of passed parameters,
     * as they are automatically checked with [requiredParameters] & [optionalParameters]
     * defined.
     */
    fun validateParameters(parameters: TypeParameters)
}
