package org.partiql.cli.plugin.localdb

import org.partiql.spi.connector.ConnectorManager
import org.partiql.spi.connector.ConnectorManagerFactory

/**
 * Let's assume that we have some external schema definition written in JSON. Looks like:
 * {
 *   "name: "tbl",
 *   "attributes": [
 *     {
 *       "name": "col_int",
 *       "type": "INT",
 *       "typeParams": [],
 *     }
 *   ]
 * }
 */

class LocalConnectorManagerFactory : ConnectorManagerFactory {
    override fun getName(): String {
        return "localdb"
    }

    override fun create(): ConnectorManager {
        return LocalConnectorManager()
    }
}
