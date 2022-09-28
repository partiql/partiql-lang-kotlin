package OTS.ITF.org.partiql.ots.operator

import OTS.ITF.org.partiql.ots.CompileTimeType

interface ScalarOp {
    /**
     * Return type when we cannot decide the return type (type inference result is [Failed])
     *
     * Empty list leads to type inference result as MISSING
     */
    val defaultReturnTypes: List<CompileTimeType>
}
