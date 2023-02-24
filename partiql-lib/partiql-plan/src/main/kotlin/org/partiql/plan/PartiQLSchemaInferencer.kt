package org.partiql.plan

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.planner.transforms.normalize
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.plan.impl.MetadataInference
import org.partiql.plan.impl.PlannerContext
import org.partiql.plan.ir.Rex
import org.partiql.plan.passes.impl.PlanTyper
import org.partiql.plan.passes.impl.PlanUtils
import org.partiql.spi.Plugin
import org.partiql.spi.sources.ColumnMetadata
import org.partiql.spi.sources.TableSchema

/**
 * Infers the output schema of a query.
 */
public object PartiQLSchemaInferencer {

    /**
     * Infers a query's schema.
     */
    public fun infer(
        query: String,
        session: PlannerSession,
        plugins: List<Plugin>
    ): TableSchema {
        val parser = PartiQLParserBuilder.standard().build()
        val ast = parser.parseAstStatement(query)
        val normalizedAst = ast.normalize()
        val ctx = PlannerContext(MetadataInference(plugins, session.catalogMap))
        return inferUsingLogicalPlan(session, ctx, normalizedAst)
    }

    /**
     * Infers using the logical plan.
     */
    private fun inferUsingLogicalPlan(session: PlannerSession, ctx: PlannerContext, normalizedAst: PartiqlAst.Statement): TableSchema {
        val query = normalizedAst as PartiqlAst.Statement.Query
        val expr = query.expr as PartiqlAst.Expr.Select
        val plan = RelConverter.convert(expr)
        val rewritten = PlanTyper.type(plan, PlanTyper.Context(session = session, plannerCtx = ctx))
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
}
