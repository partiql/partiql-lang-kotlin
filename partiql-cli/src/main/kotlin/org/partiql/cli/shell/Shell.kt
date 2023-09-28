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

import com.google.common.base.CharMatcher
import com.google.common.util.concurrent.Uninterruptibles
import org.fusesource.jansi.AnsiConsole
import org.jline.reader.EndOfFileException
import org.jline.reader.History
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.jline.utils.InfoCmp
import org.joda.time.Duration
import org.partiql.cli.format.ExplainFormatter
import org.partiql.cli.pipeline.AbstractPipeline
import org.partiql.lang.SqlException
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.delegate
import org.partiql.lang.eval.namedValue
import org.partiql.lang.graph.ExternalGraphException
import org.partiql.lang.graph.ExternalGraphReader
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.lang.util.ConfigurableExprValueFormatter
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.Properties
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.GuardedBy

private const val PROMPT_1 = "PartiQL> "
private const val PROMPT_2 = "   | "
internal const val BAR_1 = "===' "
internal const val BAR_2 = "--- "
private const val WELCOME_MSG = "Welcome to the PartiQL shell!"
private const val DEBUG_MSG = """    
■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■

    ██████╗ ███████╗██████╗ ██╗   ██╗ ██████╗ 
    ██╔══██╗██╔════╝██╔══██╗██║   ██║██╔════╝ 
    ██║  ██║█████╗  ██████╔╝██║   ██║██║  ███╗
    ██║  ██║██╔══╝  ██╔══██╗██║   ██║██║   ██║
    ██████╔╝███████╗██████╔╝╚██████╔╝╚██████╔╝
    ╚═════╝ ╚══════╝╚═════╝  ╚═════╝  ╚═════╝ 
    
■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
"""

private const val HELP = """
!list_commands        Prints this message
!help                 Prints this message
!add_to_global_env    Adds to the global environment key/value pairs of the supplied struct
!global_env           Displays the current global environment
!add_graph            Adds to the global environment a name and a graph supplied as Ion
!add_graph_from_file  Adds to the global environment a name and a graph from an Ion file
!history              Prints command history
!exit                 Exits the shell
!clear                Clears the screen
"""

private val SUCCESS: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)
private val ERROR: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
private val INFO: AttributedStyle = AttributedStyle.DEFAULT
private val WARN: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)

private val EXIT_DELAY: Duration = Duration(3000)

/**
 * Initial work to replace the REPL with JLine3. I have attempted to keep this similar to Repl.kt, but have some
 * opinions on ways to clean this up in later PRs.
 */

val exiting = AtomicBoolean(false)
val doneCompiling = AtomicBoolean(false)
val donePrinting = AtomicBoolean(false)

