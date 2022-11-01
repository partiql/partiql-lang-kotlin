package OTS.ITF.org.partiql.ots

import OTS.ITF.org.partiql.ots.type.ScalarType
import OTS.ITF.org.partiql.ots.type.TypeParameters

/**
 * A type assigned to data (a scalar value). Basically, a [scalarType] with certain type parameters.
 */
data class CompileTimeType(
    val scalarType: ScalarType,
    val parameters: TypeParameters
)

sealed class TypeInferenceResult

class Successful(
    val compileTimeType: CompileTimeType
) : TypeInferenceResult()

object Failed : TypeInferenceResult()

class Uncertain(
    val compileTimeType: CompileTimeType
) : TypeInferenceResult()
