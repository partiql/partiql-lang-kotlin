package com.amazon.howero.fs

import com.amazon.howero.fs.source.IonRecordSource
import com.amazon.howero.fs.source.TextRecordSource
import com.amazon.ion.IonSystem
import com.github.luben.zstd.ZstdInputStream
import org.partiql.spi.Plugin
import org.partiql.spi.RecordSource
import org.partiql.spi.Source
import org.partiql.spi.SourceHandle
import org.partiql.spi.SourceResolver
import org.partiql.spi.Split
import org.partiql.spi.SplitSource
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import java.util.zip.GZIPInputStream
import kotlin.reflect.KClass

class FSPlugin(override val ion: IonSystem) : Plugin {

    private val sources = FSSourceProvider(ion)

    override fun getSplitSource(source: SourceHandle): SplitSource = FSSplitSource(source as FSSourceHandle)

    override fun getRecordSource(split: Split): RecordSource {
        val s = split as FSSplit
        val input = FileInputStream(s.path.toFile())
        return sources.get(input, s.encoding)
    }

    object Sources : SourceResolver() {

        /**
         * Usage: SELECT * FROM fs.glob('glob', 'format/compression');
         */
        @Source
        fun glob(glob: String, encoding: String): SourceHandle = FSSourceHandle("glob:$glob", FsEncoding.parse(encoding))

        /**
         * Usage: SELECT * FROM fs.regex('regex', 'format/compression');
         */
        @Source
        fun regex(regex: String, encoding: String): SourceHandle = FSSourceHandle("regex:$regex", FsEncoding.parse(encoding))
    }

    class Factory : Plugin.Factory {

        override val identifier: String = "fs"

        override val config: KClass<*> = Any::class

        override val sourceResolver: SourceResolver = Sources

        override val scalarLib: Plugin.ScalarLib = FSScalarLib

        override fun create(ion: IonSystem, config: Any?): Plugin = FSPlugin(ion)
    }
}

data class FSSourceHandle(val glob: String, val encoding: FsEncoding) : SourceHandle

data class FSSplit(val path: Path, val encoding: FsEncoding) : Split

class FSSplitSource(private val source: FSSourceHandle) : SplitSource {

    private val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher(source.glob)
    private val filter = BiPredicate<Path, BasicFileAttributes> { path, attr -> attr.isRegularFile && matcher.matches(path) }
    private val paths: Iterator<Path> = Files.find(getRoot(source.glob), 100, filter).iterator()

    override fun hasNext(): Boolean = paths.hasNext()

    override fun next(): Split {
        val path = paths.next()
        return FSSplit(path, source.encoding)
    }

    override fun close() {}

    companion object {

        fun getRoot(glob: String): Path {
            val path = glob.substring(glob.indexOf(":") + 1)
            if (!path.startsWith("/")) {
                throw IllegalArgumentException("glob must be absolute path")
            }
            val chars = path.toCharArray()
            var lastSlashIndex = 0
            var escaped = false
            for (i in chars.indices) {
                val c = chars[i]
                when {
                    c == '/' -> lastSlashIndex = i
                    !escaped && (c == '*' || c == '?' || c == '[') -> break
                    !escaped && c == '\\' -> escaped = true
                    else -> escaped = true
                }
            }
            return Paths.get(path.substring(0, lastSlashIndex))
        }
    }
}

class FSSourceProvider(private val ion: IonSystem) {

    fun get(input: InputStream, encoding: FsEncoding): RecordSource {
        val stream = when (encoding.compression) {
            FsCompression.NONE -> input
            FsCompression.ZSTD -> ZstdInputStream(input)
            FsCompression.GZIP -> GZIPInputStream(input)
        }
        return when (encoding.format) {
            FsFormat.TEXT -> TextRecordSource(ion, stream)
            FsFormat.JSON,
            FsFormat.B10N,
            FsFormat.ION -> IonRecordSource(ion, stream)
        }

    }

}
