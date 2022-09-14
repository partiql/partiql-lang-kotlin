package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.operator.BinaryDivideOp
import org.partiql.lang.ots_work.interfaces.type.ScalarType
import org.partiql.lang.ots_work.plugins.standard.plugin.BehaviorWhenDivisorIsZero
import org.partiql.lang.util.div

class StandardBinaryDivideOp(
    val behaviorWhenDivisorIsZero: BehaviorWhenDivisorIsZero?,
    var currentLocationMeta: SourceLocationMeta? = null
) : BinaryDivideOp() {
    override val validOperandTypes: List<ScalarType> =
        ALL_NUMBER_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        defaultReturnTypesOfArithmeticOp

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult =
        inferTypeOfArithmeticOp(lType, rType)
}
