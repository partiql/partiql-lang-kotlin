package org.partiql.spi.catalog.impl

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Catalogs

/**
 * Standard implementation for [Catalogs] backed by an in-memory map.
 *
 * @property catalogs
 */
internal class StandardCatalogs(private val catalogs: Map<String, Catalog>) : Catalogs {

    override fun getCatalog(name: String, ignoreCase: Boolean): Catalog? {
        // search
        if (ignoreCase) {
            var match: Catalog? = null
            for (catalog in catalogs.values) {
                if (catalog.getName().equals(name, ignoreCase = true)) {
                    if (match != null) {
                        // TODO exceptions for ambiguous catalog name lookup
                        error("Catalog name is ambiguous, found more than one match.")
                    } else {
                        match = catalog
                    }
                }
            }
            return match
        }
        // lookup
        return catalogs[name]
    }
}
