package org.partiql.planner.plugins.local

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
class LocalCatalog internal constructor(
    private val name: String,
    private val root: Path,
) : Catalog {

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
    override fun resolveTable(session: Session, identifier: Identifier): Name? {
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
        return Name.of(matched)
    }

    // TODO preserving this logic if catalog regains the listing APIs.
    // override fun listTables(session: Session, namespace: Namespace): Collection<Name> {
    //     val path = toPath(namespace)
    //     if (path.notExists()) {
    //         // throw exception?
    //         return emptyList()
    //     }
    //     return super.listTables(session, namespace)
    // }

    // TODO preserving this logic if catalog regains the listing APIs.
    // override fun listNamespaces(session: Session, namespace: Namespace): Collection<Namespace> {
    //     val path = toPath(namespace)
    //     if (path.notExists() || !path.isDirectory()) {
    //         // throw exception?
    //         return emptyList()
    //     }
    //     // List all child directories
    //     return path.toFile()
    //         .listFiles()!!
    //         .filter { it.isDirectory }
    //         .map { toNamespace(it.toPath()) }
    // }

    private fun toPath(namespace: Namespace): Path {
        var curr = root
        for (level in namespace) {
            curr = curr.resolve(level)
        }
        return curr
    }

    // TODO preserving this logic if catalog regains the listing APIs.
    // private fun toNamespace(path: Path): Namespace {
    //     return Namespace.of(path.relativize(root).map { it.toString() })
    // }

    companion object {

        private const val EXT = ".ion"

        @JvmStatic
        fun builder(): Builder =
            Builder()
    }

    class Builder internal constructor() {

        private var name: String? = null
        private var root: Path? = null

        fun name(name: String): Builder = apply { this.name = name }

        fun root(root: Path): Builder = apply { this.root = root }

        fun build(): LocalCatalog = LocalCatalog(name!!, root!!)
    }
}
