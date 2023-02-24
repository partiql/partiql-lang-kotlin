package org.partiql.plan

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.ast.passes.inference.StaticTypeInferencer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.visitors.StaticTypeVisitorTransform
import org.partiql.lang.infer.PlannerContext
import org.partiql.lang.infer.Session
import org.partiql.lang.planner.transforms.normalize
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.plan.ir.Rex
import org.partiql.plan.passes.PlanTyper
import org.partiql.plan.utils.PlanUtils
import org.partiql.spi.sources.ColumnMetadata
import org.partiql.spi.sources.TableSchema
import org.partiql.spi.types.CollectionType
import org.partiql.spi.types.StaticType
import org.partiql.spi.types.StructType

/**
 * Infers the output schema of a query.
 */
public object PartiQLSchemaInferencer {

    /**
     * Infers a query's schema.
     */
    public fun infer(
        query: String,
        session: Session,
        ctx: PlannerContext
    ): TableSchema {
        val parser = PartiQLParserBuilder.standard().build()
        val ast = parser.parseAstStatement(query)
        val normalizedAst = ast.normalize()
        return inferUsingLogicalPlan(session, ctx, normalizedAst)
    }

    /**
     * Infers using the logical plan.
     */
    private fun inferUsingLogicalPlan(session: Session, ctx: PlannerContext, normalizedAst: PartiqlAst.Statement): TableSchema {
        val query = normalizedAst as PartiqlAst.Statement.Query
        val expr = query.expr as PartiqlAst.Expr.Select
        val plan = RelConverter.convert(expr)
        val rewritten = PlanTyper.type(plan, PlanTyper.Context(session = session.toConnectorSession(), plannerCtx = ctx))
        return convertSchema(rewritten)
    }

    private fun convertSchema(rex: Rex): TableSchema {
        if (rex !is Rex.Query.Collection) { TODO("Can only infer queries at the moment.") }
        val attrs = PlanUtils.getSchema(rex.rel).map { attr -> ColumnMetadata(attr.name, attr.type, null, emptyMap()) }
        return TableSchema(
            root = "UNSPECIFIED",
            steps = emptyList(),
            ordering = TableSchema.SchemaOrdering.UNORDERED,
            attributeOrdering = TableSchema.AttributeOrdering.ORDERED,
            attributes = attrs
        )
    }

    /**
     * Infers the output schema of a query using the AST.
     */
    private fun inferUsingAst(session: Session, ctx: PlannerContext, normalizedAst: PartiqlAst.Statement): TableSchema {
        // Create Inferencer
        val typedAst = StaticTypeVisitorTransform(
            ion = IonSystemBuilder.standard().build(),
            session = session,
            metadata = ctx.metadata
        ).transformStatement(normalizedAst)
        val inferencer = StaticTypeInferencer(
            session = session,
            metadata = ctx.metadata
        )

        // Gather Result
        when (val type = inferencer.inferStaticType(typedAst)) {
            is StaticTypeInferencer.InferenceResult.Success -> {
                return type.staticType.toTableSchema()
            }
            is StaticTypeInferencer.InferenceResult.Failure -> {
                println("Failed to infer output schema.")
                type.problems.forEach { problem ->
                    println(problem.toString())
                }
                throw RuntimeException("Failed to infer output schema.")
            }
        }
    }

    private fun StaticType.toTableSchema(): TableSchema {
        val bag = this as CollectionType
        val struct = bag.elementType as StructType
        val attrs = struct.fields.map { field ->
            ColumnMetadata(field.key, field.value, null, emptyMap())
        }
        return TableSchema(
            "UNSPECIFIED_NAME",
            emptyList(),
            TableSchema.SchemaOrdering.UNORDERED,
            TableSchema.AttributeOrdering.ORDERED,
            attrs
        )
    }
}
