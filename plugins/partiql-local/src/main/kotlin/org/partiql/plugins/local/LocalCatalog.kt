package org.partiql.plugins.local

import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Namespace
import org.partiql.planner.catalog.Routine
import org.partiql.planner.catalog.Table
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists

/**
 * Implementation of [Catalog] where dirs are namespaces and files are table metadata.
 */
internal class LocalCatalog(
    private val name: String,
    private val root: Path,
) : Catalog {

    private companion object {
        private const val EXT = ".ion"
    }

    init {
        assert(root.isDirectory()) { "LocalNamespace must be a directory" }
    }

    override fun getName(): String {
        return name
    }

    override fun getTable(name: Name): Table? {
        val path = toPath(name.getNamespace()).resolve(name.getName() + EXT)
        if (path.notExists() || !path.isDirectory()) {
            return null
        }
        return LocalTable(name.getName(), path)
    }

    override fun listTables(namespace: Namespace): Collection<Name> {
        val path = toPath(namespace)
        if (path.notExists()) {
            // throw exception?
            return emptyList()
        }
        return super.listTables(namespace)
    }

    override fun listNamespaces(namespace: Namespace): Collection<Namespace> {
        val path = toPath(namespace)
        if (path.notExists() || path.isDirectory()) {
            // throw exception?
            return emptyList()
        }
        // List all child directories
        return path.toFile()
            .listFiles()!!
            .filter { it.isDirectory }
            .map { toNamespace(it.toPath()) }
    }

    override fun getRoutines(name: Name): Collection<Routine> = emptyList()

    private fun toPath(namespace: Namespace): Path {
        var curr = root
        for (level in namespace) {
            curr = curr.resolve(level)
        }
        return curr
    }

    private fun toNamespace(path: Path): Namespace {
        return Namespace.of(path.relativize(root).map { it.toString() })
    }
}
