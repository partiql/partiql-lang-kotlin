/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */
@file:JvmName("Main")

package org.partiql.cli

import org.partiql.cli.io.DatumCsvReader
import org.partiql.cli.io.DatumIonReaderBuilder
import org.partiql.cli.io.DatumWriterTextPretty
import org.partiql.cli.io.Format
import org.partiql.cli.io.LazyCatalog
import org.partiql.cli.pipeline.ErrorMessageFormatter
import org.partiql.cli.pipeline.Pipeline
import org.partiql.cli.shell.Shell
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.InvalidOperationException
import picocli.CommandLine
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.Collections
import java.util.Properties
import kotlin.system.exitProcess

/**
 * Entry-point to the PartiQL command-line utility.
 */
fun main(args: Array<String>) {
    val command = CommandLine(MainCommand())
    val exitCode = command.execute(*args)
    exitProcess(exitCode)
}

/**
 * Reads the version and git hash from the generated properties file.
 */
internal class Version : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/partiql.properties"))
        return Array(1) { "PartiQL ${properties.getProperty("version")}-${properties.getProperty("commit")}" }
    }
}

/**
 * The PartiQL root command.
 */
@CommandLine.Command(
    name = "partiql",
    mixinStandardHelpOptions = true,
    versionProvider = Version::class,
    descriptionHeading = "%n@|bold,underline SYNOPSIS|@%n",
    description = [
        "%nThe PartiQL command-line utility executes queries against the input data (files or stdin).%n",
        "@|bold,underline OPTIONS|@%n",
        "Execute `partiql` without a query or without -i to launch an interactive shell%n",
    ],
    showDefaultValues = true
)
internal class MainCommand : Runnable {
    // TODO: Need to add tests to CLI. All tests were removed in the same commit as this TODO. See Git blame.

    internal companion object {
        private const val SHEBANG_PREFIX = "#!"
    }

    @CommandLine.Option(
        names = ["-d", "--dir"],
        description = ["Path to the database directory. Each file becomes a table (e.g. users.ion -> table 'users')."],
    )
    var dir: File? = null

    @CommandLine.Option(
        names = ["-e", "--env"],
        description = ["File containing the global environment"],
    )
    var env: File? = null

    @CommandLine.Option(
        names = ["--strict"],
        description = ["Execute in strict (type-checking) mode."],
    )
    var strict: Boolean = false

    @CommandLine.Option(
        names = ["--debug"],
        description = ["THIS IS FOR INTERNAL DEVELOPMENT USE ONLY. Shows typing information in the output."],
        hidden = true
    )
    var debug: Boolean = false

    @CommandLine.Option(
        names = ["-f", "--format"],
        description = ["The data format, using the form <input>[:<output>]. Supported input: partiql, ion. Supported output: partiql."],
        paramLabel = "<input[:output]>",
        converter = [Format.Converter::class],
        defaultValue = "partiql",
        showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
    )
    var format: Pair<Format, Format> = Format.PARTIQL to Format.PARTIQL

    @CommandLine.Option(
        names = ["-i", "--include"],
        description = ["An optional PartiQL script."],
    )
    var include: File? = null

    @CommandLine.Option(
        names = ["--max-errors"],
        showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
        defaultValue = "0",
        description = ["The maximum number of errors to report before bailing out. If 0 (the default), there is no limit on the number of error messages produced."],
        paramLabel = "<count>"
    )
    var maxErrors: Int? = null

    @CommandLine.Option(
        names = ["-w"],
        description = ["Inhibits all warning messages."],
    )
    var inhibitWarnings: Boolean = false

    @CommandLine.Option(
        names = ["-Werror"],
        arity = "0..1",
        description = [
            "Make the specified warning into an error. If no code is specified, all warnings are converted to errors.",
            "Codes: \${COMPLETION-CANDIDATES}"
        ],
        paramLabel = "<code>",
        help = true,
        fallbackValue = "ALL",
    )
    var warningsAsErrors: Array<ErrorCodeString>? = null

