package org.partiql.lang.typesystem.interfaces.type

/**
 * Used to define a parametric type
 */
interface ParametricType {
    /**
     * Which kind of parameters are considered as valid for the type. We can check:
     * 1. number of parameters
     * 2. type of each parameter
     * 3. value of each parameter
     * 4. constraints
     */
    fun validateParameters(parameters: List<ValueWithType>): Boolean
}