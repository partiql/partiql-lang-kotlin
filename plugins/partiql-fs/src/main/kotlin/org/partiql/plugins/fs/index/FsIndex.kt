package org.partiql.plugins.fs.index

import com.amazon.ionelement.api.loadSingleElement
import org.partiql.plugins.fs.toStaticType
import org.partiql.spi.BindingPath
import org.partiql.types.StaticType
import java.io.File
import java.nio.file.Path

/**
 *
 */
internal class FsIndex(private val root: FsNode) {

    /**
     * Search the FsNode for the type.
     */
    fun search(path: BindingPath): Pair<FsNode.Obj, KMatch>? {
        val match = mutableListOf<String>()
        var curr: FsNode? = root
        for (step in path.steps) {
            if (curr == null) return null
            when (curr) {
                is FsNode.Obj -> break
                is FsNode.Scope -> {
                    curr = curr.children.firstOrNull { step.matches(it.name) }
                    if (curr != null) match.add(curr.name)
                }
            }
        }
        // All steps matched and we're at a leaf
        if (curr is FsNode.Obj) {
            return curr to match
        }
        return null
    }

    /**
     * List all FsNodes in the path.
     */
    fun list(path: BindingPath): List<FsNode> {
        var curr: FsNode? = root
        for (step in path.steps) {
            if (curr == null) return emptyList()
            when (curr) {
                is FsNode.Obj -> break
                is FsNode.Scope -> {
                    curr = curr.children.firstOrNull { step.matches(it.name) }
                }
            }
        }
        // All steps matched and we're at a leaf
        return when (curr) {
            is FsNode.Obj -> listOf(curr)
            is FsNode.Scope -> curr.children
            null -> emptyList()
        }
    }

    companion object {

        /**
         * Create a FsIndex from the path.
         */
        fun load(root: Path): FsIndex = FsIndex(root = root.toFile().dir())

        private fun File.dir(): FsNode.Scope {
            val children = mutableListOf<FsNode>()
            val objs = mutableMapOf<String, FsNode.Obj>()
            for (f in listFiles()!!) {
                if (f.isDirectory) {
                    children.add(f.dir())
                    continue
                }
                if (f.extension != "ion") {
                    // skip non-ion
                    continue
                }
                val parts = f.name.split('.')
                val name = parts[0]
                val kind = parts[1]
                var obj = objs[name] ?: FsNode.Obj(name, StaticType.ANY)
                obj = when (kind) {
                    "ion" -> obj.copy(data = f.data())
                    "shape" -> obj.copy(shape = f.shape())
                    else -> error("Invalid obj file kind `$kind`, file: `${f.name}`")
                }
                objs[name] = obj
            }
            children.addAll(objs.values)
            return FsNode.Scope(name, children)
        }

        private fun File.data(): File = this

        private fun File.shape(): StaticType = loadSingleElement(readText()).toStaticType()
    }
}
