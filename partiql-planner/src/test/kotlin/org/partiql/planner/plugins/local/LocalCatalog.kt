package org.partiql.planner.plugins.local

import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Identifier
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Namespace
import org.partiql.planner.catalog.Routine
import org.partiql.planner.catalog.Session
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

    override fun getTable(session: Session, name: Name): Table? {
        val path = toPath(name.getNamespace()).resolve(name.getName() + EXT)
        if (path.notExists() || !path.isDirectory()) {
            return null
        }
        return LocalTable(name.getName(), path)
    }

    override fun getTableHandle(session: Session, identifier: Identifier): Table.Handle? {
        // TODO case-insensitive
        val namespace = Namespace.of(identifier.getQualifier().map { it.getText() })
        val name = identifier.last().getText()
        // lookup
        val path = toPath(namespace).resolve(name + EXT)
        if (path.notExists() || path.isDirectory()) {
            return null
        }
        return Table.Handle(
            name = Name(namespace, name),
            table = LocalTable(name, path),
        )
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

    override fun getRoutines(session: Session, name: Name): Collection<Routine> = emptyList()

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
