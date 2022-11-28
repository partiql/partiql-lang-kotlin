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

package org.partiql.shell

import com.google.common.base.CharMatcher
import com.google.common.util.concurrent.Uninterruptibles
import org.fusesource.jansi.AnsiConsole
import org.jline.reader.EndOfFileException
import org.jline.reader.History
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.jline.utils.InfoCmp
import org.joda.time.Duration
import org.partiql.format.ExplainFormatter
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.delegate
import org.partiql.lang.util.ConfigurableExprValueFormatter
import org.partiql.lang.util.ExprValueFormatter
import org.partiql.pipeline.AbstractPipeline
import java.io.Closeable
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.GuardedBy

private const val PROMPT_1 = "PartiQL> "
private const val PROMPT_2 = "   | "
private const val BAR_1 = "===' "
private const val BAR_2 = "--- "
private const val WELCOME_MSG = "Welcome to the PartiQL REPL!"

private const val HELP = """
!add_to_global_env  Adds a value to the global environment
!global_env         Displays the current global environment
!list_commands      Prints this message
!help               Prints this message
!history            Prints command history
!exit               Exits the shell
!clear              Clears the screen
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
internal class Shell(
    private val valueFactory: ExprValueFactory,
    private val output: OutputStream,
    private val compiler: AbstractPipeline,
    private val initialGlobal: Bindings<ExprValue>,
    private val config: ShellConfiguration = ShellConfiguration()
) {

    private val homeDir: Path = Paths.get(System.getProperty("user.home"))
    private val globals = ShellGlobalBinding(valueFactory).add(initialGlobal)
    private var previousResult = valueFactory.nullValue
    private val out = PrintStream(output)

    fun start() {
        val exiting = AtomicBoolean()
        val interrupter = ThreadInterrupter()
        val exited = CountDownLatch(1)
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

    private fun run(exiting: AtomicBoolean) = TerminalBuilder.builder().build().use { terminal ->
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

            // Handle commands
            val command = when (val end: Int = CharMatcher.`is`(';').or(CharMatcher.whitespace()).indexIn(line)) {
                -1 -> ""
                else -> line.substring(0, end)
            }
            when (command.toLowerCase(Locale.ENGLISH).trim()) {
                "!exit" -> return
                "!add_to_global_env" -> {
                    // Consider PicoCLI + Jline, but it doesn't easily place nice with commands + raw SQL
                    // https://github.com/partiql/partiql-lang-kotlin/issues/63
                    val arg = line.trim().removePrefix(command).trim()
                    if (arg.isEmpty() || arg.isBlank()) {
                        out.error("!add_to_global_env requires 1 parameter")
                        continue
                    }
                    executeAndPrint {
                        val locals = Bindings.buildLazyBindings<ExprValue> {
                            addBinding("_") {
                                previousResult
                            }
                        }.delegate(globals.bindings)
                        val result = compiler.compile(arg, EvaluationSession.build { globals(locals) }) as PartiQLResult.Value
                        globals.add(result.value.bindings)
                        result
                    }
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
            try {
                executeAndPrint {
                    val locals = Bindings.buildLazyBindings<ExprValue> {
                        addBinding("_") { previousResult }
                    }.delegate(globals.bindings)
                    compiler.compile(line, EvaluationSession.build { globals(locals) })
                }
            } catch (ex: Exception) {
                out.error(ex.stackTraceToString())
            }
        }
    }

    private fun executeAndPrint(func: () -> PartiQLResult?) {
        val result: PartiQLResult? = try {
            func.invoke()
        } catch (ex: Exception) {
            out.error(ex.stackTraceToString())
            out.error("ERROR!")
            null
        }
        printPartiQLResult(result)
    }

    private fun printPartiQLResult(result: PartiQLResult?) {
        when (result) {
            null -> {
                out.success("OK!")
                out.flush()
            }
            is PartiQLResult.Value -> printExprValue(ConfigurableExprValueFormatter.pretty, result.value)
            is PartiQLResult.Explain.Domain -> {
                val explain = ExplainFormatter.format(result)
                out.println(explain)
                out.flush()
            }
            is PartiQLResult.Insert,
            is PartiQLResult.Replace,
            is PartiQLResult.Delete -> error("Insert/Replace/Delete are not yet supported")
        }
    }

    private fun printExprValue(formatter: ExprValueFormatter, result: ExprValue) {
        out.info(BAR_1)
        formatter.formatTo(result, out)
        out.println()
        out.info(BAR_2)
        previousResult = result
        out.success("OK!")
        out.flush()
    }

    private fun retrievePartiQLVersionAndHash(): String {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/partiql.properties"))
        return "${properties.getProperty("version")}-${properties.getProperty("commit")}"
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

private fun PrintStream.success(string: String) = this.println(ansi(string, SUCCESS))

private fun PrintStream.error(string: String) = this.println(ansi(string, ERROR))

private fun PrintStream.info(string: String) = this.println(ansi(string, INFO))

private fun PrintStream.warn(string: String) = this.println(ansi(string, WARN))

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