    @CommandLine.Parameters(
        index = "0",
        arity = "0..1",
        description = ["An optional PartiQL statement string."],
        paramLabel = "'statement'",
        converter = [PairConverter::class],
    )
    var program: Pair<String?, File?>? = null

    @CommandLine.Parameters(
        index = "1..*",
        arity = "0..*",
        description = ["An optional list of files to execute the statement against."],
        hidden = true
    )
    var files: Array<File>? = null

    /**
     * Print a debug message to stderr, prefixed with \[DEBUG].
     */
    private fun debug(msg: String) {
        if (debug) System.err.println("[DEBUG] $msg")
    }

    /**
     * Run the CLI or Shell (default).
     */
    override fun run() {
        if (debug) {
            System.err.println("========================================")
            System.err.println("  PartiQL DEBUG MODE")
            System.err.println("========================================")
            debug("--dir         = $dir")
            debug("--env         = $env")
            debug("--strict      = $strict")
            debug("--debug       = $debug")
            debug("--format      = ${format.first}:${format.second}")
            debug("--include     = $include")
            debug("--max-errors  = $maxErrors")
            debug("-w            = $inhibitWarnings")
            debug("-Werror       = ${warningsAsErrors?.joinToString() ?: "[]"}")
            debug("program       = ${program?.first ?: program?.second?.path ?: "null"}")
            debug("files         = ${files?.joinToString { it.path } ?: "null"}")
            System.err.println("========================================")
        }
        when (val statement = statement()) {
            null -> shell()
            else -> run(statement)
        }
    }

    private fun getPipelineConfig(): Pipeline.Config {
        return Pipeline.Config(maxErrors!!, inhibitWarnings, warningsAsErrors ?: emptyArray())
    }

    /**
     * Returns the query text if present by parsing either the program string or the query file.
     */
    private fun statement(): String? {
        if (program != null && program!!.first != null && include != null) {
            error("Cannot specify both a query file and query string.")
        }
        return program?.first ?: include?.readText()
    }

    private fun shell() {
        val pipeline = pipeline()
        Shell(pipeline, session(), debug).start()
    }

    private fun pipeline(): Pipeline {
        val config = getPipelineConfig()
        return when (strict) {
            true -> Pipeline.strict(System.out, config, debug)
            else -> Pipeline.default(System.out, config, debug)
        }
    }

    private fun run(statement: String) {
        val pipeline = pipeline()
        val program = statement.trimHashBang()
        val session = session()
        val result = try {
            pipeline.execute(program, session)
        } catch (e: Pipeline.PipelineException) {
            e.message?.let { error(it) }
            return
        }

        try {
            val writer = DatumWriterTextPretty(System.out)
            writer.write(result)
        } catch (e: PRuntimeException) {
            val msg = ErrorMessageFormatter.message(e.error)
            error(msg)
        }
        println()
    }

    private fun session() = Session.builder()
        .identity(System.getProperty("user.name"))
        .namespace(emptyList())
        .catalog("default")
        .catalogs(*catalogs().toTypedArray())
        .build()

    /**
     * Produce the connector map for planning and execution.
     */
    private fun catalogs(): List<Catalog> {
        if (dir != null && files != null && files!!.isNotEmpty()) {
            error("Cannot specify both a database directory and a list of files.")
        }
        if (dir != null) {
            return listOf(loadDir(dir!!))
        }

        checkFormat(format)
        if (env != null) {
            return when (format.first) {
                Format.PARTIQL -> listOf(loadEnvPartiQL(env!!))
                Format.ION -> listOf(loadEnvIon(env!!))
                else -> error("Unsupported input format: ${format.first}")
            }
        }

        // Derive a default catalog from stdin (or file streams)
        return when (format.first) {
            Format.PARTIQL -> listOf(loadPartiQLData())
            Format.ION -> listOf(loadIon())
            else -> error("Unsupported input format: ${format.first}")
        }
    }

