package org.partiql.lang.ots_work.plugins.standard.operators

import com.amazon.ion.IonValue
import com.amazon.ionelement.api.toIonValue
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.ast.toPartiQlMetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.*
import org.partiql.lang.eval.err
import org.partiql.lang.eval.like.parsePattern
import org.partiql.lang.ots_work.interfaces.*
import org.partiql.lang.ots_work.interfaces.operators.LikeOp
import org.partiql.lang.ots_work.interfaces.operators.PosOp
import org.partiql.lang.ots_work.interfaces.operators.UnaryOp
import org.partiql.lang.ots_work.plugins.standard.types.CharType
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.types.VarcharType
import org.partiql.lang.util.codePointSequence
import org.partiql.lang.util.stringValue
import java.util.regex.Pattern

class StandardLikeOp(
    val valueFactory: ExprValueFactory
): LikeOp() {
    override val validOperandTypes: List<ScalarType> = ALL_TEXT_TYPES

    override fun inferType(value: CompileTimeType, pattern: CompileTimeType, escape: CompileTimeType?): TypeInferenceResult =
        when {
            value.scalarType !in validOperandTypes || pattern.scalarType !in validOperandTypes -> Failed
            escape === null -> Successful(BoolType.compileTimeType)
            escape.scalarType in validOperandTypes -> Uncertain(BoolType.compileTimeType)
            else -> Failed
        }

    override fun invoke(value: ExprValue, pattern: ExprValue, escape: ExprValue?): ExprValue {
        TODO()
    }
}
