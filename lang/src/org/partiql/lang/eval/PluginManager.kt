package org.partiql.lang.eval

import com.amazon.ion.IonSystem
import com.amazon.ionelement.api.AnyElement
import org.partiql.spi.Plugin
import org.partiql.spi.SourceHandle
import org.partiql.spi.SourceResolver

// COW HACK
class PluginManager {

  private val plugins = mutableMapOf<String, Plugin>()
  private val sources = mutableMapOf<String, SourceResolver>()

  fun register(ionSystem: IonSystem, plugin: Plugin.Factory) {
    // TODO configuration
    val id = plugin.identifier.toUpperCase()
    plugins[id] = plugin.create(ionSystem, null)
    sources[id] = plugin.sourceResolver
  }

  fun get(identifier: String): Plugin = plugins[identifier] ?: throw IllegalArgumentException("no plugin `$identifier` registered")

  fun source(plugin: String, identifier: String, args: List<AnyElement>): SourceHandle {
    val resolver = sources[plugin] ?: throw IllegalArgumentException("no plugin `$plugin` registered")
    return resolver.get(identifier, args)
  }

}
