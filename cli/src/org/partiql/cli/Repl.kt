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
import org.partiql.cli.ReplState.*
import org.partiql.lang.*
import org.partiql.lang.ast.*
import org.partiql.lang.eval.*
import org.partiql.lang.syntax.*
import org.partiql.lang.util.*
import java.io.*
import java.lang.IllegalArgumentException
import java.text.*

private const val PROMPT_1 = "PartiQL> "
private const val PROMPT_2 = "   | "
private const val BAR_1 = "===' "
private const val BAR_2 = "--- "

private enum class ReplState {
    /** Initial state, ready to accept new input. */
    INITIAL, 
    
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

/**
 * TODO builder, kdoc
 */
internal class Repl(private val valueFactory: ExprValueFactory,
                    private val input: InputStream,
                    private val output: OutputStream,
                    private val parser: Parser,
                    private val compiler: CompilerPipeline,
                    private val initialGlobal: Bindings) : SqlCommand() {
    
    private inner class ReplCommands {
        operator fun get(commandName: String): (String) -> Unit =
            commands[commandName] ?: 
                throw IllegalArgumentException("REPL command: '$commandName' not found! " +
                                               "use '!list_commands' to see all available commands")
        
        private val commands: Map<String, (String) -> Unit> = mapOf(
            "add_to_global_env" to ::addToGlobalEnv,
            "list_commands" to ::listCommands
        )

        private fun addToGlobalEnv(source: String) {
            if(source == "") {
                throw IllegalArgumentException("add_to_global_env requires 1 parameter")
            }

            val result = compiler.compile(source).eval(EvaluationSession.build { globals(globals) })
            globals = result.bindings.delegate(globals)
        }

        private fun listCommands(source: String) {
            output.println("")
            output.println("""|!add_to_global_env: adds a value to the global environment
                              |!list_commands: print this message""".trimMargin())
        }
    }
    
    private fun executeReplCommand() = executeTemplate { source ->
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
    private val writer = IonTextWriterBuilder.pretty().build(output)

    private fun OutputStream.print(s: String) = write(s.toByteArray(Charsets.UTF_8))
    private fun OutputStream.println(s: String) = print("$s\n")

    // Repl running state
    private val buffer = StringBuilder()
    private var globals = initialGlobal
    private var state = INITIAL
    private var previousResult = valueFactory.nullValue
    private var line: String? = null

    private fun printPrompt() = when {
        buffer.isEmpty() -> output.print(PROMPT_1)
        else             -> output.print(PROMPT_2)
    }

    private fun readLine(): String?{
        printPrompt()
        return bufferedReader.readLine()?.trim()
    }

    private fun executeTemplate(f: (String) -> Unit): ReplState {
        try {
            val source = buffer.toString().trim()
            buffer.setLength(0)

            val totalMs = timeIt { f(source) }
            output.println("OK! ($totalMs ms)")
            output.flush()
        }
        catch (e: Exception) {
            e.printStackTrace(System.out)
            output.println("ERROR!")
        }
        
        return if (line == null) {
            FINAL
        } else {
            INITIAL
        }
    }

    private fun partiQLTemplate(f: (String) -> ExprValue) = executeTemplate { source ->
        if (source != "") {
            // capture the result in a immutable binding and construct an environment for evaluation over it
            val result = f(source)
            
            output.print(BAR_1)
            val itemCount = writeResult(result, writer)
            output.println("\n$BAR_2")

            output.print("Result type was ${result.type}")

            if (result.type.isRangedFrom) {
                val formatted = NumberFormat.getIntegerInstance().format(itemCount)
                output.print(" and contained $formatted items")
            }

            output.println("")

            previousResult = result
        }
    }

    private fun executePartiQL() = partiQLTemplate { source ->
        val locals = Bindings.buildLazyBindings { addBinding("_") { previousResult } }.delegate(globals)

        compiler.compile(source).eval(EvaluationSession.build { globals(locals) })
    }

    private fun parsePartiQL() = partiQLTemplate { source ->
        val serializedAst = AstSerializer.serialize(parser.parseExprNode(source), valueFactory.ion)
        valueFactory.newFromIonValue(serializedAst)
    }

    private fun parsePartiQLWithFilters() = partiQLTemplate { source ->
        val serializedAst = AstSerializer.serialize(parser.parseExprNode(source), valueFactory.ion)
        valueFactory.newFromIonValue(serializedAst.filterTermNodes())
    }

    override fun run() {
        while (state != FINAL) {
            state = when (state) {
                INITIAL                    -> {
                    line = readLine()
                    when {
                        arrayOf("!!", "!?", "", null).any { it == line } -> EXECUTE_PARTIQL
                        line!!.startsWith("!")                           -> READ_REPL_COMMAND
                        else                                             -> READ_PARTIQL
                    }
                }

                READ_PARTIQL              -> {
                    buffer.appendln(line)
                    line = readLine()
                    when (line) {
                        "", null -> EXECUTE_PARTIQL
                        "!!"     -> PARSE_PARTIQL_WITH_FILTER
                        "!?"     -> PARSE_PARTIQL
                        else     -> READ_PARTIQL
                    }
                }

                READ_REPL_COMMAND          -> {
                    buffer.appendln(line)
                    line = readLine()
                    when (line) {
                        "", null -> EXECUTE_REPL_COMMAND
                        else     -> READ_REPL_COMMAND
                    }
                }

                EXECUTE_PARTIQL           -> executePartiQL()
                PARSE_PARTIQL             -> parsePartiQL()
                PARSE_PARTIQL_WITH_FILTER -> parsePartiQLWithFilters()
                EXECUTE_REPL_COMMAND       -> executeReplCommand()

                // shouldn't really happen
                FINAL                      -> FINAL
            }
        }
    }
}
