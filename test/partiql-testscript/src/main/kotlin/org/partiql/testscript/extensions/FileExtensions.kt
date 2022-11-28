package org.partiql.testscript.extensions

import java.io.File
import java.io.FileFilter
import java.lang.IllegalArgumentException

internal object ptsFileFilter : FileFilter {
    override fun accept(pathname: File): Boolean {
        return (pathname.isFile && pathname.name.endsWith(".sqlts")) || pathname.isDirectory
    }
}

internal fun File.listRecursive(filter: FileFilter = FileFilter { true }): List<File> = when {
    !this.exists() -> throw IllegalArgumentException("'${this.path}' not found")
    this.isDirectory -> this.listFiles(filter).flatMap { f -> f.listRecursive(filter) }
    this.isFile -> listOf(this)
    else -> throw IllegalArgumentException("couldn't read '${this.path}'. It's neither a file nor a directory")
}
