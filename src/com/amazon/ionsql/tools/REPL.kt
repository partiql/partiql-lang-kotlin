/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

@file:JvmName("REPL")

package com.amazon.ionsql.tools

import com.amazon.ion.IonSexp
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionsql.*
import java.io.File
import java.io.OutputStream
import java.util.*

private val PROMPT_1 = "ionsql> "
private val PROMPT_2 = "      | "
private val BAR_1    = "======'"
private val BAR_2    = "------"

/**
 * A very simple implementation of a read-eval-print loop (REPL)
 * for Ion SQL.
 */
fun main(args: Array<String>) {
    val ion = IonSystemBuilder.standard().build()

    val evaluator = Evaluator(ion)

    // we use the low-level parser for our config file
    val tokenizer = Tokenizer(ion)
    val parser = Parser(ion)

    fun evalConfig(expr: IonSexp): ExprValue {
        val tokens = tokenizer.tokenize(expr)
        val ast = parser.parse(tokens)
        return evaluator.eval(ast, Bindings.empty())
    }

    val globals = when {
        args.size > 0 -> {
            // we evaluate this configuration manually to avoid materializing values that
            // may be really large if we just evaluate the struct as-is it will force
            // conversion to IonValue
            val bindings = HashMap<String, ExprValue>()
            val config = ion.loader.load(File(args[0]))[0]
            for (member in config) {
                val name = member.fieldName
                val exprVal = when (member) {
                    is IonSexp -> evalConfig(member)
                    else -> member.exprValue()
                }
                bindings.put(name, exprVal)
            }
            Bindings.over { bindings[it] }
        }
        else -> Bindings.empty()
    }

    val out = IonTextWriterBuilder.pretty().build(System.out as OutputStream)
    val buffer = StringBuilder()
    var result = ion.newNull().exprValue()
    val locals = Bindings.over {
        when (it) {
            "_" -> result
            else -> globals[it]
        }
    }
    var running = true
    while (running) {
        when {
            buffer.isEmpty() -> print(PROMPT_1)
            else -> print(PROMPT_2)
        }
        val line = readLine()
        when (line) {
            "", null -> {
                val source = buffer.toString().trim()
                buffer.setLength(0)
                try {
                    if (source != "") {
                        result = evaluator.compile(source).eval(locals)

                        print(BAR_1)
                        for (value in result) {
                            value.ionValue.writeTo(out)
                            out.flush()
                        }
                        println("\n$BAR_2")
                    }
                    println("\nOK!")

                } catch (e: Exception) {
                    e.printStackTrace()
                    println("\nERROR!")
                }

                if (line == null) {
                    running = false
                }
            }
            else -> buffer.appendln(line)
        }
    }

    throw UnsupportedOperationException("Implement Me!")
}