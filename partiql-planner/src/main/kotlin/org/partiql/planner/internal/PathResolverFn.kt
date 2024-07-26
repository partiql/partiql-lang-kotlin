package org.partiql.planner.internal

import org.partiql.planner.catalog.Session
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorFn
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.fn.FnExperimental

/**
 * PathResolver which calls out to get matching function names.

 *
 * @param catalog
 * @param catalogs
 * @param session
 */
@OptIn(FnExperimental::class)
internal class PathResolverFn(
    catalog: ConnectorMetadata,
    catalogs: Map<String, ConnectorMetadata>,
    session: Session,
) : PathResolver<ConnectorFn>(catalog, catalogs, session) {

    /**
     * Default INFORMATION_SCHEMA.ROUTINES. Keep empty for now for top-level lookup.
     */
    override val schema: List<BindingName> = emptyList()

    override fun get(metadata: ConnectorMetadata, path: BindingPath): ConnectorHandle.Fn? = metadata.getFunction(path)
}
