package org.partiql.cli.io

import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Table
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.io.File

/**
 * A table that lazily loads its data from a file on first access to [getDatum].
 * Only the file path is stored at construction time; the file is read when data is needed.
 */
internal open class LazyTable(
    private val name: Name,
    protected val file: File,
    private val loader: (File) -> Datum,
) : Table {

    @Volatile
    private var cached: Datum? = null

    override fun getName(): Name = name

    // TODO: Infer schema from file metadata without consuming the lazy iterator.
    override fun getSchema(): PType = PType.dynamic()

    override fun getDatum(): Datum {
        if (cached == null) {
            cached = loader(file)
        }
        return cached!!
    }
}
