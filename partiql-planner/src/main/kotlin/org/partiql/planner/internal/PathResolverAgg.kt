package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.casts.CastTable
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorAgg
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.fn.FnExperimental

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
