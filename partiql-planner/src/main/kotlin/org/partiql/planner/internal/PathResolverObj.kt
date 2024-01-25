package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObject

internal class PathResolverObj(
    catalog: ConnectorMetadata,
    session: PartiQLPlanner.Session,
) : PathResolver<ConnectorObject>(catalog, session) {

    override fun get(metadata: ConnectorMetadata, path: BindingPath): ConnectorHandle<ConnectorObject>? =
        metadata.getObject(path)

    //
    // /**
    //  * @return a [Pair] where [Pair.first] is the catalog index and [Pair.second] is the value index within that catalog
    //  */
    // private fun getOrAddCatalogValue(
    //     catalogName: String,
    //     valuePath: List<String>,
    //     valueType: StaticType,
    // ): Pair<Int, Int> {
    //     val catalogIndex = getOrAddCatalog(catalogName)
    //     val symbols = symbols[catalogIndex].symbols
    //     return symbols.indexOfFirst { value ->
    //         value.path == valuePath
    //     }.let { index ->
    //         when (index) {
    //             -1 -> {
    //                 this.symbols[catalogIndex] = this.symbols[catalogIndex].copy(
    //                     symbols = symbols + listOf(Catalog.Symbol(valuePath, valueType))
    //                 )
    //                 catalogIndex to this.symbols[catalogIndex].symbols.lastIndex
    //             }
    //             else -> {
    //                 catalogIndex to index
    //             }
    //         }
    //     }
    // }
    //
    // private fun getOrAddCatalog(catalogName: String): Int {
    //     return symbols.indexOfFirst { catalog ->
    //         catalog.name == catalogName
    //     }.let {
    //         when (it) {
    //             -1 -> {
    //                 symbols.add(Catalog(catalogName, emptyList()))
    //                 symbols.lastIndex
    //             }
    //             else -> it
    //         }
    //     }
    // }

}