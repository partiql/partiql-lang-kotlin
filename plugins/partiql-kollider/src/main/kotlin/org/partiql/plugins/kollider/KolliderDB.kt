package org.partiql.plugins.kollider

import org.partiql.plugins.kollider.index.KIndex
import java.nio.file.Path

/**
 * Database simply holds the [KIndex] object.
 */
internal class KolliderDB(
    internal val version: Int,
    internal val index: KIndex,
) {

    companion object {

        @JvmStatic
        fun load(root: Path): KolliderDB {
            // TODO actually read the .partiql file
            val index = KIndex.load(root)
            return KolliderDB(1, index)
        }
    }
}
