/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval.io

import com.amazon.ion.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*
import java.io.BufferedReader
import java.io.Reader
import java.io.Writer

/**
 * Provides adapters for delimited input (e.g. TSV/CSV) as lazy sequences of values.
 *
 * Note that this implementation does not (yet) handle various escaping of TSV/CSV files
 * as specified in [RFC-4180](https://tools.ietf.org/html/rfc4180) or what Microsoft Excel
 * specifies in its particular dialect.
 */
object DelimitedValues {
    /** How to convert each element. */
    enum class ConversionMode {
        /** Attempt to parse each value as a scalar, and fall back to string. */
        AUTO {
            override fun convert(ion: IonSystem, raw: String) = try {
                    val converted = ion.singleValue(raw)
                    when (converted) {
                        is IonInt, is IonFloat, is IonDecimal, is IonTimestamp -> converted
                        // if we can't convert the above, we just use the input string as-is
                        else -> ion.newString(raw)
                    }
                } catch (e: IonException) {
                    ion.newString(raw)
                }.exprValue()
        },
        /** Each field is a string. */
        NONE {
            override fun convert(ion: IonSystem, raw: String) = ion.newString(raw).exprValue()
        };

        abstract fun convert(ion: IonSystem, raw: String): ExprValue
    }

    /**
     * Lazily loads a stream of values from a [Reader] into a sequence backed [ExprValue].
     * The [ExprValue] is single pass only.  This does **not** close the [Reader].
     *
     * @param ion The system to use.
     * @param input The input source.
     * @param delimiter The delimiter to use between columns.
     * @param hasHeader Whether the first row of the delimited input defines the columns.
     * @param conversionMode How column text should be converted.
     */
    @JvmStatic
    fun exprValue(ion: IonSystem,
                  input: Reader,
                  delimiter: String,
                  hasHeader: Boolean,
                  conversionMode: ConversionMode): ExprValue {
        val reader = BufferedReader(input)
        val columns: List<String> = when {
            hasHeader -> {
                val line = reader.readLine()
                    ?: throw IllegalArgumentException("Got EOF for header row")

                line.split(delimiter)
            }
            else -> emptyList()
        }

        val seq = generateSequence {
            val line = reader.readLine()
            when (line) {
                null -> null
                else -> {
                    // construct the row
                    val row = ion.newEmptyStruct().apply {
                        line.splitToSequence(delimiter)
                            .forEachIndexed { i, raw ->
                                val name = when {
                                    i < columns.size -> columns[i]
                                    else -> syntheticColumnName(i)
                                }
                                add(name, conversionMode.convert(ion, raw).ionValue)
                            }
                    }.seal()
                    IonExprValue(row)
                }
            }
        }

        return SequenceExprValue(ion, seq)
    }

    // TODO make this configurable
    private fun IonValue.csvStringValue(): String = when (type) {
        // TODO configurable null handling
        IonType.NULL, IonType.BOOL, IonType.INT,
        IonType.FLOAT, IonType.DECIMAL, IonType.TIMESTAMP -> toString()
        IonType.SYMBOL, IonType.STRING -> stringValue() ?: toString()
        // TODO LOB/BLOB support
        else -> throw IllegalArgumentException(
            "Delimited data column must not be $type type"
        )
    }

    /**
     * Writes the given [ExprValue] to the given [Writer] as delimited text.
     * The [ExprValue] **must** have the [OrderBindNames] facet (e.g. result of a `SELECT`
     * expression with a list projection) in order to have the schema necessary to emit
     * the columns in the right order; scalars are not allowed.
     *
     * @param ion The system to use.
     * @param output The output sink.
     * @param value The value to serialize.
     * @param delimiter The column separator.
     * @param newline The newline character.
     * @param writeHeader Whether or not to write the header.
     */
    @JvmStatic
    fun writeTo(ion: IonSystem,
                output: Writer,
                value: ExprValue,
                delimiter: String,
                newline: String,
                writeHeader: Boolean): Unit {
        val nullValue = ion.newNull()
        var names: List<String>? = null
        for (row in value) {
            val colNames = row.asFacet(OrderedBindNames::class.java)?.orderedNames
                ?: throw IllegalArgumentException("Delimited data must be ordered tuple: $row")
            if (names == null) {
                // first row defines column names
                names = colNames

                if (writeHeader) {
                    names.joinTo(output, delimiter)
                    output.write(newline)
                }
            } else if (names != colNames) {
                // mismatch on the tuples
                throw IllegalArgumentException(
                    "Inconsistent row names: $colNames != $names"
                )
            }

            names.map {
                val col = row.bindings[it]?.ionValue ?: nullValue
                col.csvStringValue()
            }.joinTo(output, delimiter)
            output.write(newline)
        }
    }
}
