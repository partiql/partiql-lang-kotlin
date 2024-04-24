package org.partiql.plugins.fs

import org.partiql.plugins.fs.index.FsIndex
import java.nio.file.Path

/**
 * Database simply holds the [FsIndex] object.
 */
internal class FsDB(
    internal val version: Int,
    internal val index: FsIndex,
) {

    companion object {

        @JvmStatic
        fun load(root: Path): FsDB {
            // TODO actually read the .partiql file
            val index = FsIndex.load(root)
            return FsDB(1, index)
        }
    }
}
