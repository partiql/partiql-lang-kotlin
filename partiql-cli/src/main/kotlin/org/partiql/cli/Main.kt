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

import org.partiql.cli.io.Format
import org.partiql.cli.pipeline.ErrorMessageFormatter
import org.partiql.cli.pipeline.Pipeline
import org.partiql.cli.shell.Shell
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumReader
import org.partiql.spi.value.ValueUtils
import org.partiql.spi.value.io.PartiQLValueTextWriter
import picocli.CommandLine
import java.io.File
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
        description = ["Path to the database directory"],
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
        description = ["The data format, using the form <input>[:<output>]."],
        paramLabel = "<input[:output]>",
        converter = [Format.Converter::class],
    )
    lateinit var format: Pair<Format, Format>

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
    lateinit var warningsAsErrors: Array<ErrorCodeString>

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
    )
    var files: Array<File>? = null

    /**
     * Run the CLI or Shell (default).
     */
    override fun run() {
        when (val statement = statement()) {
            null -> shell()
            else -> run(statement)
        }
    }

    private fun getPipelineConfig(): Pipeline.Config {
        warningsAsErrors = if (this::warningsAsErrors.isInitialized) warningsAsErrors else emptyArray()
        return Pipeline.Config(maxErrors!!, inhibitWarnings, warningsAsErrors)
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
        val config = getPipelineConfig()
        val pipeline = when (strict) {
            true -> Pipeline.strict(System.out, config)
            else -> Pipeline.default(System.out, config)
        }
        Shell(pipeline, session(), debug).start()
    }

    private fun run(statement: String) {
        val config = getPipelineConfig()
        val pipeline = when (strict) {
            true -> Pipeline.strict(System.out, config)
            else -> Pipeline.default(System.out, config)
        }
        val program = statement.trimHashBang()
        val session = session()
        val result = try {
            pipeline.execute(program, session)
        } catch (e: Pipeline.PipelineException) {
            e.message?.let { error(it) }
            return
        }

        // TODO add format support
        checkFormat(format)
        try {
            val writer = PartiQLValueTextWriter(System.out)
            val p = ValueUtils.newPartiQLValue(result)
            writer.append(p) // TODO: Create a Datum writer
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
            TODO("Local directory plugin not implemented")
            // var root = dir!!
            // val connector = LocalPlugin.create(root.toPath())
            // return mapOf("default" to connector)
        }

        if(env != null){
            return listOf(parsePartiQL(env!!))
        }

        // Derive a `default catalog from stdin (or file streams)
        return listOf(parseIon())
    }

    private fun parsePartiQL(env: File): Catalog {
        val stream = env.inputStream()
        val compiler = PartiQLCompiler.standard()
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val data = stream.bufferedReader(charset("UTF-8")).use { it.readText() }
        val parseResult = parser.parse(data)
        val statement = parseResult.statements[0]
        val plan = planner.plan(statement, Session.empty()).plan
        val datum = compiler.prepare(plan, Mode.PERMISSIVE()).execute()
        val catalog = Catalog.builder()
            .name("default")
            .define(
                Table.standard(
                    name = Name.of("stdin"),
                    schema = datum.type,
                    datum = datum,
                )
            )
            .build()
        return catalog
    }

    private fun parseIon(): Catalog {
        val stream = stream()
        val datum = if (stream != null) {
            val reader = DatumReader.ion(stream)
            val values = reader.readAll()
            when (values.size) {
                0 -> Datum.nullValue()
                1 -> values.first()
                else -> Datum.bag(values)
            }
        } else {
            Datum.nullValue()
        }

        val catalog = Catalog.builder()
            .name("default")
            .define(
                Table.standard(
                    name = Name.of("stdin"),
                    schema = datum.type,
                    datum = datum,
                )
            )
            .build()
        return catalog
    }

    /**
     * Consider making "readAll" a static method of DatumReader.
     *
     */
    private fun DatumReader.readAll(): List<Datum> {
        val values = mutableListOf<Datum>()
        val next = next()
        while (next != null) {
            values.add(next)
        }
        return values
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

    /**
     * TODO support additional input/output formats.
     */
    private fun checkFormat(format: Pair<Format, Format>) {
        if (format.first != Format.ION) {
            error("Unsupported input format: ${format.first}")
        }
        if (format.second != Format.ION) {
            error("Unsupported output format: ${format.second}")
        }
    }
}
