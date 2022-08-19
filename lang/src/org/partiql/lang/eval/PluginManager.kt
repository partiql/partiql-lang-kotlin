package org.partiql.lang.eval

import com.amazon.ion.IonSystem
import com.amazon.ionelement.api.AnyElement
import org.partiql.lang.eval.function.ScalarExprLib
import org.partiql.spi.Plugin
import org.partiql.spi.SourceHandle
import org.partiql.spi.SourceResolver

// COW HACK
class PluginManager {

    private val plugins = mutableMapOf<String, Plugin>()
    private val scalarLibs = mutableMapOf<String, Plugin.ScalarLib>()
    private val sources = mutableMapOf<String, SourceResolver>()

    fun register(ion: IonSystem, plugin: Plugin.Factory) {
        // TODO configuration
        val id = plugin.identifier.toUpperCase()
        plugins[id] = plugin.create(ion, null)
        sources[id] = plugin.sourceResolver
        val scalarLib = plugin.scalarLib(ion)
        if (scalarLib != null) {
            scalarLibs[id] = scalarLib
        }
    }

    fun get(identifier: String): Plugin = plugins[identifier]
        ?: throw IllegalArgumentException("no plugin `$identifier` registered")

    fun source(plugin: String, identifier: String, args: List<AnyElement>): SourceHandle {
        val resolver = sources[plugin] ?: throw IllegalArgumentException("no plugin `$plugin` registered")
        return resolver.get(identifier, args)
    }

    // https://media.giphy.com/media/cQtlhD48EG0SY/giphy.gif
    fun scalarExprLibs(valueFactory: ExprValueFactory): List<ScalarExprLib> = scalarLibs.map { (namespace, library) ->
        ScalarExprLib(
            namespace = namespace,
            library = library,
            valueFactory = valueFactory
        )
    }

}
