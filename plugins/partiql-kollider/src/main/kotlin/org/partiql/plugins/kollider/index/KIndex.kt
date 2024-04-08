package org.partiql.plugins.kollider.index

import com.amazon.ionelement.api.loadSingleElement
import org.partiql.plugins.kollider.toStaticType
import org.partiql.spi.BindingPath
import org.partiql.types.StaticType
import java.io.File
import java.nio.file.Path

/**
 *
 */
internal class KIndex(private val root: KNode) {

    /**
     * Search the KNode for the type.
     */
    public fun lookup(path: BindingPath): Pair<KNode.Obj, KMatch>? {
        val match = mutableListOf<String>()
        var curr: KNode? = root
        for (step in path.steps) {
            if (curr == null) return null
            when (curr) {
                is KNode.Obj -> break
                is KNode.Scope -> {
                    curr = curr.children.firstOrNull { step.matches(it.name) }
                    if (curr != null) match.add(curr.name)
                }
            }
        }
        // All steps matched and we're at a leaf
        if (curr is KNode.Obj) {
            return curr to match
        }
        return null
    }

    companion object {

        /**
         * Create a KIndex from the path.
         */
        fun load(root: Path): KIndex = KIndex(root = root.toFile().dir())

        private fun File.dir(): KNode.Scope {
            val children = mutableListOf<KNode>()
            val objs = mutableMapOf<String, KNode.Obj>()
            for (f in listFiles()!!) {
                if (f.isDirectory) {
                    children.add(f.dir())
                    continue
                }
                val parts = f.nameWithoutExtension.split(".")
                val name = parts[0]
                val kind = parts[1]
                var obj = objs[name] ?: KNode.Obj(name, StaticType.ANY)
                obj = when (kind) {
                    "ion" -> obj.copy(data = f.data())
                    "shape" -> obj.copy(shape = f.shape())
                    else -> error("Invalid obj file kind `$kind`, file: `${f.name}`")
                }
                objs[name] = obj
            }
            children.addAll(objs.values)
            return KNode.Scope(name, children)
        }

        private fun File.data(): File = this

        private fun File.shape(): StaticType = loadSingleElement(readText()).toStaticType()
    }
}
