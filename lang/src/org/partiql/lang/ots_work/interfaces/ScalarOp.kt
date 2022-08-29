package org.partiql.lang.ots_work.interfaces

interface ScalarOp {
    val scalarOpId: ScalarOpId

    /**
     * Return type when we cannot decide the return type (type inference result is [Failed])
     *
     * Empty list leads to type inference result as MISSING
     */
    val defaultReturnTypes: List<CompileTimeType>
}
