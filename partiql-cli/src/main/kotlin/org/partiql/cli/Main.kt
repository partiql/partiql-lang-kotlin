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
import picocli.CommandLine
import java.io.File
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
    var strict: Boolean? = false

    @CommandLine.Option(
        names = ["-f", "--format"],
        description = ["The data format, using the form <input>[:<output>]."],
        paramLabel = "<input[:output]>",
        converter = [Format.Converter::class],
    )
    var format: Pair<Format, Format>? = null

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
     * Run the CLI or Shell (default)
     */
    override fun run() {
        val statement: String? = statement()
        println(statement)
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

    // /**
    //  * Runs the CLI
    //  */
    // private fun runCli(exec: ExecutionOptions, stream: InputStream) {
    //     val input = when (exec.inputFile) {
    //         null -> EmptyInputStream()
    //         else -> FileInputStream(exec.inputFile!!)
    //     }
    //     val output = when (exec.outputFile) {
    //         null -> UnclosableOutputStream(System.out)
    //         else -> FileOutputStream(exec.outputFile!!)
    //     }
    //     val query = stream.readBytes().toString(Charsets.UTF_8)
    //     val queryLines = query.lines()
    //     val queryWithoutShebang = when (queryLines.firstOrNull()?.startsWith(SHEBANG_PREFIX)) {
    //         false -> query
    //         else -> queryLines.subList(1, queryLines.size).joinToString(System.lineSeparator())
    //     }
    //     input.use { src ->
    //         output.use { out ->
    //             Cli(
    //                 ion,
    //                 src,
    //                 exec.inputFormat,
    //                 out,
    //                 exec.outputFormat,
    //                 options.pipeline,
    //                 options.environment,
    //                 queryWithoutShebang,
    //                 exec.wrapIon
    //             ).run()
    //             out.write(System.lineSeparator().toByteArray(Charsets.UTF_8))
    //         }
    //     }
    // }
    //
    // /**
    //  * Runs the interactive shell
    //  */
    // private fun runShell(shell: ShellOptions = ShellOptions()) {
    //     val config = Shell.ShellConfiguration(isMonochrome = shell.isMonochrome)
    //     Shell(System.out, options.pipeline, options.environment, config).start()
    // }

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
