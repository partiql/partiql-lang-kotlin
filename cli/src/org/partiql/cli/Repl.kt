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

package org.partiql.cli

import com.amazon.ion.system.*
import com.amazon.ionelement.api.toIonValue
import org.partiql.cli.ReplState.*
import org.partiql.lang.*
import org.partiql.lang.eval.*
import org.partiql.lang.syntax.*
import org.partiql.lang.util.*
import java.io.*
import java.util.Properties
import java.util.concurrent.*

internal const val PROMPT_1 = "PartiQL> "
internal const val PROMPT_2 = "   | "
internal const val BAR_1 = "===' "
internal const val BAR_2 = "--- "
internal const val WELCOME_MSG = "Welcome to the PartiQL REPL!"

private enum class ReplState {
    /** Initial state, first state as soon as you start the REPL */
    INIT,

    /** Ready to accept new input. */
    READY,

    /** Reading a PartiQL query. Transitions to execute when one of the execution tokens is found. */
    READ_PARTIQL,

    /** Read a trailing semi-colon. Add the current line to the buffer and transition to execute. */
    LAST_PARTIQL_LINE,

    /** Ready to execute a PartiQL query. */
    EXECUTE_PARTIQL,

    /** Ready to parse a PartiQL query and display AST with meta nodes filtered out. */
    PARSE_PARTIQL_WITH_FILTER,

    /** Reading a REPL command. Transitions to execute when one of the execution tokens is found. */
    READ_REPL_COMMAND,

    /** Ready to parse a REPL command. */
    EXECUTE_REPL_COMMAND,

    /** Final state, nothing left to execute ready to terminate. */
    FINAL
}

private class GlobalBinding(private val valueFactory: ExprValueFactory) {



    private val knownNames = mutableSetOf<String>()
    var bindings = Bindings.empty<ExprValue>()
        private set

    fun add(bindings: Bindings<ExprValue>): GlobalBinding {
        when (bindings) {
            is MapBindings -> {
                knownNames.addAll(bindings.originalCaseMap.keys)
                this.bindings = bindings.delegate(this.bindings)
            }
            Bindings.empty<ExprValue>() -> {
            } // nothing to do
            else           -> throw IllegalArgumentException("Invalid binding type for global environment: $bindings")
        }

        return this
    }

    fun asExprValue(): ExprValue {
        val values: Sequence<ExprValue> = knownNames.map {
            bindings[BindingName(it, BindingCase.SENSITIVE)]
        }.filterNotNull().asSequence()

        return valueFactory.newStruct(values, StructOrdering.UNORDERED)
    }
}

interface Timer {
    fun <T> timeIt(block: () -> T): Long {
        val start = System.nanoTime()
        block()
        val end = System.nanoTime()

        return TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS)
    }
}

/**
 * TODO builder, kdoc
 */
