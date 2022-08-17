package com.amazon.howero.fs

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import org.partiql.spi.Plugin
import org.partiql.spi.RecordSource
import org.partiql.spi.Source
import org.partiql.spi.SourceEncoding
import org.partiql.spi.SourceHandle
import org.partiql.spi.SourceResolver
import org.partiql.spi.Split
import org.partiql.spi.SplitSource
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import kotlin.reflect.KClass

class FSPlugin(override val ion: IonSystem) : Plugin {

    override fun getSplitSource(source: SourceHandle): SplitSource = FSSplitSource(source as FSSourceHandle)

    override fun getRecordSource(split: Split): RecordSource = object : RecordSource {

        override fun get(): Sequence<IonValue> {
            val s = split as FSSplit
            return sequence {
                val record = ion.newEmptyStruct()
                record.add("path", ion.newString(s.path.toString()))
                val encoding = ion.newEmptyStruct()
                encoding.add("format", ion.newString(s.encoding.format.name))
                encoding.add("compression", ion.newString(s.encoding.compression.name))
                record.add("encoding", encoding)
                yield(record)
            }
        }

    }

    object Sources : SourceResolver() {

        /**
         * Usage: SELECT * FROM fs.glob('glob', 'format/compression');
         */
        @Source
        fun glob(glob: String, encoding: String): SourceHandle = FSSourceHandle("glob:$glob", SourceEncoding.parse(encoding))

        /**
         * Usage: SELECT * FROM fs.regex('regex', 'format/compression');
         */
        @Source
        fun regex(regex: String, encoding: String): SourceHandle = FSSourceHandle("regex:$regex", SourceEncoding.parse(encoding))
    }

    class Factory : Plugin.Factory {

        override val identifier: String = "fs"

        override val config: KClass<*> = Any::class

        override val sourceResolver: SourceResolver = Sources

        override fun create(ion: IonSystem, config: Any?): Plugin = FSPlugin(ion)
    }
}

data class FSSourceHandle(val glob: String, val encoding: SourceEncoding) : SourceHandle

data class FSSplit(val path: Path, val encoding: SourceEncoding) : Split

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
