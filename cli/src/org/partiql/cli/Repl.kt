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
import org.partiql.lang.*
import org.partiql.lang.ast.*
import org.partiql.lang.eval.*
import org.partiql.lang.syntax.*
import org.partiql.lang.util.*
import java.io.*
import java.text.*

private const val PROMPT_1 = "sql> "
private const val PROMPT_2 = "   | "
private const val BAR_1 = "===' "
private const val BAR_2 = "--- "

/**
 * TODO builder, kdoc
 */
internal class Repl(private val valueFactory: ExprValueFactory,
                    private val input: InputStream,
                    private val output: OutputStream,
                    private val parser: Parser,
                    private val compiler: CompilerPipeline,
                    private val globals: Bindings): SqlCommand() {

    private val buffer = StringBuilder()

    private val bufferedReader = input.bufferedReader(Charsets.UTF_8)
    private val writer = IonTextWriterBuilder.pretty().build(output)

    private fun OutputStream.print(s: String) = write(s.toByteArray(Charsets.UTF_8))
    private fun OutputStream.println(s: String) = print("$s\n")

    private fun printPrompt() = when {
        buffer.isEmpty() -> output.print(PROMPT_1)
        else             -> output.print(PROMPT_2)
    }

    override fun run() {
        var result = valueFactory.nullValue
        var running = true

        while (running) {
            printPrompt()

            val line = bufferedReader.readLine()

            when (line) {
                "", "!!", "!?", null -> {
                    val source = buffer.toString().trim()
                    buffer.setLength(0)

                    val totalMs = timeIt {
                        try {
                            if (source != "") {
                                // capture the result in a immutable binding
                                // and construct an environment for evaluation over it
                                val previousResult = result
                                val locals = Bindings.buildLazyBindings {
                                    addBinding("_") {
                                        previousResult
                                    }
                                }.delegate(globals)

                                result = when (line) {
                                    "!!" ->
                                        valueFactory.newFromIonValue(
                                            AstSerializer.serialize(parser.parseExprNode(source), valueFactory.ion)
                                                .filterTermNodes())
                                    "!?" ->
                                        valueFactory.newFromIonValue(
                                            AstSerializer.serialize(parser.parseExprNode(source), valueFactory.ion).clone())
                                    else ->
                                        compiler.compile(source).eval(EvaluationSession.build { globals(locals) })
                                }

                                output.print(BAR_1)
                                val itemCount = writeResult(result, writer)
                                output.println("\n$BAR_2")

                                output.print("Result type was ${result.type}")

                                if (result.type.isRangedFrom) {
                                    val formattted = NumberFormat.getIntegerInstance().format(itemCount)
                                    output.print(" and contained $formattted items")
                                }
                            }

                            output.print("\nOK!")
                            output.flush()
                        }
                        catch (e: Exception) {
                            e.printStackTrace(System.out)
                            output.print("\nERROR!")
                        }
                    }

                    output.println(" ($totalMs ms)")

                    if (line == null) {
                        running = false
                    }
                }
                else           -> buffer.appendln(line)
            }
        }
    }
}
