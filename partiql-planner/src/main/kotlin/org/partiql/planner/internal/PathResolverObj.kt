package org.partiql.planner.internal

import org.partiql.planner.catalog.Session
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObject

/**
 * PathResolver implementation which calls out to get database objects.
 *
 * @param catalog
 * @param catalogs
 * @param session
 */
internal class PathResolverObj(
    catalog: ConnectorMetadata,
    catalogs: Map<String, ConnectorMetadata>,
    session: Session,
) : PathResolver<ConnectorObject>(catalog, catalogs, session) {

    override fun get(metadata: ConnectorMetadata, path: BindingPath): ConnectorHandle.Obj? = metadata.getObject(path)
}
