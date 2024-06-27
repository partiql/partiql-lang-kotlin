package org.partiql.plugins.local

import org.partiql.planner.metadata.Namespace
import org.partiql.planner.metadata.Table
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension

/**
 * Thin wrapper over a directory.
 *  - subdir -> namespace
 *  - file   -> table
 */
public class LocalNamespace(private val path: Path) : Namespace {

    init {
        assert(path.isDirectory()) { "LocalNamespace must be a directory" }
    }

    override fun getName(): String = path.nameWithoutExtension

    override fun getNamespaces(): Collection<Namespace> {
        return path.toFile().listFiles { f, _ -> f.isDirectory }!!.map { LocalNamespace(it.toPath()) }
    }

    override fun getNamespace(name: String): Namespace? {
        val p = path.resolve(name)
        return when (p.isDirectory()) {
            true -> LocalNamespace(p)
            else -> null
        }
    }

    override fun getTable(name: String): Table? {
        val p = path.resolve(name)
        return when (p.isRegularFile()) {
            true -> LocalTable(p)
            else -> null
        }
    }
}
