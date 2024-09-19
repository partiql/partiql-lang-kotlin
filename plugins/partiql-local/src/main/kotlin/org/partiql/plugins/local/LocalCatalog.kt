package org.partiql.plugins.local

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Namespace
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
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

    override fun getTable(session: Session, name: Name): Table? {
        val path = toPath(name.getNamespace()).resolve(name.getName() + EXT)
        if (path.notExists() || path.isDirectory()) {
            return null
        }
        return LocalTable(name, path)
    }

    /**
     * TODO this doesn't handle ambiguous binding errors or back-tracking for longest prefix searching.
     */
    override fun getTableHandle(session: Session, identifier: Identifier): Table.Handle? {
        val matched = mutableListOf<String>()
        var curr = root
        for (part in identifier) {
            var next: Path? = null
            for (child in curr.toFile().listFiles()!!) {
                // TODO ambiguous bindings errors
                if (part.matches(child.nameWithoutExtension)) {
                    next = child.toPath()
                    break
                }
            }
            if (next == null) {
                break
            }
            curr = next
            matched.add(curr.nameWithoutExtension)
        }
        // Does this table exist?
        val path = curr
        if (path.notExists() || path.isDirectory()) {
            return null
        }
        // Remove the extension
        val name = Name.of(matched)
        return Table.Handle(name, LocalTable(name, path))
    }

    override fun listTables(session: Session, namespace: Namespace): Collection<Name> {
        val path = toPath(namespace)
        if (path.notExists()) {
            // throw exception?
            return emptyList()
        }
        return super.listTables(session, namespace)
    }

    override fun listNamespaces(session: Session, namespace: Namespace): Collection<Namespace> {
        val path = toPath(namespace)
        if (path.notExists() || !path.isDirectory()) {
            // throw exception?
            return emptyList()
        }
        // List all child directories
        return path.toFile()
            .listFiles()!!
            .filter { it.isDirectory }
            .map { toNamespace(it.toPath()) }
    }

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