    /**
     * Load an environment file as PartiQL data.
     *
     * The environment file is expected to be a PartiQL struct literal where each key-value pair
     * corresponds to a new table in the default catalog.
     */
    private fun loadEnvPartiQL(env: File): Catalog {
        try {
            val stream = env.inputStream()
            val pipeline = pipeline()
            val data = stream.bufferedReader(charset("UTF-8")).use { it.readText() }
            val (datum, type) = pipeline.executeWithType(data, Session.empty())
            return envCatalog(datum, type)
        } catch (e: FileNotFoundException) {
            error("The environment file does not exist: ${env.path}")
        } catch (e: IOException) {
            error("Failed to read the environment file: ${e.message}")
        } catch (e: Pipeline.PipelineException) {
            error("Failed to parse the environment file: ${e.message}")
        } catch (e: InvalidOperationException) {
            error("The environment file must contain a struct with table definitions")
        } catch (e: Exception) {
            error("Unexpected error occurs when loading the environment file: ${e.message}")
        }
    }

    /**
     * Load an environment file as Ion data (with PartiQL annotation support).
     *
     * The environment file is expected to be an Ion struct where each key-value pair
     * corresponds to a new table in the default catalog.
     */
    private fun loadEnvIon(env: File): Catalog {
        try {
            val stream = env.inputStream()
            val reader = DatumIonReaderBuilder.standard().build(stream)
            val datum = reader.read()
            return envCatalog(datum)
        } catch (e: FileNotFoundException) {
            error("The environment file does not exist: ${env.path}")
        } catch (e: IOException) {
            error("Failed to read the environment file: ${e.message}")
        } catch (e: InvalidOperationException) {
            error("The environment file must contain a struct with table definitions")
        } catch (e: Exception) {
            error("Unexpected error occurs when loading the environment file: ${e.message}")
        }
    }

    /**
     * Build a catalog from an environment datum (expected to be a struct).
     */
    private fun envCatalog(datum: Datum, inferredType: PType? = null): Catalog {
        val catalogName = "default"
        val schemas = if (inferredType != null && inferredType.code() == PType.ROW) {
            inferredType.fields.associate { it.name to it.type }
        } else {
            emptyMap()
        }
        return Catalog.builder()
            .name(catalogName).apply {
                datum.fields.forEach {
                    define(
                        Table.standard(
                            name = Name.of(it.name),
                            schema = schemas[it.name] ?: it.value.type,
                            datum = it.value
                        )
                    )
                    debug("Loaded table '${it.name}' under catalog '$catalogName'")
                }
            }
            .build().also {
                debug("Finished loading catalog '$catalogName'")
            }
    }

    private fun loadPartiQLData(): Catalog {
        val catalogName = "default"
        val stream = stream()
        val datum = if (stream != null) {
            try {
                val pipeline = pipeline()
                val data = stream.bufferedReader(charset("UTF-8")).use { it.readText() }
                pipeline.execute(data, Session.empty())
            } catch (e: Pipeline.PipelineException) {
                error("Failed to parse input as PartiQL: ${e.message}")
            }
        } else {
            Datum.nullValue()
        }

        return Catalog.builder()
            .name(catalogName)
            .define(
                Table.standard(
                    name = Name.of("stdin"),
                    schema = datum.type,
                    datum = datum,
                )
            )
            .build().also {
                debug("Loaded table 'stdin' under catalog '$catalogName'")
                debug("Finished loading catalog '$catalogName'")
            }
    }

    /**
     * Load Ion data from stdin/files (with PartiQL annotation support).
     */
    private fun loadIon(): Catalog {
        val catalogName = "default"
        val stream = stream()
        val datum = if (stream != null) {
            val reader = DatumIonReaderBuilder.standard().build(stream)
            val values = reader.readAll()
            when (values.size) {
                0 -> Datum.nullValue()
                1 -> values.first()
                else -> Datum.bag(values)
            }
        } else {
            Datum.nullValue()
        }

        return Catalog.builder()
            .name(catalogName)
            .define(
                Table.standard(
                    name = Name.of("stdin"),
                    schema = datum.type,
                    datum = datum,
                )
            )
            .build().also {
                debug("Loaded table 'stdin' under catalog '$catalogName'")
                debug("Finished loading catalog '$catalogName'")
            }
    }

