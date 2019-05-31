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

import com.amazon.ion.*
import com.amazon.ion.system.*
import org.partiql.cli.ReplState.*
import org.partiql.lang.*
import org.partiql.lang.ast.*
import org.partiql.lang.eval.*
import org.partiql.lang.syntax.*
import org.partiql.lang.util.*
import java.io.*
import java.lang.IllegalArgumentException
import java.text.*
import java.util.concurrent.*

internal const val PROMPT_1 = "PartiQL> "
internal const val PROMPT_2 = "   | "
internal const val BAR_1 = "===' "
internal const val BAR_2 = "--- "
internal const val WELCOME_MSG = "Welcome to the PartiQL REPL!" // TODO: extract version from gradle.build and append to message 

private enum class ReplState {
    /** Initial state, first state as soon as you start the REPL */
    INIT,

    /** Ready to accept new input. */
    READY,
    
    /** Reading a PartiQL query. Transitions to execute when one of the execution tokens is found. */
    READ_PARTIQL,

    /** Ready to execute a PartiQL query. */
    EXECUTE_PARTIQL,

    /** Ready to parse a PartiQL query and display the full AST. */
    PARSE_PARTIQL,

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
    var bindings = Bindings.EMPTY
        private set

    fun add(bindings: Bindings): GlobalBinding {
        when (bindings) {
            is MapBindings -> {
                knownNames.addAll(bindings.bindingMap.originalCaseMap.keys)
                this.bindings = bindings.delegate(this.bindings)
            }
            Bindings.EMPTY -> {
            } // nothing to do 
            else           -> throw IllegalArgumentException("Invalid binding type for global environment: $bindings")
        }

        return this
    }

