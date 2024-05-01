package org.partiql.plugins.fs

import org.partiql.plugins.fs.index.FsIndex
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchKey
import java.nio.file.attribute.BasicFileAttributes

/**
 * Database simply holds the [FsIndex] object.
 */
internal class FsDB internal constructor(
    internal val version: Int,
    internal val root: Path,
    internal var index: FsIndex,
) : Runnable {

    val watcher = FileSystems.getDefault().newWatchService()

    val keys: MutableMap<WatchKey, Path> = mutableMapOf()
    companion object {

        @JvmStatic
        fun load(root: Path): FsDB {
            // TODO actually read the .partiql file
            val index = FsIndex.load(root)
            return FsDB(1, root, index)
        }
    }

    /**
     * Register the given directory with the WatchService
     */
    @Throws(IOException::class)
    private fun register(dir: Path) {
        val key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        keys[key] = dir
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private fun registerAll(start: Path) {
        // register directory and sub-directories
        Files.walkFileTree(
            start,
            object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    register(dir)
                    return FileVisitResult.CONTINUE
                }
            }
        )
    }

    override fun run() {
        registerAll(root)
        while (true) {
            val k: WatchKey = watcher.take()
            for (e in k.pollEvents()) {
                this.index = FsIndex.load(root)
//                throw RuntimeException("update completed")
            }
            k.reset()
        }
    }
}
