package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorFn
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.fn.FnExperimental

/**
 * PathResolver which calls out to get matching function names.

 *
 * @param catalog
 * @param session
 */
@OptIn(FnExperimental::class)
internal class PathResolverFn(
    catalog: ConnectorMetadata,
    session: PartiQLPlanner.Session,
) : PathResolver<ConnectorFn>(catalog, session) {

    override fun get(metadata: ConnectorMetadata, path: BindingPath): ConnectorHandle.Fn? = metadata.getFunction(path)
}
