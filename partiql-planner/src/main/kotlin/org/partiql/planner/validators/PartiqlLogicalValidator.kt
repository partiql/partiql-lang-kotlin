package org.partiql.planner.validators

import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IntElementSize
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.TextElement
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.errors.PropertyValueMap
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.pig.runtime.LongPrimitive
import org.partiql.planner.PlannerException
import org.partiql.planner.SemanticException
import org.partiql.planner.impl.addSourceLocation
import org.partiql.planner.impl.err
import org.partiql.planner.impl.errorContextFrom

/**
 * Provides rules for basic AST sanity checks that should be performed before any attempt at further AST processing.
 * This is provided as a distinct [PartiqlLogical.Visitor] so that all other visitors may assume that the AST at least
 * passed the checking performed here.
 *
 * Any exception thrown by this class should always be considered an indication of a bug in one of the following places:
 * - [org.partiql.planner.transforms.AstToLogicalVisitorTransform]
 */
public class PartiqlLogicalValidator : PartiqlLogical.Visitor() {
    override fun visitExprLit(node: PartiqlLogical.Expr.Lit) {
        val ionValue = node.value
        val metas = node.metas
        if (node.value is IntElement && ionValue.integerSize == IntElementSize.BIG_INTEGER) {
            throw PlannerException(
                message = "Int overflow or underflow at compile time",
                errorCode = ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
                errorContext = errorContextFrom(metas),
                internal = false
            )
        }
    }

    private fun validateDecimalOrNumericType(scale: LongPrimitive?, precision: LongPrimitive?, metas: MetaContainer) {
        if (scale != null && precision != null) {
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

    override fun visitTypeDecimalType(node: PartiqlLogical.Type.DecimalType) {
        validateDecimalOrNumericType(node.scale, node.precision, node.metas)
    }

    override fun visitTypeNumericType(node: PartiqlLogical.Type.NumericType) {
        validateDecimalOrNumericType(node.scale, node.precision, node.metas)
    }

    override fun visitExprStruct(node: PartiqlLogical.Expr.Struct) {
        node.parts.forEach { part ->
            when (part) {
                is PartiqlLogical.StructPart.StructField -> {
                    if (part.fieldName is PartiqlLogical.Expr.Missing ||
                        (part.fieldName is PartiqlLogical.Expr.Lit && (part.fieldName as PartiqlLogical.Expr.Lit).value !is TextElement)
                    ) {
                        val type = when (val fieldName = part.fieldName) {
                            is PartiqlLogical.Expr.Lit -> fieldName.value.type.toString()
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
