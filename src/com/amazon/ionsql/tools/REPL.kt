/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

@file:JvmName("REPL")

package com.amazon.ionsql.tools

import com.amazon.ion.*
import com.amazon.ion.IonType.*
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.eval.io.DelimitedValues
import com.amazon.ionsql.eval.io.DelimitedValues.ConversionMode
import com.amazon.ionsql.syntax.IonSqlParser
import com.amazon.ionsql.util.*
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS

private val PROMPT_1 = "ionsql> "
private val PROMPT_2 = "      | "
private val BAR_1    = "======' "
private val BAR_2    = "------- "

private val ION = IonSystemBuilder.standard().build()

private fun delimitedReadHandler(delimiter: String): (InputStream, IonStruct) -> ExprValue =
    { input, options ->
        val encoding = options["encoding"]?.stringValue() ?: "UTF-8"
        val reader = InputStreamReader(input, encoding)

        val conversion = options["conversion"]?.stringValue() ?: "none"
        val conversionMode = ConversionMode
            .values()
            .find { it.name.toLowerCase() == conversion } ?:
                throw IllegalArgumentException("Unknown conversion: $conversion")

        val hasHeader = options["header"]?.booleanValue() ?: false

        DelimitedValues.exprValue(ION, reader, delimiter, hasHeader, conversionMode)
    }

private val READ_HANDLERS = mapOf(
    "ion" to { input, _ ->
        SequenceExprValue(
            ION,
            ION.iterate(input).asSequence().map { it.exprValue() }
        )
    },
    "tsv" to delimitedReadHandler("\t"),
    "csv" to delimitedReadHandler(",")
)

private fun delimitedWriteHandler(delimiter: String): (ExprValue, OutputStream, IonStruct) -> Unit =
    { results, out, options ->
        val encoding = options["encoding"]?.stringValue() ?: "UTF-8"
        val writeHeader = options["header"]?.booleanValue() ?: false
        val nl = options["nl"]?.stringValue() ?: "\n"

        val writer = OutputStreamWriter(out, encoding)
        writer.use {
            DelimitedValues.writeTo(ION, writer, results, delimiter, nl, writeHeader)
        }
    }

private val WRITE_HANDLERS = mapOf(
    "ion" to { results, out, _ ->
        IonTextWriterBuilder.pretty().build(out).use {
            for (result in results) {
                result.ionValue.writeTo(it)
            }
        }
    },
    "tsv" to delimitedWriteHandler("\t"),
    "csv" to delimitedWriteHandler(",")
)

private fun optionsStruct(requiredArity: Int,
                          args: List<ExprValue>,
                          optionsIndex: Int = requiredArity): IonStruct = when (args.size) {
    requiredArity -> ION.newEmptyStruct()
    requiredArity + 1 -> {
        val optVal = args[optionsIndex].ionValue
        when (optVal) {
            is IonStruct -> optVal
            else -> throw IllegalArgumentException(
                "Invalid option: $optVal"
            )
        }
    }
    else -> throw IllegalArgumentException(
        "Bad number of arguments: ${args.size}"
    )
}

/**
 * A very simple implementation of a read-eval-print loop (REPL)
 * for Ion SQL.
 */
fun main(args: Array<String>) {
    // TODO probably should be in "common" utility
    val replFunctions = mapOf<String, (Environment, List<ExprValue>) -> ExprValue>(
        "read_file" to { _, args ->
            val options = optionsStruct(1, args)
            val fileName = args[0].stringValue()
            val fileType = options["type"]?.stringValue() ?: "ion"
            val handler = READ_HANDLERS[fileType] ?:
                throw IllegalArgumentException("Unknown file type: $fileType")
            val seq = Sequence<ExprValue> {
                // TODO we should take care to clean this up properly
                val fileInput = FileInputStream(fileName)
                handler(fileInput, options).iterator()
            }
            SequenceExprValue(ION, seq)
        },
        "write_file" to { _, args ->
            val options = optionsStruct(2, args, optionsIndex = 1)
            val fileName = args[0].stringValue()
            val fileType = options["type"]?.stringValue() ?: "ion"
            val resultsIndex = when (args.size) {
                2 -> 1
                else -> 2
            }
            val results = args[resultsIndex]
            val handler = WRITE_HANDLERS[fileType] ?:
                throw IllegalArgumentException("Unknown file type: $fileType")

            try {
                FileOutputStream(fileName).use {
                    handler(results, it, options)
                }
                ION.newBool(true).exprValue()
            } catch (e: Exception) {
                e.printStackTrace()
                ION.newBool(false).exprValue()
            }
        }
    )

    val parser = IonSqlParser(ION)
    val evaluator = EvaluatingCompiler(ION, parser, replFunctions)

    val globals = when {
        args.isNotEmpty() -> {
            val configSource = File(args[0]).readText(charset("UTF-8"))
            val config = evaluator.compile(configSource).eval(Bindings.empty())
            config.bindings
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
                                add(parser.parse(source))
                            }.exprValue()
                            else -> evaluator.compile(source).eval(locals)
                        }

                        print(BAR_1)
                        for (value in result.rangeOver()) {
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
