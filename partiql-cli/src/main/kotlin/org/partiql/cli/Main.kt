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

import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.loadAllElements
import org.partiql.cli.io.Format
import org.partiql.cli.pipeline.Pipeline
import org.partiql.cli.shell.Shell
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.plugins.fs.FsPlugin
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.toIon
import picocli.CommandLine
import java.io.File
import java.io.InputStream
import java.io.SequenceInputStream
import java.time.Instant
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
internal class MainCommand() : Runnable {
    var shell: Shell? = null
    internal companion object {
        private const val SHEBANG_PREFIX = "#!"
    }

    @CommandLine.Option(
        names = ["-d", "--dir"],
        description = ["Path to the database directory"],
    )
    var dir: File? = null

    @CommandLine.Option(
        names = ["--strict"],
        description = ["Execute in strict (type-checking) mode."],
    )
    var strict: Boolean = false

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
        val pipeline = when (strict) {
            true -> Pipeline.strict()
            else -> Pipeline.default()
        }
        shell = Shell(pipeline, session())

        shell!!.start()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun run(statement: String) {
        val pipeline = when (strict) {
            true -> Pipeline.strict()
            else -> Pipeline.default()
        }
        val program = statement.trimHashBang()

        val session = session()
        val result = pipeline.execute(program, session)
        when (result) {
            is PartiQLResult.Error -> {
                error(result.cause.stackTrace)
            }
            is PartiQLResult.Value -> {
                // TODO handle output format
                val ion = result.value.toIon()
                val writer = IonTextWriterBuilder.pretty().build(System.out as Appendable)
                ion.writeTo(writer)
                println()
            }
        }
    }

    private fun session() = Pipeline.Session(
        queryId = "cli",
        userId = System.getProperty("user.name"),
        currentCatalog = "default",
        currentDirectory = emptyList(),
        connectors = connectors(),
        instant = Instant.now(),
        debug = false,
        mode = when (strict) {
            true -> PartiQLEngine.Mode.STRICT
            else -> PartiQLEngine.Mode.PERMISSIVE
        }
    )

    /**
     * Produce the connector map for planning and execution.
     */
    private fun connectors(): Map<String, Connector> {
        if (dir != null && files != null && files!!.isNotEmpty()) {
            error("Cannot specify both a database directory and a list of files.")
        }
        // Hack in jdbc for demo
        if (dir != null) {
            var root = dir!!
            val connector = FsPlugin.create(root.toPath())
            return mapOf("default" to connector)
        }
        // Derive a `default catalog from stdin (or file streams)
        val stream = stream()
        val value = if (stream != null) {
            val reader = IonReaderBuilder.standard().build(stream)
            val values = loadAllElements(reader).toList()
            when (values.size) {
                0 -> ionNull()
                1 -> values.first()
                else -> ionListOf(values)
            }
        } else {
            ionNull()
        }
        val catalog = MemoryCatalog.builder()
            .name("default")
            .info(InfoSchema.ext())
            .define(
                name = "stdin",
                type = StaticType.ANY,
                value = value,
            )
            .build()
        return mapOf(
            "default" to MemoryConnector(catalog)
        )
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
}
