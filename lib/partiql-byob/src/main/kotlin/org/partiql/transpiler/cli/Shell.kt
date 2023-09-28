/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.transpiler.cli

import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.google.common.util.concurrent.Uninterruptibles
import org.fusesource.jansi.AnsiConsole
import org.jline.reader.EndOfFileException
import org.jline.reader.History
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.jline.utils.AttributedStyle.BOLD
import org.jline.utils.InfoCmp
import org.joda.time.Duration
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.test.plugin.FsConnector
import org.partiql.planner.test.plugin.FsPlugin
import org.partiql.planner.test.toIon
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.transpiler.PartiQLTranspiler
import org.partiql.transpiler.TpTarget
import org.partiql.transpiler.TranspilerProblem
import org.partiql.transpiler.targets.partiql.PartiQLTarget
import org.partiql.transpiler.targets.redshift.RedshiftTarget
import org.partiql.transpiler.targets.trino.TrinoTarget
import java.io.Closeable
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.GuardedBy

private val PROMPT_1 =
    AttributedStringBuilder().styled(BOLD.foreground(AttributedStyle.MAGENTA), "pql").append(" ▶ ").toAnsi()
private const val PROMPT_2 = "    | "
private const val BAR_1 = "===' "
private const val BAR_2 = "--- "
private const val WELCOME_MSG = """    

 ____ ____ ____ ____ 
||B |||Y |||O |||B ||
||__|||__|||__|||__||
|/__\|/__\|/__\|/__\|

"""

private const val HELP = """
\h                  Print this message
\s                  Print command history
\q                  Disconnect
\d                  Describe catalog
\dt <table>         Describe table
\debug on|off       Toggle debug printing
\path               Get/Set the schema path (dot delimited)
\t [<target>]       Get/Set the transpiler target
\clear              Clear screen
"""

private val SUCCESS: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)
private val ERROR: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
private val INFO: AttributedStyle = AttributedStyle.DEFAULT
private val WARN: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)

private val EXIT_DELAY: Duration = Duration(3000)

private class ShellState {
    public var target: TpTarget<*> = PartiQLTarget
    public var path: List<String> = listOf()
    public var debug: Boolean = false
}

