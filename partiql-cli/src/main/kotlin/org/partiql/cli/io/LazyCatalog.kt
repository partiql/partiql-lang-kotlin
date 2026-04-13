package org.partiql.cli.io

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum
import java.io.File

/**
 * A catalog backed by a directory of data files. The directory is scanned on each
 * [getTable]/[resolveTable] call so that file renames, additions, and deletions
 * are picked up without restarting the CLI.
 *
 * @param name the catalog name
 * @param directory the directory containing data files
 * @param supportedExtensions the file extensions to recognize as tables
 * @param loader a function that reads a file into a [Datum]
 */
internal class LazyCatalog(
    private val name: String,
    private val directory: File,
    private val supportedExtensions: Set<String>,
    private val loader: (File) -> Datum,
) : Catalog {

    override fun getName(): String = name

    override fun getTable(session: Session, name: Name): Table? {
        val file = findFile(name.getName()) ?: return null
        return createTable(name, file)
    }

    override fun resolveTable(session: Session, identifier: Identifier): Name? {
        val target = identifier.first()
        val file = directory.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            ?.firstOrNull { target.matches(it.nameWithoutExtension) }
        if (file != null) return Name.of(file.nameWithoutExtension)
        // Check if there's a file with a matching name but unsupported extension
        val unsupported = directory.listFiles()
            ?.firstOrNull {
                it.isFile && it.extension.lowercase() !in supportedExtensions &&
                    target.matches(it.nameWithoutExtension)
            }
        if (unsupported != null) {
            System.err.println("Warning: '${unsupported.name}' has unsupported extension '.${unsupported.extension}'. Supported: $supportedExtensions")
        }
        return null
    }

    private fun findFile(tableName: String): File? {
        return directory.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            ?.firstOrNull { it.nameWithoutExtension.equals(tableName, ignoreCase = true) }
    }

    private fun createTable(name: Name, file: File): Table = when (file.extension.lowercase()) {
        "parquet" -> ParquetTable(name, file)
        else -> LazyTable(name, file, loader)
    }
}