    fun asExprValue(): ExprValue {
        val values: Sequence<ExprValue> = knownNames.map { bindings[BindingName(it, BindingCase.SENSITIVE)] }
            .filterNotNull()
            .asSequence()
        
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
                    private val input: InputStream,
                    private val output: OutputStream,
                    private val parser: Parser,
                    private val compiler: CompilerPipeline,
                    private val initialGlobal: Bindings,
                    private val timer: Timer = object: Timer{}) : SqlCommand() {

    private inner class ReplCommands {
        operator fun get(commandName: String): (String) -> ExprValue? = 
            commands[commandName] ?: 
                throw IllegalArgumentException("REPL command: '$commandName' not found! " + 
                                               "use '!list_commands' to see all available commands")

        private val commands: Map<String, (String) -> ExprValue?> = mapOf("add_to_global_env" to ::addToGlobalEnv,
                                                                          "global_env" to ::globalEnv,
                                                                          "list_commands" to ::listCommands)

        private fun addToGlobalEnv(source: String): ExprValue? {
            if (source == "") {
                throw IllegalArgumentException("add_to_global_env requires 1 parameter")
            }

            val locals = Bindings.buildLazyBindings { addBinding("_") { previousResult } }.delegate(globals.bindings)
            val result = compiler.compile(source).eval(EvaluationSession.build { globals(locals) })
            globals.add(result.bindings)

            return result
        }

        private fun globalEnv(source: String): ExprValue? = globals.asExprValue()

        private fun listCommands(source: String): ExprValue? {
            output.println("")
            output.println("""
                |!add_to_global_env: adds a value to the global environment
                |!global_env: displays the current global environment
                |!list_commands: print this message
            """.trimMargin())
            return null
        }
    }

    private fun executeReplCommand(): ReplState = executeTemplate(valuePrettyPrinter) { source ->
        // TODO make a real parser for this. partiql-lang-kotlin/issues/63
        val splitIndex = source.indexOfFirst { it == ' ' }.let {
            if (it == -1) {
                source.length
            } else {
                it
            }
        }

        val replCommandName = source.substring(1, splitIndex)
        val cmdSource = source.substring(splitIndex)

        commands[replCommandName].invoke(cmdSource)
    }
    
    private val commands = ReplCommands()

    private val bufferedReader = input.bufferedReader(Charsets.UTF_8)
    private val valuePrettyPrinter = NonConfigurableExprValuePrettyPrinter(output) // for PartiQL values
    private val astPrettyPrinter = object : ExprValuePrettyPrinter {
        val ionWriter = IonTextWriterBuilder.pretty().build(output) // for the AST
        
        override fun prettyPrint(value: ExprValue) {
            value.ionValue.writeTo(ionWriter)
            ionWriter.flush()
        }
    } 

    private fun OutputStream.print(s: String) = write(s.toByteArray(Charsets.UTF_8))
    private fun OutputStream.println(s: String) = print("$s\n")

    // Repl running state
    private val buffer = StringBuilder()
    private var globals = GlobalBinding(valueFactory).add(initialGlobal)
    private var state = INIT
    private var previousResult = valueFactory.nullValue
    private var line: String? = null

    private fun printWelcomeMessage() = output.println(WELCOME_MSG)
    
    private fun printPrompt() { 
        when {
            buffer.isEmpty() -> output.print(PROMPT_1)
            else             -> output.print(PROMPT_2)
        }
        output.flush()
    }

    private fun readLine(): String? {
        printPrompt()
        return bufferedReader.readLine()?.trim()
    }

    private fun executeTemplate(prettyPrinter: ExprValuePrettyPrinter, f: (String) -> ExprValue?): ReplState {
        try {
            val source = buffer.toString().trim()
            buffer.setLength(0)

            val totalMs = timer.timeIt {
                val result = f(source)
                if (result != null) {
                    output.println(BAR_1)
                    prettyPrinter.prettyPrint(result)
                    
                    output.println("\n$BAR_2")

                    previousResult = result
                }
            }
            output.println("OK! ($totalMs ms)")
            output.flush()
        }
        catch (e: Exception) {
            e.printStackTrace(PrintStream(output))
            output.println("ERROR!")
        }

        return if (line == null) {
            FINAL
        } else {
            READY
        }
    }

    private fun executePartiQL(): ReplState = executeTemplate(valuePrettyPrinter) { source ->
        if (source != "") {
            val locals = Bindings.buildLazyBindings { addBinding("_") { previousResult } }.delegate(globals.bindings)

            compiler.compile(source).eval(EvaluationSession.build { globals(locals) })
        } else {
            null
        }
    }

    private fun parsePartiQL(): ReplState = executeTemplate(astPrettyPrinter) { source ->
        if (source != "") {
            val serializedAst = AstSerializer.serialize(parser.parseExprNode(source), valueFactory.ion)
            valueFactory.newFromIonValue(serializedAst)
        } else {
            null
        }
    }

    private fun parsePartiQLWithFilters(): ReplState = executeTemplate(astPrettyPrinter) { source ->
        if (source != "") {
            val serializedAst = AstSerializer.serialize(parser.parseExprNode(source), valueFactory.ion)
            valueFactory.newFromIonValue(serializedAst.filterTermNodes())
        } else {
            null
        }
    }

    override fun run() {
        while (state != FINAL) {
            state = when (state) {
                INIT                     -> {
                    printWelcomeMessage()
                    READY
                }
                READY                    -> {
                    line = readLine()
                    when {
                        line == null                               -> FINAL
                        arrayOf("!!", "!?", "").any { it == line } -> EXECUTE_PARTIQL
                        line!!.startsWith("!")                     -> READ_REPL_COMMAND
                        else                                       -> READ_PARTIQL
                    }
                }

                READ_PARTIQL              -> {
                    buffer.appendln(line)
                    line = readLine()
                    when (line) {
                        null     -> FINAL
                        ""       -> EXECUTE_PARTIQL
                        "!!"     -> PARSE_PARTIQL_WITH_FILTER
                        "!?"     -> PARSE_PARTIQL
                        else     -> READ_PARTIQL
                    }
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
                PARSE_PARTIQL             -> parsePartiQL()
                PARSE_PARTIQL_WITH_FILTER -> parsePartiQLWithFilters()
                EXECUTE_REPL_COMMAND      -> executeReplCommand()

                // shouldn't really happen
                FINAL                     -> FINAL
            }
        }
    }
}