internal class Repl(private val valueFactory: ExprValueFactory,
                    input: InputStream,
                    output: OutputStream,
                    private val parser: Parser,
                    private val compiler: CompilerPipeline,
                    initialGlobal: Bindings<ExprValue>,
                    private val timer: Timer = object : Timer {}
) : PartiQLCommand {

    private val outputWriter = OutputStreamWriter(output, "UTF-8")

    private inner class ReplCommands {
        operator fun get(commandName: String): (String) -> ExprValue? = commands[commandName]
                                                                        ?: throw IllegalArgumentException("REPL command: '$commandName' not found! " + "use '!list_commands' to see all available commands")

        private val commands: Map<String, (String) -> ExprValue?> = mapOf("add_to_global_env" to ::addToGlobalEnv,
                                                                          "global_env" to ::globalEnv,
                                                                          "list_commands" to ::listCommands)

        private fun addToGlobalEnv(source: String): ExprValue? {
            if (source == "") {
                throw IllegalArgumentException("add_to_global_env requires 1 parameter")
            }

            val locals = Bindings.buildLazyBindings<ExprValue> { addBinding("_") { previousResult } }.delegate(globals.bindings)
            val result = compiler.compile(source).eval(EvaluationSession.build { globals(locals) })
            globals.add(result.bindings)

            return result
        }

        private fun globalEnv(@Suppress("UNUSED_PARAMETER") source: String): ExprValue? = globals.asExprValue()

        private fun listCommands(@Suppress("UNUSED_PARAMETER") source: String): ExprValue? {
            outputWriter.write("\n")
            outputWriter.write("""
                |!add_to_global_env: adds a value to the global environment
                |!global_env: displays the current global environment
                |!list_commands: print this message
                |
            """.trimMargin())
            return null
        }
    }

    private fun executeReplCommand(): ReplState = executeTemplate(valuePrettyPrinter) { source ->
        // TODO make a real parser for this. partiql-lang-kotlin/issues/63
        val splitIndex = source.indexOfFirst { it == ' ' }.let {
            if (it == -1) {
                source.length
            }
            else {
                it
            }
        }

        val replCommandName = source.substring(1, splitIndex)
        val cmdSource = source.substring(splitIndex)

        commands[replCommandName].invoke(cmdSource)
    }

    private val commands = ReplCommands()

    private val bufferedReader = input.bufferedReader(Charsets.UTF_8)
    private val valuePrettyPrinter = ConfigurableExprValueFormatter.pretty
    private val astPrettyPrinter = object : ExprValueFormatter {
        val writer = IonTextWriterBuilder.pretty().build(outputWriter)

        override fun formatTo(value: ExprValue, out: Appendable) {
            value.ionValue.writeTo(writer)
            writer.flush()
        }
    }

    // Repl running state
    private val buffer = StringBuilder()
    private var globals = GlobalBinding(valueFactory).add(initialGlobal)
    private var state = INIT
    private var previousResult = valueFactory.nullValue
    private var line: String? = null

    private fun printWelcomeMessage() {
        outputWriter.write(WELCOME_MSG)
        outputWriter.write("\n")
    }

    fun retrievePartiQLVersionAndHash(): String {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/partiql.properties"))
        return "${properties.getProperty("version")}-${properties.getProperty("commit")}"
    }

    private fun printVersionNumber() {
        outputWriter.write("Using version: ${retrievePartiQLVersionAndHash()}")
        outputWriter.write("\n")
    }

    private fun printPrompt() {
        when {
            buffer.isEmpty() -> outputWriter.write(PROMPT_1)
            else             -> outputWriter.write(PROMPT_2)
        }
        outputWriter.flush()
    }

    private fun readLine(): String? {
        printPrompt()
        return bufferedReader.readLine()?.trim()
    }

    private fun executeTemplate(formatter: ExprValueFormatter, f: (String) -> ExprValue?): ReplState {
        try {
            val source = buffer.toString().trim()
            buffer.setLength(0)

            val result = f(source)
            if (result != null) {
                outputWriter.write(BAR_1)
                outputWriter.write("\n")

                formatter.formatTo(result, outputWriter)
                outputWriter.write("\n")

                outputWriter.write(BAR_2)
                outputWriter.write("\n")

                previousResult = result
            }
            outputWriter.write("OK!")
            outputWriter.write("\n")
            outputWriter.flush()
        }
        catch (e: Exception) {
            e.printStackTrace(PrintWriter(outputWriter))
            outputWriter.write("ERROR!")
            outputWriter.write("\n")
        }

        return if (line == null) {
            FINAL
        }
        else {
            READY
        }
    }

    private fun executePartiQL(): ReplState = executeTemplate(valuePrettyPrinter) { source ->
        if (source != "") {
            val locals = Bindings.buildLazyBindings<ExprValue> { addBinding("_") { previousResult } }.delegate(globals.bindings)

            compiler.compile(source).eval(EvaluationSession.build { globals(locals) })
        }
        else {
            null
        }
    }

    private fun parsePartiQLWithFilters(): ReplState = executeTemplate(astPrettyPrinter) { source ->
        if (source != "") {
            val astStatementSexp = parser.parseAstStatement(source).toIonElement()
            val astStatmentIonValue = astStatementSexp.asAnyElement().toIonValue(valueFactory.ion)
            valueFactory.newFromIonValue(astStatmentIonValue)
        }
        else {
            null
        }
    }

    override fun run() {
        while (state != FINAL) {
            state = when (state) {
                INIT                      -> {
                    printWelcomeMessage()
                    printVersionNumber()
                    READY
                }

                READY                     -> {
                    line = readLine()
                    when {
                        line == null                         -> FINAL
                        arrayOf("!!", "").any { it == line } -> EXECUTE_PARTIQL
                        line!!.startsWith("!")               -> READ_REPL_COMMAND
                        line!!.endsWith(";")                 -> LAST_PARTIQL_LINE
                        else                                 -> READ_PARTIQL
                    }
                }

                READ_PARTIQL              -> {
                    buffer.appendln(line)
                    line = readLine()
                    when {
                        line == null         -> FINAL
                        line == ""           -> EXECUTE_PARTIQL
                        line!!.endsWith(";") -> LAST_PARTIQL_LINE
                        line == "!!"         -> PARSE_PARTIQL_WITH_FILTER
                        else                 -> READ_PARTIQL
                    }
                }

                LAST_PARTIQL_LINE         -> {
                    buffer.appendln(line)
                    EXECUTE_PARTIQL
                }

                READ_REPL_COMMAND         -> {
                    buffer.appendln(line)
                    line = readLine()
                    when (line) {
                        null -> FINAL
                        ""   -> EXECUTE_REPL_COMMAND
                        else -> READ_REPL_COMMAND
                    }
                }

                EXECUTE_PARTIQL           -> executePartiQL()
                PARSE_PARTIQL_WITH_FILTER -> parsePartiQLWithFilters()
                EXECUTE_REPL_COMMAND      -> executeReplCommand()

                // shouldn't really happen
                FINAL                     -> FINAL
            }
        }
    }
}