internal class Shell(
    private val plugin: FsPlugin,
    private val catalog: String,
) {

    // Always use `fs`
    private val catalogConfig = mapOf(
        catalog to ionStructOf(
            field("connector_name", ionString("fs")),
        ),
    )

    private val output = System.out
    private val homeDir: Path = Paths.get(System.getProperty("user.home"))
    private val out = PrintStream(output)
    private val currentUser = System.getProperty("user.name")

    private val state = ShellState()
    private val exiting = AtomicBoolean()

    // our frenemy
    private val transpiler = PartiQLTranspiler(listOf(plugin))

    // for \d commands
    private val connector = plugin.getConnectorFactories()
        .first()
        .create(catalog, catalogConfig[catalog]!!) as FsConnector

    // dummy, doesn't matter
    private val connectorSession = object : ConnectorSession {
        override fun getQueryId(): String = ""
        override fun getUserId(): String = ""
    }

    fun start() {
        val interrupter = ThreadInterrupter()
        val exited = CountDownLatch(1)
        Runtime.getRuntime().addShutdownHook(
            Thread {
                exiting.set(true)
                interrupter.interrupt()
                Uninterruptibles.awaitUninterruptibly(exited, EXIT_DELAY.millis, TimeUnit.MILLISECONDS)
            }
        )
        try {
            AnsiConsole.systemInstall()
            run(exiting)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        } finally {
            exited.countDown()
            interrupter.close()
            AnsiConsole.systemUninstall()
        }
    }

    private fun run(exiting: AtomicBoolean) = TerminalBuilder.builder().build().use { terminal ->
        val highlighter = ShellHighlighter()
        val reader = LineReaderBuilder.builder().terminal(terminal).parser(ShellParser)
            .option(LineReader.Option.GROUP_PERSIST, true).option(LineReader.Option.AUTO_LIST, true)
            .option(LineReader.Option.CASE_INSENSITIVE, true).variable(LineReader.LIST_MAX, 10).highlighter(highlighter)
            .variable(LineReader.HISTORY_FILE, homeDir.resolve(".partiql/.byob-history"))
            .variable(LineReader.SECONDARY_PROMPT_PATTERN, PROMPT_2).build()
        out.info(WELCOME_MSG)

        // !! MAIN LOOP !!
        while (!exiting.get()) {
            val line: String = try {
                reader.readLine(PROMPT_1)
            } catch (ex: UserInterruptException) {
                if (ex.partialLine.isNotEmpty()) {
                    reader.history.add(ex.partialLine)
                }
                continue
            } catch (ex: EndOfFileException) {
                out.info("^D")
                return
            }

            if (line.isBlank()) {
                out.success("OK!")
                continue
            }

            if (line.startsWith("\\")) {
                // Handle commands, consider an actual arg parsing library
                val args = line.trim().substring(1).split(" ")
                if (state.debug) {
                    out.info("argv: [${args.joinToString()}]")
                }
                val command = args[0]
                when (command) {
                    "h" -> {
                        // Print help
                        out.info(HELP)
                    }
                    "s" -> {
                        // Print history
                        for (entry in reader.history) {
                            out.println(entry.pretty())
                        }
                    }
                    "q" -> return
                    "d" -> {
                        // Describe Catalog
                        out.println()
                        out.info("Catalog: $catalog")
                        out.info("-------------------------")
                        val objects = connector.listObjects()
                        for (obj in objects) {
                            out.info(obj.sql())
                        }
                        out.println()
                    }
                    "dt" -> {
                        // Describe Table
                        val arg1 = args.getOrNull(1)
                        if (arg1 == null) {
                            out.error("Expected <table> argument")
                            continue
                        }
                        val metadata = connector.getMetadata(connectorSession)
                        val path = arg1.toBindingPath()
                        val handle = metadata.getObjectHandle(connectorSession, path)
                        if (handle == null) {
                            out.error("Did not find table `$arg1`")
                            continue
                        }
                        val type = metadata.getObjectType(connectorSession, handle)
                        if (type == null) {
                            out.error("Did not find type for table `$arg1`")
                            continue
                        }
                        val typeE = type.toIon()
                        val writer = IonTextWriterBuilder.pretty().build(out as Appendable)
                        typeE.writeTo(writer)
                        out.appendLine()
                    }
                    "debug" -> {
                        // Toggle debug printing
                        val arg1 = args.getOrNull(1)
                        if (arg1 == null) {
                            out.info("debug: ${state.debug}")
                            continue
                        }
                        when (arg1) {
                            "on" -> state.debug = true
                            "off" -> state.debug = false
                            else -> out.error("Expected on|off")
                        }
                    }
                    "t" -> {
                        // Get/Set Target
                        val arg1 = args.getOrNull(1)
                        if (arg1 == null) {
                            out.info("Target: ${state.target.target}")
                            out.info("Version: ${state.target.version}")
                            out.println()
                        } else {
                            setTarget(arg1)
                        }
                    }
                    "clear" -> {
                        // Clear screen
                        terminal.puts(InfoCmp.Capability.clear_screen)
                        terminal.flush()
                    }
                    else -> out.error("Unrecognized command \\$command")
                }
            } else {
                // Transpile input to desired target
                transpile(line)
            }
        }
    }

    /**
     * Split on ".", prefix with schema path
     */
    private fun String.toBindingPath(): BindingPath {
        val steps = (state.path + this.split(".")).map {
            BindingName(it, BindingCase.SENSITIVE)
        }
        return BindingPath(steps)
    }

    private fun setTarget(targetArg: String) {
        val target = when (targetArg) {
            "partiql" -> PartiQLTarget
            "redshift" -> RedshiftTarget
            "trino" -> TrinoTarget
            else -> {
                out.error("Unknown target `$targetArg`")
                return
            }
        }
        state.target = target
        out.info("Set target to `$targetArg`")
    }

    private fun transpile(input: String) {
        val session = PartiQLPlanner.Session(
            queryId = "q__byob",
            userId = currentUser ?: "byob",
            currentCatalog = catalog,
            currentDirectory = state.path,
            catalogConfig = catalogConfig,
        )
        try {
            val result = transpiler.transpile(input, state.target, session)
            out.info("==============================")
            out.info("Transpilation Output:")
            out.info(result.output.value.toString())
            out.println()
            out.info("==============================")
            out.info("Output Schema:")
            val outputSchema = java.lang.StringBuilder()
            val ionWriter = IonTextWriterBuilder.minimal().withPrettyPrinting().build(outputSchema)
            result.output.schema.toIon().writeTo(ionWriter)
            out.info(outputSchema.toString())
            out.println()
            if (result.problems.isNotEmpty()) {
                out.warn("--- HINTS --------------")
                for (problem in result.problems) {
                    val message = problem.toString()
                    when (problem.level) {
                        TranspilerProblem.Level.INFO -> out.info(message)
                        TranspilerProblem.Level.WARNING -> out.warn(message)
                        TranspilerProblem.Level.ERROR -> out.error(message)
                    }
                }
                out.println()
            }
            if (state.debug) {
                out.info("----- DEBUG -----------")
                out.info(result.output.toDebugString())
            }
            out.println()
        } catch (ex: Exception) {
            out.error(ex.stackTraceToString())
        }
    }
}

/**
 * Pretty print a History.Entry with a gutter for the entry index
 */
private fun History.Entry.pretty(): String {
    val entry = StringBuilder()
    for (line in this.line().lines()) {
        entry.append('\t').append(line).append('\n')
    }
    return AttributedStringBuilder().style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
        .append(java.lang.String.format("%5d", this.index() + 1)).style(AttributedStyle.DEFAULT).append(entry.trimEnd())
        .toAnsi()
}

private fun ansi(string: String, style: AttributedStyle) = AttributedString(string, style).toAnsi()

public fun PrintStream.success(string: String) = this.println(ansi(string, SUCCESS))

public fun PrintStream.error(string: String) = this.println(ansi(string, ERROR))

public fun PrintStream.info(string: String) = this.println(ansi(string, INFO))

public fun PrintStream.warn(string: String) = this.println(ansi(string, WARN))

private class ThreadInterrupter : Closeable {
    private val thread = Thread.currentThread()

    @GuardedBy("this")
    private var processing = true

    @Synchronized
    fun interrupt() {
        if (processing) {
            thread.interrupt()
        }
    }

    @Synchronized
    override fun close() {
        processing = false
        Thread.interrupted()
    }
}

private fun BindingPath.sql() = steps.joinToString(".") { it.sql() }

private fun BindingName.sql() = when (bindingCase) {
    BindingCase.SENSITIVE -> "\"$name\""
    BindingCase.INSENSITIVE -> name
}
