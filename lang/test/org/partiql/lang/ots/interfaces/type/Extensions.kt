package org.partiql.lang.ots.interfaces.type

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.Plugin
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.TypeParameters
import OTS.ITF.org.partiql.ots.operator.ScalarOp
import OTS.ITF.org.partiql.ots.type.ScalarType
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType

internal val ion = IonSystemBuilder.standard().build()
internal val valueFactory = ExprValueFactory.standard(ion)

internal open class DummyPlugin : Plugin {
    override val scalarTypes: List<ScalarType>
        get() = error("Not yet implemented")
    override fun findScalarType(typeAlias: String): ScalarType? {
        error("Not yet implemented")
    }
    override val posOp: ScalarOp
        get() = error("Not yet implemented")
    override val negOp: ScalarOp
        get() = error("Not yet implemented")
    override val binaryPlusOp: ScalarOp
        get() = error("Not yet implemented")
    override val binaryMinusOp: ScalarOp
        get() = error("Not yet implemented")
    override val binaryTimesOp: ScalarOp
        get() = error("Not yet implemented")
    override val binaryDivideOp: ScalarOp
        get() = error("Not yet implemented")
    override val binaryModuloOp: ScalarOp
        get() = error("Not yet implemented")
    override val binaryConcatOp: ScalarOp
        get() = error("Not yet implemented")
    override val notOp: ScalarOp
        get() = error("Not yet implemented")
    override val likeOp: ScalarOp
        get() = error("Not yet implemented")
    override fun scalarTypeCastInference(
        sourceType: CompileTimeType,
        targetType: CompileTimeType
    ): TypeInferenceResult {
        error("Not yet implemented")
    }
}

internal open class DummyScalarType : ScalarType {
    override val typeName: String
        get() = error("Not yet implemented")
    override val aliases: List<String>
        get() = error("Not yet implemented")
    override fun validateParameters(typeParameters: TypeParameters) {
        error("Not yet implemented")
    }
    override val runTimeType: ExprValueType
        get() = error("Not yet implemented")
}
