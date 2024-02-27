package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.casts.CastTable
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorAgg
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.fn.FnExperimental

/**
 * Today, all aggregations are hard-coded into the grammar. We cannot implement user-defined aggregations until
 * the grammar and AST are updated appropriately. We should not have an aggregation node in the AST, just a call node.
 * During planning, we would then check if a call is an aggregation and translate the AST to the appropriate algebra.
 *
 * PartiQL.g4
 *
 * aggregate
 *     : func=COUNT PAREN_LEFT ASTERISK PAREN_RIGHT
 *     | func=(COUNT|MAX|MIN|SUM|AVG|EVERY|ANY|SOME) PAREN_LEFT setQuantifierStrategy? expr PAREN_RIGHT
 *     ;
 *
 */
@OptIn(FnExperimental::class)
internal class PathResolverAgg(
    catalog: ConnectorMetadata,
    session: PartiQLPlanner.Session,
) : PathResolver<ConnectorAgg>(catalog, session) {

    companion object {
        @JvmStatic
        public val casts = CastTable.partiql
    }

    override fun get(metadata: ConnectorMetadata, path: BindingPath): ConnectorHandle.Agg? {
        return metadata.getAggregation(path)
    }
}
