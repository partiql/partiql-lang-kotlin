package org.partiql.lang.planner.validators

import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IntElementSize
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.TextElement
import org.partiql.lang.ast.IsCountStarMeta
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.addSourceLocation
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.types.BuiltInScalarType
import org.partiql.lang.types.TYPE_ALIAS_TO_SCALAR_TYPE

/**
 * Provides rules for basic AST sanity checks that should be performed before any attempt at further AST processing.
 * This is provided as a distinct [PartiqlLogical.Visitor] so that all other visitors may assume that the AST at least
 * passed the checking performed here.
 *
 * Any exception thrown by this class should always be considered an indication of a bug in one of the following places:
 * - [org.partiql.lang.planner.transforms.AstToLogicalVisitorTransform]
 */
class PartiqlLogicalValidator(private val typedOpBehavior: TypedOpBehavior) : PartiqlLogical.Visitor() {
    override fun visitExprLit(node: PartiqlLogical.Expr.Lit) {
        val ionValue = node.value
        val metas = node.metas
        if (node.value is IntElement && ionValue.integerSize == IntElementSize.BIG_INTEGER) {
            throw EvaluationException(
                message = "Int overflow or underflow at compile time",
                errorCode = ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
                errorContext = errorContextFrom(metas),
                internal = false
            )
        }
    }

    private fun validateDecimalOrNumericType(precision: Long?, scale: Long?, metas: MetaContainer) {
        if (scale != null && precision != null && typedOpBehavior == TypedOpBehavior.HONOR_PARAMETERS) {
            if (scale !in 0..precision) {
                err(
                    "Scale $scale should be between 0 and precision $precision",
                    errorCode = ErrorCode.SEMANTIC_INVALID_DECIMAL_ARGUMENTS,
                    errorContext = errorContextFrom(metas),
                    internal = false
                )
            }
        }
    }

    override fun visitTypeScalarType(node: PartiqlLogical.Type.ScalarType) {
        super.visitTypeScalarType(node)

        val scalarType = TYPE_ALIAS_TO_SCALAR_TYPE[node.alias.text]
        if (scalarType == BuiltInScalarType.DECIMAL || scalarType == BuiltInScalarType.NUMERIC) {
            validateDecimalOrNumericType(node.parameters.getOrNull(0)?.value, node.parameters.getOrNull(1)?.value, node.metas)
        }
    }

    override fun visitExprCallAgg(node: PartiqlLogical.Expr.CallAgg) {
        val setQuantifier = node.setq
        val metas = node.metas
        if (setQuantifier is PartiqlLogical.SetQuantifier.Distinct && metas.containsKey(IsCountStarMeta.TAG)) {
            err(
                "COUNT(DISTINCT *) is not supported",
                ErrorCode.EVALUATOR_COUNT_DISTINCT_STAR,
                errorContextFrom(metas),
                internal = false
            )
        }
    }

    override fun visitExprStruct(node: PartiqlLogical.Expr.Struct) {
        node.parts.forEach { part ->
            when (part) {
                is PartiqlLogical.StructPart.StructField -> {
                    if (part.fieldName is PartiqlLogical.Expr.Missing ||
                        (part.fieldName is PartiqlLogical.Expr.Lit && part.fieldName.value !is TextElement)
                    ) {
                        val type = when (part.fieldName) {
                            is PartiqlLogical.Expr.Lit -> part.fieldName.value.type.toString()
                            else -> "MISSING"
                        }
                        throw SemanticException(
                            "Found struct part to be of type $type",
                            ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
                            PropertyValueMap().addSourceLocation(part.fieldName.metas).also { pvm ->
                                pvm[Property.ACTUAL_TYPE] = type
                            }
                        )
                    }
                }
                is PartiqlLogical.StructPart.StructFields -> { /* intentionally empty */ }
            }
        }
    }
}
