package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObject

/**
 * PathResolver implementation which calls out to get database objects.
 *
 * @param catalog
 * @param session
 */
internal class PathResolverObj(
    catalog: ConnectorMetadata,
    session: PartiQLPlanner.Session,
) : PathResolver<ConnectorObject>(catalog, session) {

    override fun get(metadata: ConnectorMetadata, path: BindingPath): ConnectorHandle.Obj? = metadata.getObject(path)
}
