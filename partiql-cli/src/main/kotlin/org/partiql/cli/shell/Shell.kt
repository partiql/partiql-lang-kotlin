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

package org.partiql.cli.shell

import com.google.common.util.concurrent.Uninterruptibles
import org.fusesource.jansi.AnsiConsole
import org.jline.reader.EndOfFileException
import org.jline.reader.History
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.jline.utils.InfoCmp
import org.joda.time.Duration
import org.partiql.cli.pipeline.Pipeline
import java.io.Closeable
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.GuardedBy

private const val PROMPT_1 = "PartiQL> "
private const val PROMPT_2 = "   | "
internal const val BAR_1 = "===' "
internal const val BAR_2 = "--- "

/**
 * Commands based upon:
 *  - https://sqlite.org/cli.html
 *  - https://duckdb.org/docs/api/cli/dot_commands
 *  - https://www.postgresql.org/docs/current/app-psql.html
 *
 * Legacy commands
 * ------------------------
 * !list_commands        Prints this message
 * !help                 Prints this message
 * !add_to_global_env    Adds to the global environment key/value pairs of the supplied struct
 * !global_env           Displays the current global environment
 * !add_graph            Adds to the global environment a name and a graph supplied as Ion
 * !add_graph_from_file  Adds to the global environment a name and a graph from an Ion file
 * !history              Prints command history
 * !exit                 Exits the shell
 * !clear                Clears the screen
 */
private const val HELP = """
.cd <path>              Changes the current directory.
.clear                  Clears the screen.
.debug on|off           Toggle debug printing.
.exit                   Exits the shell.
.help                   Prints this message.
.history                Prints command history.
.import <file>          Imports the data in the given file.
.path                   Prints the current search path.
.pwd                    Prints the current directory.
.run <file>             Runs the script.
.set <name> <value>     Sets the shell variable to the given value.
"""

/**
 */

private val EXIT_DELAY: Duration = Duration(3000)

private val SUCCESS: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)
private val ERROR: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
private val INFO: AttributedStyle = AttributedStyle.DEFAULT
private val WARN: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)

private fun ansi(string: String, style: AttributedStyle) = AttributedString(string, style).toAnsi()
internal fun PrintStream.success(string: String) = this.println(ansi(string, SUCCESS))
internal fun PrintStream.error(string: String) = this.println(ansi(string, ERROR))
internal fun PrintStream.info(string: String) = this.println(ansi(string, INFO))
internal fun PrintStream.warn(string: String) = this.println(ansi(string, WARN))

val exiting = AtomicBoolean(false)
val doneCompiling = AtomicBoolean(true)
val donePrinting = AtomicBoolean(true)

