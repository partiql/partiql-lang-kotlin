/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

@file:JvmName("REPL")

package com.amazon.ionsql.tools

import com.amazon.ion.*
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionsql.*
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS

private val PROMPT_1 = "ionsql> "
private val PROMPT_2 = "      | "
private val BAR_1    = "======' "
private val BAR_2    = "------- "

private val ION = IonSystemBuilder.standard().build()

private val DELIM_CONVERTERS = mapOf<String, (String) -> IonValue>(
    "none" to { raw -> ION.newString(raw) },
    "auto" to { raw ->
        try {
            val converted = ION.singleValue(raw)
            when (converted) {
                is IonInt, is IonFloat, is IonDecimal, is IonTimestamp -> converted
                else -> ION.newString(raw)
            }
        } catch (e: IonException) {
            ION.newString(raw)
        }
    }
)

private fun createDelimitedReadHandler(delimiter: String): (InputStream, IonStruct) -> Sequence<ExprValue> =
    { input, options ->
        val encoding = options["encoding"]?.stringValue() ?: "UTF-8"
        val reader = BufferedReader(InputStreamReader(input, encoding))

        val conversion = options["conversion"]?.stringValue() ?: "none"
        val converter = DELIM_CONVERTERS[conversion] ?:
            throw IllegalArgumentException("Unknown conversion: $conversion")

        val hasHeader = options["header"]?.booleanValue() ?: false
        val columns: List<String> = when {
            hasHeader -> {
                val line = reader.readLine()
                    ?: throw IllegalArgumentException("Got EOF for header row")

                line.split(delimiter)
            }
            else -> emptyList()
        }

        object : Iterator<ExprValue> {
            var line = reader.readLine()

            override fun hasNext(): Boolean = line != null

            override fun next(): ExprValue {
                if (line == null) {
                    throw NoSuchElementException()
                }
                val row = ION.newEmptyStruct()
                line.splitToSequence(delimiter)
                    .forEachIndexed { i, raw ->
                        val name = when {
                            i < columns.size -> columns[i]
                            else -> "_$i"
                        }
                        row.add(name, converter(raw))
                    }

                line = reader.readLine()
                return IonExprValue(row)
            }
        }.asSequence()
    }

private val READ_HANDLERS = mapOf<String, (InputStream, IonStruct) -> Sequence<ExprValue>>(
    "ion" to { input, options ->
        ION.iterate(input).asSequence().map { it.exprValue() }
    },
    "tsv" to createDelimitedReadHandler("\t"),
    "csv" to createDelimitedReadHandler(",")
)

/**
 * A very simple implementation of a read-eval-print loop (REPL)
 * for Ion SQL.
 */
fun main(args: Array<String>) {
    // TODO probably should be in "common" utility
    val repl_functions = mapOf<String, (Bindings, List<ExprValue>) -> ExprValue>(
        "read_file" to { env, args ->
            val options = when (args.size) {
                1 -> {
                    ION.newEmptyStruct()
                }
                2 -> {
                    val optVal = args[1].ionValue
                    when (optVal) {
                        is IonStruct -> optVal
                        else -> throw IllegalArgumentException(
                            "Invalid option: $optVal"
                        )
                    }
                }
                else -> throw IllegalArgumentException(
                    "Bad number of arguments for read_file: ${args.size}"
                )
            }

            val fileName = args[0].ionValue.stringValue()
            val fileType = options["type"]?.stringValue() ?: "ion"
            val handler = READ_HANDLERS[fileType] ?:
                throw IllegalArgumentException("Unknown file type: $fileType")
            SequenceExprValue(ION) {
                // TODO we should take care to clean this up properly
                val fileInput = FileInputStream(fileName)
                handler(fileInput, options)
            }
        },
        "write_file" to { env, args ->
            when (args.size) {
                2 -> {
                    val fileName = args[0].ionValue.stringValue()
                    val results = args[1]
                    FileOutputStream(fileName).use {
                        IonTextWriterBuilder.pretty().build(it).use {
                            for (result in results) {
                                result.ionValue.writeTo(it)
                            }
                        }
                    }
                    ION.newBool(true).exprValue()
                }
                else -> throw IllegalArgumentException(
                    "Bad number of arguments for write_file: ${args.size}"
                )
            }
        }
    )

    val evaluator = Evaluator(ION, repl_functions)

    // we use the low-level parser for our config file
    val tokenizer = Tokenizer(ION)
    val parser = Parser(ION)

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
            val config = ION.loader.load(File(args[0]))[0]
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
    var result = ION.newNull().exprValue()
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
            "", "!!", null -> {
                val source = buffer.toString().trim()
                buffer.setLength(0)
                val startNs = System.nanoTime()
                try {
                    if (source != "") {
                        result = when (line) {
                            "!!" -> ION.newEmptyList().apply {
                                add(evaluator.parse(source))
                            }.exprValue()
                            else -> evaluator.compile(source).eval(locals)
                        }

                        print(BAR_1)
                        for (value in result) {
                            value.ionValue.writeTo(out)
                            out.flush()
                        }
                        println("\n$BAR_2")
                    }
                    print("\nOK!")

                } catch (e: Exception) {
                    e.printStackTrace(System.out)
                    print("\nERROR!")
                }
                val endNs = System.nanoTime()
                val totalMs = MILLISECONDS.convert(endNs - startNs, NANOSECONDS)
                println(" ($totalMs ms)")

                if (line == null) {
                    running = false
                }
            }
            else -> buffer.appendln(line)
        }
    }
}
