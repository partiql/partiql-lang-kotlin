package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IntElementSize
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.TextElement
import org.partiql.lang.ast.IsCountStarMeta
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.domains.addSourceLocation
import org.partiql.lang.domains.errorContextFrom
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.err
import org.partiql.pig.runtime.LongPrimitive

/**
 * TODO: kdoc
 */
/**
 * Provides rules for basic AST sanity checks that should be performed before any attempt at further AST processing.
 * This is provided as a distinct [PartiqlPhysical.Visitor] so that all other visitors may assume that the AST at least
 * passed the checking performed here.
 *
 * Any exception thrown by this class should always be considered an indication of a bug in one of the following places:
 *
 * - [org.partiql.lang.syntax.SqlParser]
 * - A visitor transform pass (internal or external)
 *
 */
class PartiqlPhysicalSanityValidator : PartiqlPhysical.Visitor() {

    private var compileOptions = CompileOptions.standard()

    fun validate(statement: PartiqlPhysical.Statement, compileOptions: CompileOptions = CompileOptions.standard()) {
        this.compileOptions = compileOptions
        this.walkStatement(statement)
    }

    override fun visitExprLit(node: PartiqlPhysical.Expr.Lit) {
        val ionValue = node.value
        val metas = node.metas
        if(node.value is IntElement && ionValue.integerSize == IntElementSize.BIG_INTEGER) {
            throw EvaluationException(message = "Int overflow or underflow at compile time",
                errorCode = ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
                errorContext = errorContextFrom(metas),
                internal = false)
        }
    }

    private fun validateDecimalOrNumericType(scale: LongPrimitive?, precision: LongPrimitive?, metas: MetaContainer) {
        if (scale != null && precision != null && compileOptions.typedOpBehavior == TypedOpBehavior.HONOR_PARAMETERS) {
            if (scale.value !in 0..precision.value) {
                err("Scale ${scale.value} should be between 0 and precision ${precision.value}",
                    errorCode = ErrorCode.SEMANTIC_INVALID_DECIMAL_ARGUMENTS,
                    errorContext = errorContextFrom(metas),
                    internal = false)
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
            err("COUNT(DISTINCT *) is not supported",
                ErrorCode.EVALUATOR_COUNT_DISTINCT_STAR,
                errorContextFrom(metas),
                internal = false)
        }
    }

    override fun visitExprSelect(node: PartiqlPhysical.Expr.Select) {
        val projection = node.project
        val groupBy = node.group
        val having = node.having
        val metas = node.metas

        if (groupBy != null) {
            if (groupBy.strategy is PartiqlPhysical.GroupingStrategy.GroupPartial) {
                err("GROUP PARTIAL not supported yet",
                    ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                    errorContextFrom(metas).also {
                        it[Property.FEATURE_NAME] = "GROUP PARTIAL"
                    }, internal = false)
            }

            when (projection) {
                is PartiqlPhysical.Projection.ProjectPivot -> {
                    err("PIVOT with GROUP BY not supported yet",
                        ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                        errorContextFrom(metas).also {
                            it[Property.FEATURE_NAME] = "PIVOT with GROUP BY"
                        }, internal = false)
                }
                is PartiqlPhysical.Projection.ProjectValue, is PartiqlPhysical.Projection.ProjectList -> {
                    // use of group by with SELECT & SELECT VALUE is supported
                }
            }
        }

        if ((groupBy == null || groupBy.keyList.keys.isEmpty()) && having != null) {
            throw SemanticException("HAVING used without GROUP BY (or grouping expressions)",
                ErrorCode.SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY,
                PropertyValueMap().addSourceLocation(metas))
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
