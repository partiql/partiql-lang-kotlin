package org.partiql.cli.io

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum
import java.io.File

/**
 * A catalog backed by a directory of data files. Table metadata (names) is loaded eagerly
 * at construction time, but file contents are loaded lazily on first access.
 *
 * @param name the catalog name
 * @param directory the directory containing data files
 * @param supportedExtensions the file extensions to recognize as tables
 * @param loader a function that reads a file into a [Datum]
 */
internal class LazyCatalog(
    private val name: String,
    directory: File,
    supportedExtensions: Set<String>,
    loader: (File) -> Datum,
) : Catalog {

    private val tables: Map<Name, Table>

    init {
        val files = directory.listFiles()?.filter { it.isFile && it.extension in supportedExtensions } ?: emptyList()
        tables = files.associate { file ->
            val tableName = Name.of(file.nameWithoutExtension)
            tableName to when (file.extension) {
                "parquet" -> ParquetTable(tableName, file)
                else -> LazyTable(tableName, file, loader)
            }
        }
    }

    override fun getName(): String = name

    override fun getTable(session: Session, name: Name): Table? = tables[name]

    override fun resolveTable(session: Session, identifier: Identifier): Name? {
        val first = identifier.first()
        for ((name, _) in tables) {
            if (first.matches(name.getName())) {
                return name
            }
        }
        return null
    }
}
