package OTS.ITF.org.partiql.ots

import OTS.ITF.org.partiql.ots.type.ScalarType

/**
 * For now, we assume all the type parameters are optional and all the type parameters
 * are integers.
 */
typealias TypeParameters = List<Int>

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
