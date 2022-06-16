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

package org.partiql.cli.functions

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonSystemBuilder
import org.apache.commons.csv.CSVFormat
import org.partiql.extensions.cli.functions.BaseFunction
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.BaseExprValue
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.io.DelimitedValues
import org.partiql.lang.eval.io.DelimitedValues.ConversionMode
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.asIonStruct
import org.partiql.lang.util.booleanValue
import org.partiql.lang.util.stringValue
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader

internal class ReadFile(valueFactory: ExprValueFactory) : BaseFunction(valueFactory) {
    override val signature = FunctionSignature(
        name = "read_file",
        requiredParameters = listOf(StaticType.STRING),
        optionalParameter = StaticType.STRUCT,
        returnType = StaticType.BAG
    )

    private fun conversionModeFor(name: String) =
        ConversionMode.values().find { it.name.toLowerCase() == name }
            ?: throw IllegalArgumentException("Unknown conversion: $name")

    private fun fileReadHandler(csvFormat: CSVFormat): (InputStream, IonStruct) -> ExprValue = { input, options ->
        val encoding = options["encoding"]?.stringValue() ?: "UTF-8"
        val reader = InputStreamReader(input, encoding)
        val conversion = options["conversion"]?.stringValue() ?: "none"

        val hasHeader = options["header"]?.booleanValue() ?: false
        val ignoreEmptyLine = options["ignore_empty_line"]?.booleanValue() ?: true
        val ignoreSurroundingSpace = options["ignore_surrounding_space"]?.booleanValue() ?: true
        val trim = options["trim"]?.booleanValue() ?: true
        val delimiter = options["delimiter"]?.stringValue()?.first() // CSVParser library only accepts a single character as delimiter
        val record = options["line_breaker"]?.stringValue()
        val escape = options["escape"]?.stringValue()?.first() // CSVParser library only accepts a single character as escape
        val quote = options["quote"]?.stringValue()?.first() // CSVParser library only accepts a single character as quote

        val csvFormatWithOptions = csvFormat.withIgnoreEmptyLines(ignoreEmptyLine)
            .withIgnoreSurroundingSpaces(ignoreSurroundingSpace)
            .withTrim(trim)
            .let { if (hasHeader) it.withFirstRecordAsHeader() else it }
            .let { if (delimiter != null) it.withDelimiter(delimiter) else it }
            .let { if (record != null) it.withRecordSeparator(record) else it }
            .let { if (escape != null) it.withEscape(escape) else it }
            .let { if (quote != null) it.withQuote(quote) else it }
        val seq = Sequence {
            DelimitedValues.exprValue(valueFactory, reader, csvFormatWithOptions, conversionModeFor(conversion)).iterator()
        }
        valueFactory.newBag(seq)
    }

    private fun ionReadHandler(): (InputStream, IonStruct) -> ExprValue = { input, _ ->
        IonReaderBuilder.standard().build(input).use { reader ->
            val value = when (reader.next()) {
                null -> valueFactory.missingValue
                else -> valueFactory.newFromIonReader(reader)
            }
            if (reader.next() != null) {
                val message = "As of v0.7.0, PartiQL requires that Ion files contain only a single Ion value for " +
                    "processing. Please consider wrapping multiple values in a list."
                throw IllegalStateException(message)
            }
            value
        }
    }