internal class Shell(
    private val pipeline: Pipeline,
) {

    private var state: State = State(false)

    private class State(
        @JvmField var debug: Boolean
    )

    private val home: Path = Paths.get(System.getProperty("user.home"))
    private val out = PrintStream(System.out)

    fun start() {
        val interrupter = ThreadInterrupter()
        val exited = CountDownLatch(1)
        // pipelineService.submit(RunnablePipeline(inputs, results, compiler, doneCompiling))
        // printingService.submit(RunnableWriter(out, ConfigurableExprValueFormatter.pretty, values, donePrinting))
        Runtime
            .getRuntime()
            .addShutdownHook(
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

    private val signalHandler = Terminal.SignalHandler { sig ->
        if (sig == Terminal.Signal.INT) {
            exiting.set(true)
        }
    }

    private fun run(exiting: AtomicBoolean) = TerminalBuilder.builder()
        .name("PartiQL")
        .nativeSignals(true)
        .signalHandler(signalHandler)
        .build().use { terminal ->
            val reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(ShellParser)
                .completer(ShellCompleter)
                .option(LineReader.Option.GROUP_PERSIST, true)
                .option(LineReader.Option.AUTO_LIST, true)
                .option(LineReader.Option.CASE_INSENSITIVE, true)
                .variable(LineReader.LIST_MAX, 10)
                .highlighter(ShellHighlighter)
                .variable(LineReader.HISTORY_FILE, home.resolve(".partiql/.history"))
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, PROMPT_2)
                .build()
            // out.info("Typing mode: ${compiler.options.typingMode.name}")
            // out.info("Using version: ${retrievePartiQLVersionAndHash()}")

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

                if (line.startsWith(".")) {
                    // Handle commands, consider an actual arg parsing library
                    val args = line.trim().substring(1).split(" ")
                    if (state.debug) {
                        out.info("argv: [${args.joinToString()}]")
                    }
                    val command = args[0]
                    when (command) {
                        "help" -> {
                            // Print help
                            out.info(HELP)
                        }
                        "history" -> {
                            // Print history
                            for (entry in reader.history) {
                                out.println(entry.pretty())
                            }
                        }
                        "clear" -> {
                            // Clear screen
                            terminal.puts(InfoCmp.Capability.clear_screen)
                            terminal.flush()
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
                        "exit" -> {
                            // Exit
                            System.exit(0)
                        }
                        else -> out.error("Unrecognized command .$command")
                    }
                } else {
                    println("Placeholder: $line")
                    // // Execute PartiQL
                    // executeAndPrint {
                    //     val locals = refreshBindings()
                    //     evaluatePartiQL(line, locals, exiting)
                    // }
                    // waitUntil(true, donePrinting)
                }
            }
            out.println("Thanks for using PartiQL!")
        }

    /** After a command [detectedCommand] has been detected to start the user input,
     * analyze the entire [wholeLine] user input again, expecting to find more input after the command.
     * Returns the extra input or null if none present.  */
    private fun requireInput(wholeLine: String, detectedCommand: String): String? {
        val input = wholeLine.trim().removePrefix(detectedCommand).trim()
        if (input.isEmpty() || input.isBlank()) {
            out.error("Command $detectedCommand requires input.")
            return null
        }
        return input
    }

    private fun requireTokenAndMore(input: String, detectedCommand: String): Pair<String, String>? {
        val trimmed = input.trim()
        val n = trimmed.indexOf(' ')
        if (n == -1) {
            out.error("Command $detectedCommand, after token $trimmed, requires more input.")
            return null
        }
        val token = trimmed.substring(0, n)
        val rest = trimmed.substring(n).trim()
        return Pair(token, rest)
    }

    /**
     * If nothing was caught and execution finished: return null
     * If something was caught: resets service and returns defaultReturn
     */
    private fun catchCancellation(
        doneExecuting: AtomicBoolean,
        cancellationFlag: AtomicBoolean,
        service: ExecutorService,
        addToQueue: () -> Unit,
    ) {
        doneExecuting.set(false)
        addToQueue.invoke()
        while (!doneExecuting.get()) {
            if (exiting.get()) {
                service.shutdown()
                service.shutdownNow()
                cancellationFlag.set(false)
            }
        }
    }

    private fun retrievePartiQLVersionAndHash(): String {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/partiql.properties"))
        return "${properties.getProperty("version")}-${properties.getProperty("commit")}"
    }

    private fun waitUntil(until: Boolean, actual: AtomicBoolean) {
        while (actual.get() != until) {
            // Do nothing
        }
    }

    /**
     * A configuration class representing any configurations specified by the user
     * @param isMonochrome specifies the removal of syntax highlighting
     */
    class ShellConfiguration(val isMonochrome: Boolean = false)
}

/**
 * Pretty print a History.Entry with a gutter for the entry index
 */
private fun History.Entry.pretty(): String {
    val entry = StringBuilder()
    for (line in this.line().lines()) {
        entry.append('\t').append(line).append('\n')
    }
    return AttributedStringBuilder()
        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
        .append(java.lang.String.format("%5d", this.index() + 1))
        .style(AttributedStyle.DEFAULT)
        .append(entry.trimEnd())
        .toAnsi()
}

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