    /**
     * Produce a stream of all input files (or stdin)
     */
    private fun stream(): InputStream? {
        val streams: MutableList<InputStream> = mutableListOf()
        if (program?.second != null) {
            streams.add(program!!.second!!.inputStream())
        }
        if (files != null) {
            streams.addAll(files!!.map { it.inputStream() })
        }
        if (streams.isEmpty() && System.`in`.available() != 0) {
            streams.add(System.`in`)
        }
        return when (streams.size) {
            0 -> null
            1 -> streams.first()
            else -> SequenceInputStream(Collections.enumeration(streams))
        }
    }

    private fun String.trimHashBang(): String {
        val lines = this.lines()
        return when (lines.firstOrNull()?.startsWith(SHEBANG_PREFIX)) {
            false -> this
            else -> lines.subList(1, lines.size).joinToString(System.lineSeparator())
        }
    }

    private class PairConverter : CommandLine.ITypeConverter<Pair<String?, File?>> {

        override fun convert(value: String?): Pair<String?, File?>? {
            if (value == null) {
                return null
            }
            val str = value.trim()
            return if (File(str).exists()) {
                // file path
                (null to File(str))
            } else {
                // statement string
                (str.trim('\'') to (null as File?))
            }
        }
    }

    private fun loadDir(directory: File): Catalog {
        if (!directory.isDirectory) {
            error("Not a directory: ${directory.path}")
        }
        val supportedExtensions = setOf("ion", "pql", "csv", "tsv", "json", "parquet")
        directory.listFiles()
            ?.filter { it.isFile && it.extension !in supportedExtensions }
            ?.forEach { System.err.println("Warning: skipping '${it.name}' (unsupported extension '.${it.extension}'). Supported: $supportedExtensions") }
        return LazyCatalog("default", directory, supportedExtensions) { file ->
            debug("Loading table '${file.nameWithoutExtension}' from ${file.name}")
            when (file.extension) {
                "ion", "json" -> {
                    val reader = DatumIonReaderBuilder.standard().build(file.inputStream())
                    val first = try { reader.read() } catch (_: IOException) { return@LazyCatalog Datum.nullValue() }
                    val second = try { reader.read() } catch (_: IOException) { return@LazyCatalog first }
                    Datum.bag(Iterable {
                        var state = 0
                        object : Iterator<Datum> {
                            private var next: Datum? = null
                            override fun hasNext(): Boolean {
                                if (next != null) return true
                                next = when (state) {
                                    0 -> { state = 1; first }
                                    1 -> { state = 2; second }
                                    else -> try { reader.read() } catch (_: IOException) { null }
                                }
                                return next != null
                            }
                            override fun next(): Datum {
                                if (!hasNext()) throw NoSuchElementException()
                                return next!!.also { next = null }
                            }
                        }
                    })
                }
                "csv" -> DatumCsvReader.read(file.inputStream(), ',')
                "tsv" -> DatumCsvReader.read(file.inputStream(), '\t')
                "pql" -> {
                    val text = file.readText(Charsets.UTF_8)
                    pipeline().execute(text, Session.empty())
                }
                else -> Datum.nullValue()
            }
        }
    }

    private fun checkFormat(format: Pair<Format, Format>) {
        val supportedInput = setOf(Format.PARTIQL, Format.ION)
        val supportedOutput = setOf(Format.PARTIQL)
        if (format.first !in supportedInput) {
            error("Unsupported input format: ${format.first}. Supported: ${supportedInput.joinToString()}")
        }
        if (format.second !in supportedOutput) {
            error("Unsupported output format: ${format.second}. Supported: ${supportedOutput.joinToString()}")
        }
    }
}
