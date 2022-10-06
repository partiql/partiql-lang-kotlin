package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.type.ScalarType
import OTS.ITF.org.partiql.ots.type.TypeParameters
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ExprValueType

object FloatType : ScalarType {
    val compileTimeType: CompileTimeType = CompileTimeType(this, emptyList())

    override val id = "float"

    override val names = listOf("float")

    override fun validateParameters(typeParameters: TypeParameters) {
        if (typeParameters.isNotEmpty()) {
            throw SemanticException(
                message = "FLOAT precision parameter is unsupported",
                errorCode = ErrorCode.SEMANTIC_FLOAT_PRECISION_UNSUPPORTED,
            )
        }
    }

    override val runTimeType: ExprValueType
        get() = ExprValueType.FLOAT
}
