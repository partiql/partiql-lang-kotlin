package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IntElementSize
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.TextElement
import org.partiql.lang.ast.IsCountStarMeta
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.domains.addSourceLocation
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.pig.runtime.LongPrimitive

/**
 * Provides rules for basic AST sanity checks that should be performed before any attempt at further phsycial
 * plan processing. This is provided as a distinct [PartiqlPhysical.Visitor] so that [PlanCompiler] may assume that
 * the physical plan passed the checks performed here.
 *
 * Any exception thrown by this class should always be considered an indication of a bug:
 */
class PartiqlPhysicalSanityValidator(val evaluatorOptions: EvaluatorOptions) : PartiqlPhysical.Visitor() {

    /**
     * Quick validation step to make sure the indexes of any variables make sense.
     * It is unlikely that this check will ever fail, but if it does, it likely means there's a bug in
     * [org.partiql.lang.planner.transforms.VariableIdAllocator] or that the plan was malformed by other means.
     */
    override fun visitPlan(node: PartiqlPhysical.Plan) {
        node.locals.forEachIndexed { idx, it ->
            if (it.registerIndex.value != idx.toLong()) {
                throw EvaluationException(
                    message = "Variable index must match ordinal position of variable",
                    errorCode = ErrorCode.INTERNAL_ERROR,
                    errorContext = propertyValueMapOf(),
                    internal = true
                )
            }
        }
        super.visitPlan(node)
    }

    override fun visitExprLit(node: PartiqlPhysical.Expr.Lit) {
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

    private fun validateDecimalOrNumericType(scale: LongPrimitive?, precision: LongPrimitive?, metas: MetaContainer) {
        if (scale != null && precision != null && evaluatorOptions.typedOpBehavior == TypedOpBehavior.HONOR_PARAMETERS) {
            if (scale.value !in 0..precision.value) {
                err(
                    "Scale ${scale.value} should be between 0 and precision ${precision.value}",
                    errorCode = ErrorCode.SEMANTIC_INVALID_DECIMAL_ARGUMENTS,
                    errorContext = errorContextFrom(metas),
                    internal = false
                )
            }
        }
    }

    override fun visitTypeDecimalType(node: PartiqlPhysical.Type.DecimalType) {
        validateDecimalOrNumericType(node.scale, node.precision, node.metas)
    }

    override fun visitTypeNumericType(node: PartiqlPhysical.Type.NumericType) {
        validateDecimalOrNumericType(node.scale, node.precision, node.metas)
    }

    override fun visitExprCallAgg(node: PartiqlPhysical.Expr.CallAgg) {
        val setQuantifier = node.setq
        val metas = node.metas
        if (setQuantifier is PartiqlPhysical.SetQuantifier.Distinct && metas.containsKey(IsCountStarMeta.TAG)) {
            err(
                "COUNT(DISTINCT *) is not supported",
                ErrorCode.EVALUATOR_COUNT_DISTINCT_STAR,
                errorContextFrom(metas),
                internal = false
            )
        }
    }

    override fun visitExprStruct(node: PartiqlPhysical.Expr.Struct) {
        node.fields.forEach { field ->
            if (field.first is PartiqlPhysical.Expr.Missing || (field.first is PartiqlPhysical.Expr.Lit && field.first.value !is TextElement)) {
                val type = when (field.first) {
                    is PartiqlPhysical.Expr.Lit -> field.first.value.type.toString()
                    else -> "MISSING"
                }
                throw SemanticException(
                    "Found struct field to be of type $type",
                    ErrorCode.SEMANTIC_NON_TEXT_STRUCT_FIELD_KEY,
                    PropertyValueMap().addSourceLocation(field.first.metas).also { pvm ->
                        pvm[Property.ACTUAL_TYPE] = type
                    }
                )
            }
        }
    }
}
