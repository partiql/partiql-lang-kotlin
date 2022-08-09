package org.partiql.lang.ots.interfaces

import org.partiql.lang.eval.ExprValueType

/**
 * This interface is used to define a scalar type
 */
interface ScalarType {
    /**
     * a type ID
     */
    val id: String

    /**
     * Run-time type
     */
    val runTimeType: ExprValueType

    /**
     * Create an instance of this type
     */
    fun createType(parameters: TypeParameters): CompileTimeType
}
