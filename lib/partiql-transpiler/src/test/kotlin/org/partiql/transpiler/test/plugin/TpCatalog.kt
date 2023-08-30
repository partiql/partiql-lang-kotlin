package org.partiql.transpiler.test.plugin

import com.amazon.ionelement.api.loadSingleElement
import org.partiql.spi.BindingPath
import org.partiql.transpiler.test.toStaticType
import org.partiql.types.StaticType
import java.io.File
import java.nio.file.Path

private sealed class TpTree(val name: String) {

    // "Directory" node
    class D(name: String, val children: List<TpTree>) : TpTree(name)

    // Type node
    class T(name: String, val type: StaticType) : TpTree(name)
}

/**
 * Build a memoized catalog tree from local schema definitions.
 */
public class TpCatalog private constructor(private val root: TpTree.D) {

    /**
     * Search the tree for the type.
     */
    public fun lookup(path: BindingPath): TpObject? {
        val match = mutableListOf<String>()
        var curr: TpTree? = root
        for (step in path.steps) {
            if (curr == null) return null
            match.add(curr.name)
            when (curr) {
                is TpTree.T -> break
                is TpTree.D -> curr = curr.children.firstOrNull { step.isEquivalentTo(it.name) }
            }
        }
        // All steps matched and we're at a leaf
        if (match.size == path.steps.size && curr is TpTree.T) {
            match.add(curr.name)
            return TpObject(match, curr.type)
        }
        return null
    }

    companion object {

        /**
         * Builds a TpTree from the given root.
         */
        public fun load(root: Path): TpCatalog = TpCatalog(root.toFile().tree() as TpTree.D)

        private fun File.tree(): TpTree = when (this.isDirectory) {
            true -> d()
            else -> t()
        }

        private fun File.d(): TpTree.D {
            val children = listFiles()!!.map { it.tree() }
            return TpTree.D(name, children)
        }

        private fun File.t(): TpTree.T {
            val text = readText()
            val ion = loadSingleElement(text)
            val type = ion.toStaticType()
            return TpTree.T(nameWithoutExtension, type)
        }
    }
}