internal class Shell(
    output: OutputStream,
    private val compiler: AbstractPipeline,
    initialGlobal: Bindings<ExprValue>,
    private val config: ShellConfiguration = ShellConfiguration()
) {
    private val homeDir: Path = Paths.get(System.getProperty("user.home"))
    private val globals = ShellGlobalBinding().add(initialGlobal)
    private var previousResult = ExprValue.nullValue
    private val out = PrintStream(output)
    private val currentUser = System.getProperty("user.name")

    private val inputs: BlockingQueue<RunnablePipeline.Input> = ArrayBlockingQueue(1)
    private val results: BlockingQueue<PartiQLResult> = ArrayBlockingQueue(1)
    private var pipelineService: ExecutorService = Executors.newFixedThreadPool(1)
    private val values: BlockingQueue<ExprValue> = ArrayBlockingQueue(1)
    private var printingService: ExecutorService = Executors.newFixedThreadPool(1)

    fun start() {
        val interrupter = ThreadInterrupter()
        val exited = CountDownLatch(1)
        pipelineService.submit(RunnablePipeline(inputs, results, compiler, doneCompiling))
        printingService.submit(RunnableWriter(out, ConfigurableExprValueFormatter.pretty, values, donePrinting))
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

            val highlighter = when {
                this.config.isMonochrome -> null
                else -> ShellHighlighter()
            }
            val completer = AggregateCompleter(CompleterDefault())
            val reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(ShellParser)
                .completer(completer)
                .option(LineReader.Option.GROUP_PERSIST, true)
                .option(LineReader.Option.AUTO_LIST, true)
                .option(LineReader.Option.CASE_INSENSITIVE, true)
                .variable(LineReader.LIST_MAX, 10)
                .highlighter(highlighter)
                .expander(ShellExpander)
                .variable(LineReader.HISTORY_FILE, homeDir.resolve(".partiql/.history"))
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, PROMPT_2)
                .build()

            out.info(WELCOME_MSG)
            out.info("Typing mode: ${compiler.options.typingMode.name}")
            out.info("Using version: ${retrievePartiQLVersionAndHash()}")
            if (compiler is AbstractPipeline.PipelineDebug) {
                out.println("\n\n")
                out.success(DEBUG_MSG)
                out.println("\n\n")
            }

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

                // Pretty print AST
                if (line.endsWith("\n!!")) {
                    printAST(line.removeSuffix("!!"))
                    continue
                }

                if (line.isBlank()) {
                    out.success("OK!")
                    continue
                }

                // Handle commands
                val command = when (val end: Int = CharMatcher.`is`(';').or(CharMatcher.whitespace()).indexIn(line)) {
                    -1 -> ""
                    else -> line.substring(0, end)
                }.lowercase(Locale.ENGLISH).trim()
                when (command) {
                    "!exit" -> return
                    "!add_to_global_env" -> {
                        // Consider PicoCLI + Jline, but it doesn't easily place nice with commands + raw SQL
                        // https://github.com/partiql/partiql-lang-kotlin/issues/63
                        val arg = requireInput(line, command) ?: continue
                        executeAndPrint {
                            val locals = refreshBindings()
                            val result = evaluatePartiQL(
                                arg,
                                locals,
                                exiting
                            ) as PartiQLResult.Value
                            globals.add(result.value.bindings)
                            result
                        }
                        continue
                    }
                    "!add_graph" -> {
                        val input = requireInput(line, command) ?: continue
                        val (name, graphStr) = requireTokenAndMore(input, command) ?: continue
                        bringGraph(name, graphStr)
                        continue
                    }
                    "!add_graph_from_file" -> {
                        val input = requireInput(line, command) ?: continue
                        val (name, filename) = requireTokenAndMore(input, command) ?: continue
                        val graphStr = readTextFile(filename) ?: continue
                        bringGraph(name, graphStr)
                        continue
                    }
                    "!global_env" -> {
                        executeAndPrint { AbstractPipeline.convertExprValue(globals.asExprValue()) }
                        continue
                    }
                    "!clear" -> {
                        terminal.puts(InfoCmp.Capability.clear_screen)
                        terminal.flush()
                        continue
                    }
                    "!history" -> {
                        for (entry in reader.history) {
                            out.println(entry.pretty())
                        }
                        continue
                    }
                    "!list_commands", "!help" -> {
                        out.info(HELP)
                        continue
                    }
                }

                // Execute PartiQL
                executeAndPrint {
                    val locals = refreshBindings()
                    evaluatePartiQL(line, locals, exiting)
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

    private fun readTextFile(filename: String): String? =
        try {
            val file = File(filename)
            file.readText()
        } catch (ex: Exception) {
            out.error("Could not read text from file '$filename'${ex.message?.let { ":\n$it" } ?: "."}")
            null
        }

    /** Prepare bindings to use for the next evaluation. */
    private fun refreshBindings(): Bindings<ExprValue> {
        return Bindings.buildLazyBindings<ExprValue> {
            addBinding("_") {
                previousResult
            }
        }.delegate(globals.bindings)
    }

    /** Evaluate a textual PartiQL query [textPartiQL] in the context of given [bindings]. */
    private fun evaluatePartiQL(
        textPartiQL: String,
        bindings: Bindings<ExprValue>,
        exiting: AtomicBoolean
    ): PartiQLResult {
        doneCompiling.set(false)
        inputs.put(
            RunnablePipeline.Input(
                textPartiQL,
                EvaluationSession.build {
                    globals(bindings)
                    user(currentUser)
                }
            )
        )
        return catchCancellation(
            doneCompiling,
            exiting,
            pipelineService,
            PartiQLResult.Value(value = ExprValue.newString("Compilation cancelled."))
        ) {
            pipelineService = Executors.newFixedThreadPool(1)
            pipelineService.submit(RunnablePipeline(inputs, results, compiler, doneCompiling))
        } ?: results.poll(5, TimeUnit.SECONDS)!!
    }

    private fun bringGraph(name: String, graphIonText: String) {
        try {
            val graph = ExprValue.newGraph(ExternalGraphReader.read(graphIonText))
            val namedGraph = graph.namedValue(ExprValue.newString(name))
            globals.add(Bindings.ofMap(mapOf(name to namedGraph)))
            out.info("""Bound identifier "$name" to a graph. """)
        } catch (ex: ExternalGraphException) {
            out.error(ex.message)
        }
    }

    private fun executeAndPrint(func: () -> PartiQLResult) {
        val result: PartiQLResult? = try {
            func.invoke()
        } catch (ex: SqlException) {
            out.error(ex.generateMessage())
            out.error(ex.message)
            null // signals that there was an error
        } catch (ex: NotImplementedError) {
            out.error(ex.message ?: "kotlin.NotImplementedError was raised")
            null // signals that there was an error
        }
        printPartiQLResult(result)
    }

    private fun printPartiQLResult(result: PartiQLResult?) {
        when (result) {
            null -> {
                out.error("ERROR!")
            }
            is PartiQLResult.Value -> {
                try {
                    donePrinting.set(false)
                    values.put(result.value)
                    catchCancellation(donePrinting, exiting, printingService, 1) {
                        printingService = Executors.newFixedThreadPool(1)
                        printingService.submit(
                            RunnableWriter(
                                out,
                                ConfigurableExprValueFormatter.pretty,
                                values,
                                donePrinting
                            )
                        )
                    }
                } catch (ex: EvaluationException) { // should not need to do this here; see https://github.com/partiql/partiql-lang-kotlin/issues/1002
                    out.error(ex.generateMessage())
                    out.error(ex.message)
                    return
                }
                out.success("OK!")
            }
            is PartiQLResult.Explain.Domain -> {
                val explain = ExplainFormatter.format(result)
                out.println(explain)
                out.success("OK!")
            }
            is PartiQLResult.Insert,
            is PartiQLResult.Replace,
            is PartiQLResult.Delete -> {
                out.warn("Insert/Replace/Delete are not yet supported")
            }
        }
        out.flush()
    }

    /**
     * If nothing was caught and execution finished: return null
     * If something was caught: resets service and returns defaultReturn
     */
    private fun <T> catchCancellation(
        doneExecuting: AtomicBoolean,
        cancellationFlag: AtomicBoolean,
        service: ExecutorService,
        defaultReturn: T,
        resetService: () -> Unit
    ): T? {
        while (!doneExecuting.get()) {
            if (exiting.get()) {
                service.shutdown()
                service.shutdownNow()
                when (service.awaitTermination(2, TimeUnit.SECONDS)) {
                    true -> {
                        cancellationFlag.set(false)
                        doneExecuting.set(false)
                        resetService()
                        return defaultReturn
                    }
                    false -> throw Exception("Printing service couldn't terminate")
                }
            }
        }
        return null
    }

    private fun retrievePartiQLVersionAndHash(): String {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/partiql.properties"))
        return "${properties.getProperty("version")}-${properties.getProperty("commit")}"
    }

    private fun printAST(query: String) {
        if (query.isNotBlank()) {
            val parser = PartiQLParserBuilder.standard().build()
            val ast = try {
                parser.parseAstStatement(query)
            } catch (ex: SqlException) {
                out.error(ex.generateMessage())
                out.error(ex.message)
                out.error("ERROR!")
                out.flush()
                return
            }
            val explain = PartiQLResult.Explain.Domain(value = ast, format = null)
            val output = ExplainFormatter.format(explain)
            out.println(output)
            out.success("OK!")
            out.flush()
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
