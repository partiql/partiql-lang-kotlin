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

package org.partiql.lang.eval.io

import com.amazon.ion.IonDecimal
import com.amazon.ion.IonException
import com.amazon.ion.IonFloat
import com.amazon.ion.IonInt
import com.amazon.ion.IonSystem
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.orderedNames
import org.partiql.lang.eval.syntheticColumnName
import org.partiql.lang.eval.toIonValue
import org.partiql.lang.util.stringValue
import java.io.BufferedReader
import java.io.Reader
import java.io.Writer

/**
 * Provides adapters for delimited input (e.g. TSV/CSV) as lazy sequences of values.
 *
 * This implementation uses Apache CSVParser library and follows [RFC-4180](https://tools.ietf.org/html/rfc4180) format.
 * The only difference is that it is allowed for each row to have different numbers of fields.
 */
object DelimitedValues {
    /** How to convert each element. */
    enum class ConversionMode {
        /** Attempt to parse each value as a scalar, and fall back to string. */
        AUTO {
            @Deprecated("[ExprValueFactory] is deprecated.", replaceWith = ReplaceWith("convert(raw)"))
            @Suppress("DEPRECATION") // Deprecation of ExprValueFactory.
            override fun convert(valueFactory: org.partiql.lang.eval.ExprValueFactory, raw: String): ExprValue =
                convert(raw)

            override fun convert(raw: String): ExprValue = try {
                when (val converted = IonicParse.simpleIon4ExprValue(raw)) {
                    is IonInt, is IonFloat, is IonDecimal, is IonTimestamp ->
                        ExprValue.of(converted)
                    // if we can't convert the above, we just use the input string as-is
                    else -> ExprValue.newString(raw)
                }
            } catch (e: IonException) {
                ExprValue.newString(raw)
            }
        },
        /** Each field is a string. */
        NONE {
            @Deprecated("[ExprValueFactory] is deprecated.", replaceWith = ReplaceWith("convert(raw)"))
            @Suppress("DEPRECATION") // Deprecation of ExprValueFactory.
            override fun convert(valueFactory: org.partiql.lang.eval.ExprValueFactory, raw: String): ExprValue =
                convert(raw)

            override fun convert(raw: String): ExprValue = ExprValue.newString(raw)
        };

        @Deprecated("[ExprValueFactory] is deprecated.", replaceWith = ReplaceWith("convert(raw)"))
        @Suppress("DEPRECATION") // Deprecation of ExprValueFactory.
        abstract fun convert(valueFactory: org.partiql.lang.eval.ExprValueFactory, raw: String): ExprValue

        abstract fun convert(raw: String): ExprValue
    }

    @JvmStatic
    @Deprecated(
        "Deprecated, because of the deprecated [ExprValueFactory] argument.",
        replaceWith = ReplaceWith("exprValue(input, csvFormat, conversionMode)")
    )
    fun exprValue(
        @Suppress("DEPRECATION", "UNUSED_PARAMETER")
        valueFactory: org.partiql.lang.eval.ExprValueFactory,
        input: Reader,
        csvFormat: CSVFormat,
        conversionMode: ConversionMode
    ): ExprValue = exprValue(input, csvFormat, conversionMode)

    /**
     * Lazily loads a stream of values from a [Reader] into a sequence backed [ExprValue].
     * This does **not** close the [Reader].
     *
     * @param input The input source.
     * @param csvFormat What the format of csv files is.
     * @param conversionMode How column text should be converted.
     */
    @JvmStatic
    fun exprValue(
        input: Reader,
        csvFormat: CSVFormat,
        conversionMode: ConversionMode
    ): ExprValue {
        val reader = BufferedReader(input)
        val csvParser = CSVParser(reader, csvFormat)
        val columns: List<String> = csvParser.headerNames

        val seq = csvParser.asSequence().map { csvRecord ->
            ExprValue.newStruct(
                csvRecord.mapIndexed { i, value ->
                    val name = when {
                        i < columns.size -> columns[i]
                        else -> syntheticColumnName(i)
                    }
                    conversionMode.convert(value).namedValue(ExprValue.newString(name))
                },
                StructOrdering.ORDERED
            )
        }

        return ExprValue.newBag(seq)
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
    fun writeTo(
        ion: IonSystem,
        output: Writer,
        value: ExprValue,
        delimiter: Char,
        newline: String,
        writeHeader: Boolean
    ) {
        CSVPrinter(output, CSVFormat.DEFAULT.withDelimiter(delimiter).withRecordSeparator(newline)).use { csvPrinter ->
            var names: List<String>? = null
            for (row in value) {
                val colNames = row.orderedNames
                    ?: throw IllegalArgumentException("Delimited data must be ordered tuple: $row")
                if (names == null) {
                    // first row defines column names
                    names = colNames
                    if (writeHeader) {
                        csvPrinter.printRecord(names)
                    }
                } else if (names != colNames) { // We need to check if the column names in other rows are all the same as the first one's.
                    throw IllegalArgumentException(
                        "Inconsistent row names: $colNames != $names"
                    )
                }

                csvPrinter.printRecord(
                    names.map {
                        val col = row.bindings[BindingName(it, BindingCase.SENSITIVE)]?.toIonValue(ion) ?: ion.newNull()
                        col.csvStringValue()
                    }
                )
            }
        }
    }
}
