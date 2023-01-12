/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.cli.pico

import com.amazon.ion.IonSystem
import org.partiql.cli.query.Cli
import org.partiql.cli.shell.Shell
import org.partiql.cli.utils.InputSource
import org.partiql.cli.utils.UnclosableOutputStream
import picocli.CommandLine
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@CommandLine.Command(
    name = "partiql",
    mixinStandardHelpOptions = true,
    versionProvider = PartiQLVersionProvider::class,
    descriptionHeading = "%n@|bold,underline,yellow The PartiQL CLI|@%n",
    description = [
        "%nThe PartiQL CLI allows query execution in two modes: Non-Interactive and Interactive (default).%n",
        "@|bold,underline General Options|@%n",
        "These options configure both Non-Interactive and Interactive executions.%n"
    ],
    showDefaultValues = true
)
internal class PartiQLCommand(private val ion: IonSystem) : Runnable {

    @CommandLine.Mixin
    internal lateinit var options: PipelineOptions

    @CommandLine.ArgGroup(
        exclusive = false,
        heading = "%n@|bold,underline Non-Interactive (Single Query Execution)|@%n%n" +
            "Specifying any of the below options will trigger Non-Interactive execution. " +
            "Also, passing input through standard input will trigger its execution.%n%n"
    )
    internal var executionOptions: ExecutionOptions? = null

    @CommandLine.ArgGroup(exclusive = false, heading = "%n@|bold,underline Interactive (Shell) Configurations|@%n%n")
    internal var shellOptions: ShellOptions? = null

    internal companion object {
        private const val SHEBANG_PREFIX = "#!"
    }

    /**
     * Run the CLI or Shell (default)
     */
    override fun run() {
        val command = executionOptions ?: ExecutionOptions()
        val shell = shellOptions ?: ShellOptions()
        when {
            command.query != null -> runCli(command, command.query!!.inputStream())
            System.console() == null -> runCli(command, System.`in`)
            else -> runShell(shell)
        }
    }

    /**
     * Runs the CLI
     */
    private fun runCli(exec: ExecutionOptions, stream: InputStream) {
        val input = exec.inputFile?.let { InputSource.FileSource(it) }
        val output = when (exec.outputFile) {
            null -> UnclosableOutputStream(System.out)
            else -> FileOutputStream(exec.outputFile!!)
        }
        val query = stream.readBytes().toString(Charsets.UTF_8)
        val queryLines = query.lines()
        val queryWithoutShebang = when (queryLines.firstOrNull()?.startsWith(SHEBANG_PREFIX)) {
            false -> query
            else -> queryLines.subList(1, queryLines.size).joinToString(System.lineSeparator())
        }
        output.use { out ->
            Cli(ion, input, exec.inputFormat, out, exec.outputFormat, options.pipeline, options.environment, queryWithoutShebang, exec.wrapIon).run()
            out.write(System.lineSeparator().toByteArray(Charsets.UTF_8))
        }
    }

    /**
     * Runs the interactive shell
     */
    private fun runShell(shell: ShellOptions = ShellOptions()) {
        val config = Shell.ShellConfiguration(isMonochrome = shell.isMonochrome)
        Shell(System.out, options.pipeline, options.environment, config).start()
    }

    /**
     * Options specific to single query execution
     */
    class ExecutionOptions {
        @CommandLine.Option(names = ["-i", "--in"], description = ["The path to the input file"], paramLabel = "FILE")
        var inputFile: File? = null

        @CommandLine.Option(names = ["--in-format"], description = ["The input file format: [\${COMPLETION-CANDIDATES}]"], paramLabel = "FORMAT")
        var inputFormat: InputFormat = InputFormat.ION

        @CommandLine.Option(names = ["-o", "--out"], description = ["The path to the output file"], paramLabel = "FILE")
        var outputFile: File? = null

        @CommandLine.Option(names = ["--out-format"], description = ["The output file format: [\${COMPLETION-CANDIDATES}]"], paramLabel = "FORMAT")
        var outputFormat: OutputFormat = OutputFormat.PARTIQL

        @CommandLine.Option(names = ["-w", "--wrap-ion"], description = ["Indicates that the input Ion file contains a sequence of Ion values rather than a single Ion collection"])
        var wrapIon: Boolean = false

        @CommandLine.Parameters(arity = "0..1", index = "0..1", description = ["The filepath of the PartiQL query to execute"], paramLabel = "PARTIQL_FILE")
        var query: File? = null
    }

    /**
     * Options specific to the shell
     */
    class ShellOptions {
        @CommandLine.Option(names = ["-m", "--monochrome"], description = ["Specifies that syntax highlighting should not be used"])
        var isMonochrome: Boolean = false
    }

    enum class InputFormat {
        ION,
        PARTIQL
    }

    enum class OutputFormat {
        ION_TEXT,
        ION_BINARY,
        PARTIQL,
        PARTIQL_PRETTY
    }
}
