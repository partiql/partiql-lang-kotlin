/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

@file:JvmName("REPL")

package com.amazon.ionsql.tools

import com.amazon.ion.*
import com.amazon.ion.IonType.*
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

private fun delimitedReadHandler(delimiter: String): (InputStream, IonStruct) -> Sequence<ExprValue> =
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

private val READ_HANDLERS = mapOf(
    "ion" to { input, options ->
        ION.iterate(input).asSequence().map { it.exprValue() }
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
            var names: List<String>? = null
            for (row in results) {
                val colNames = row.asFacet(OrderedBindNames::class.java)?.orderedNames
                    ?: throw IllegalArgumentException("Delimited data must be ordered tuple: $row")
                if (names == null) {
                    // first row defines column names
                    names = colNames

                    if (writeHeader) {
                        names.joinTo(writer, delimiter)
                        writer.write(nl)
                    }
                } else if (names != colNames) {
                    // mismatch on the tuples
                    throw IllegalArgumentException(
                        "Inconsistent row names: $colNames != $names"
                    )
                }

                row.map {
                    val col = it.ionValue
                    when (col.type) {
                        // TODO configurable null handling
                        NULL, BOOL, INT, FLOAT, DECIMAL, TIMESTAMP -> col.toString()
                        SYMBOL, STRING -> col.stringValue()
                        // TODO LOB/BLOB support
                        else -> throw IllegalArgumentException(
                            "Delimited data column must not be ${col.type} type"
                        )
                    }
                }.joinTo(writer, delimiter)
                writer.write(nl)
            }
        }
    }

private val WRITE_HANDLERS = mapOf(
    "ion" to { results, out, options ->
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
    val replFunctions = mapOf<String, (Bindings, List<ExprValue>) -> ExprValue>(
        "read_file" to { env, args ->
            val options = optionsStruct(1, args)
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
            val options = optionsStruct(2, args, optionsIndex = 1)
            val fileName = args[0].ionValue.stringValue()
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

    val evaluator = EvaluatingCompiler(ION, replFunctions)

    val globals = when {
        args.isNotEmpty() -> {
            val configSource = File(args[0]).readText(charset("UTF-8"))
            val config = evaluator.compile(configSource).eval(Bindings.empty())
            config.bind(Bindings.empty())
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