    class MDRowExprValue(private val valueFactory: ExprValueFactory, private val rowString: String, private val header: List<String>) :
        BaseExprValue() {
        /** The Ion type that MarkdownRowExprValue is must similar to is a struct. */
        override val type: ExprValueType
            get() = ExprValueType.STRUCT

        /** Ion representation of the current value. */
        override val ionValue: IonValue
            get() = lazyIonValue

        /** Laziness is even more important here because [ionValue] is likely to never be called. */
        private val lazyIonValue by lazy {
            valueFactory.ion.newEmptyStruct().apply {
                rowValues.map { kvp ->
                    if (kvp.value.scalar.numberValue() != null) {
                        add(kvp.key, valueFactory.ion.newInt(kvp.value.scalar.numberValue()))
                    } else {
                        add(kvp.key, valueFactory.ion.newString(kvp.value.scalar.stringValue()))
                    }
                }
                makeReadOnly()
            }
        }

        /**
         * The lazily constructed set of row values.  Laziness is good practice here because it avoids
         * constructing this map if it isn't needed.
         */
        private val rowValues: Map<String, ExprValue> by lazy {
            rowString.trim().drop(1).dropLast(1)
                .split('|')
                .mapIndexed { i, it ->
                    val fieldName = header[i]

                    val value = it.trim()
                    // Note that we invoke
                    if (value[0] == '\"') {
                        fieldName to valueFactory.newString(value.drop(1).dropLast(1)).namedValue(valueFactory.newString(fieldName))
                    } else {
                        fieldName to valueFactory.newInt(value.toInt()).namedValue(valueFactory.newString(fieldName))
                    }
                }.toMap()
        }

        /** An iterator over the values contained in this instance of [ExprValue], if any. */
        override fun iterator() = rowValues.values.iterator()

        private val bindingsInstance by lazy {
            Bindings.ofMap(rowValues)
        }

        override val bindings: Bindings<ExprValue>
            get() = bindingsInstance
    }
    /**
     * The current supported format for readFile function is
     * read_file('simple.csv', {'type':'csv'});
     * Extend the support to markdown
     */
    private fun mdReadHandler(): (InputStream, IonStruct) -> ExprValue = { input, options ->
        val reader = InputStreamReader(input)
        val ion = IonSystemBuilder.standard().build()
        val valueFactory = ExprValueFactory.standard(ion)
        val pipeline = CompilerPipeline.standard(valueFactory)
        // get header
        val rowData = reader.readLines()
        val header = rowData[0]
            .trim()
            .drop(1)
            .dropLast(1)
            .split('|')
            .map { it ->
                it.trim()
            }

        // a bag is a sequence of ExprValue, so create a Mdrowexprvalue class
        valueFactory.newBag(
            rowData.asSequence()
                .filter { it -> it.isNotEmpty() }
                .drop(2)
                .map { it ->
                    MDRowExprValue(pipeline.valueFactory, it, header)
                }
        )
    }

    private val readHandlers = mapOf(
        "ion" to ionReadHandler(),
        "csv" to fileReadHandler(CSVFormat.DEFAULT),
        "tsv" to fileReadHandler(CSVFormat.DEFAULT.withDelimiter('\t')),
        "excel_csv" to fileReadHandler(CSVFormat.EXCEL),
        "mysql_csv" to fileReadHandler(CSVFormat.MYSQL),
        "postgresql_csv" to fileReadHandler(CSVFormat.POSTGRESQL_CSV),
        "postgresql_text" to fileReadHandler(CSVFormat.POSTGRESQL_TEXT),
        "customized" to fileReadHandler(CSVFormat.DEFAULT),
        "md" to mdReadHandler()
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val fileName = required[0].stringValue()
        val fileType = "ion"
        val handler = readHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")
        // TODO we should take care to clean up this `FileInputStream` properly
        //  https://github.com/partiql/partiql-lang-kotlin/issues/518
        val fileInput = FileInputStream(fileName)
        return handler(fileInput, valueFactory.ion.newEmptyStruct())
    }

    override fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        val options = opt.ionValue.asIonStruct()
        val fileName = required[0].stringValue()
        val fileType = options["type"]?.stringValue() ?: "ion"
        val handler = readHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")
        // TODO we should take care to clean up this `FileInputStream` properly
        //  https://github.com/partiql/partiql-lang-kotlin/issues/518
        val fileInput = FileInputStream(fileName)
        return handler(fileInput, options)
    }
}
