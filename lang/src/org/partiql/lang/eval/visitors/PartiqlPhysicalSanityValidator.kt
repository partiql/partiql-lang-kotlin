package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.TextElement
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.domains.addSourceLocation
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.planner.EvaluatorOptions

/**
 * Provides rules for basic AST sanity checks that should be performed before any attempt at further physical
 * plan processing. This is provided as a distinct [PartiqlPhysical.Visitor] so that the planner and evaluator may
 * assume that the physical plan has passed the checks performed here.
 *
 * Any exception thrown by this class should always be considered an indication of a bug.
 */
internal class PartiqlPhysicalSanityValidator(private val evaluatorOptions: EvaluatorOptions) : PartiqlPhysical.Visitor() {
    override fun visitExprStruct(node: PartiqlPhysical.Expr.Struct) {
        node.parts.forEach { part ->
            when (part) {
                is PartiqlPhysical.StructPart.StructField -> {
                    if (part.fieldName is PartiqlPhysical.Expr.Missing ||
                        (part.fieldName is PartiqlPhysical.Expr.Lit && part.fieldName.value !is TextElement)
                    ) {
                        val type = when (part.fieldName) {
                            is PartiqlPhysical.Expr.Lit -> part.fieldName.value.type.toString()
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
                is PartiqlPhysical.StructPart.StructFields -> { /* intentionally empty */ }
            }
        }
    }
}
