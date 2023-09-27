package org.partiql.plugins.local

import com.amazon.ionelement.api.loadSingleElement
import org.partiql.spi.BindingPath
import org.partiql.types.StaticType
import java.io.File
import java.nio.file.Path

private sealed class FsTree(val name: String) {

    // "Directory" node
    class D(name: String, val children: List<FsTree>) : FsTree(name)

    // Type node
    class T(name: String, val type: StaticType) : FsTree(name)
}

/**
 * Build a memoized catalog tree from local schema definitions.
 */
public class LocalCatalog private constructor(private val root: FsTree.D) {

    /**
     * Search the tree for the type.
     */
    public fun lookup(path: BindingPath): LocalObject? {
        val match = mutableListOf<String>()
        var curr: FsTree? = root
        for (step in path.steps) {
            if (curr == null) return null
            when (curr) {
                is FsTree.T -> break
                is FsTree.D -> {
                    curr = curr.children.firstOrNull { step.isEquivalentTo(it.name) }
                    if (curr != null) match.add(curr.name)
                }
            }
        }
        // All steps matched and we're at a leaf
        if (curr is FsTree.T) {
            return LocalObject(match, curr.type)
        }
        return null
    }

    companion object {

        /**
         * Builds a FsTree from the given root.
         */
        public fun load(root: Path): LocalCatalog = LocalCatalog(root.toFile().tree() as FsTree.D)

        private fun File.tree(): FsTree = when (this.isDirectory) {
            true -> d()
            else -> t()
        }

        private fun File.d(): FsTree.D {
            val children = listFiles()!!.map { it.tree() }
            return FsTree.D(name, children)
        }

        private fun File.t(): FsTree.T {
            val text = readText()
            val ion = loadSingleElement(text)
            val type = ion.toStaticType()
            return FsTree.T(nameWithoutExtension, type)
        }
    }
}
