package org.partiql.lang.ots_work.interfaces

/**
 * For now, we assume all the type parameters are optional and all the typa parameters
 * are integers. The length of this list represents the number of optional type parameters
 * a type has. Null value means the parameter is not explicitly specified in the original
 * query
 */
typealias TypeParameters = List<Int?>

data class CompileTimeType(
    val scalarType: ScalarType,
    val parameters: TypeParameters
)

sealed class TypeInferenceResult

class Successful(
    val compileTimeType: CompileTimeType
) : TypeInferenceResult()

class Failed(
    val compileTimeType: CompileTimeType? = null
) : TypeInferenceResult()

class Uncertain(
    val compileTimeType: CompileTimeType
) : TypeInferenceResult()
