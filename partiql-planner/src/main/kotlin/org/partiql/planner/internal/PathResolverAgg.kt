package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
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

    override fun get(metadata: ConnectorMetadata, path: BindingPath): ConnectorHandle.Agg? {
        return metadata.getAggregation(path)
    }
}
