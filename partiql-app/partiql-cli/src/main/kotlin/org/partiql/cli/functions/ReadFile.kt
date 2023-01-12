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

import com.amazon.ion.IonSystem
import org.apache.commons.csv.CSVFormat
import org.partiql.cli.utils.DelimitedFileIterable
import org.partiql.cli.utils.InputSource
import org.partiql.cli.utils.InputSourceIterable
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.io.DelimitedValues.ConversionMode
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import java.io.File

internal class ReadFile(private val ion: IonSystem) : ExprFunction {
    override val signature = FunctionSignature(
        name = "read_file",
        requiredParameters = listOf(StaticType.STRING),
        optionalParameter = StaticType.STRUCT,
        returnType = StaticType.BAG
    )

    companion object {
        fun conversionModeFor(name: String) =
            ConversionMode.values().find { it.name.toLowerCase() == name }
                ?: throw IllegalArgumentException("Unknown conversion: $name")
    }

    private fun fileReadHandler(csvFormat: CSVFormat): (File, Bindings<ExprValue>) -> ExprValue = { input, bindings ->
        val encoding = bindings[BindingName("encoding", BindingCase.SENSITIVE)]?.stringValue() ?: "UTF-8"
        val conversion = bindings[BindingName("conversion", BindingCase.SENSITIVE)]?.stringValue() ?: "none"

        val hasHeader = bindings[BindingName("header", BindingCase.SENSITIVE)]?.booleanValue() ?: false
        val ignoreEmptyLine = bindings[BindingName("ignore_empty_line", BindingCase.SENSITIVE)]?.booleanValue() ?: true
        val ignoreSurroundingSpace = bindings[BindingName("ignore_surrounding_space", BindingCase.SENSITIVE)]?.booleanValue() ?: true
        val trim = bindings[BindingName("trim", BindingCase.SENSITIVE)]?.booleanValue() ?: true
        val delimiter = bindings[BindingName("delimiter", BindingCase.SENSITIVE)]?.stringValue()?.first() // CSVParser library only accepts a single character as delimiter
        val record = bindings[BindingName("line_breaker", BindingCase.SENSITIVE)]?.stringValue()
        val escape = bindings[BindingName("escape", BindingCase.SENSITIVE)]?.stringValue()?.first() // CSVParser library only accepts a single character as escape
        val quote = bindings[BindingName("quote", BindingCase.SENSITIVE)]?.stringValue()?.first() // CSVParser library only accepts a single character as quote

        val csvFormatWithOptions = csvFormat.withIgnoreEmptyLines(ignoreEmptyLine)
            .withIgnoreSurroundingSpaces(ignoreSurroundingSpace)
            .withTrim(trim)
            .let { if (hasHeader) it.withFirstRecordAsHeader() else it }
            .let { if (delimiter != null) it.withDelimiter(delimiter) else it }
            .let { if (record != null) it.withRecordSeparator(record) else it }
            .let { if (escape != null) it.withEscape(escape) else it }
            .let { if (quote != null) it.withQuote(quote) else it }

        val inputSource = InputSource.FileSource(input)
        val iterable = DelimitedFileIterable(ion, inputSource, csvFormatWithOptions, encoding, conversion)
        iterable.use { ExprValue.newBag(it) }
    }

    private fun ionReadHandler(): (File, Bindings<ExprValue>) -> ExprValue = { file, bindings ->
        when (bindings[BindingName("wrap-ion", BindingCase.SENSITIVE)]?.booleanValue() ?: false) {
            true -> {
                val inputSource = InputSource.FileSource(file)
                val iterable = InputSourceIterable(ion, inputSource)
                iterable.use { iter -> ExprValue.newBag(iter) }
            }
            false -> {
                file.inputStream().use { stream ->
                    ion.newReader(stream).use { reader ->
                        when (reader.next()) {
                            null -> ExprValue.missingValue
                            else -> ExprValue.newFromIonReader(ion, reader)
                        }.also {
                            if (reader.next() != null) {
                                val message = "PartiQL requires that Ion files contain only a single Ion value for " +
                                    "processing. Please consider using option 'wrap-ion'."
                                throw IllegalStateException(message)
                            }
                        }
                    }
                }
            }
        }
    }

    private val readHandlers = mapOf(
        "ion" to ionReadHandler(),
        "csv" to fileReadHandler(CSVFormat.DEFAULT),
        "tsv" to fileReadHandler(CSVFormat.DEFAULT.withDelimiter('\t')),
        "excel_csv" to fileReadHandler(CSVFormat.EXCEL),
        "mysql_csv" to fileReadHandler(CSVFormat.MYSQL),
        "postgresql_csv" to fileReadHandler(CSVFormat.POSTGRESQL_CSV),
        "postgresql_text" to fileReadHandler(CSVFormat.POSTGRESQL_TEXT),
        "customized" to fileReadHandler(CSVFormat.DEFAULT)
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val fileName = required[0].stringValue()
        val fileType = "ion"
        val handler: (File, Bindings<ExprValue>) -> ExprValue = readHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")
        val fileInput = File(fileName)
        return handler(fileInput, Bindings.empty())
    }

    override fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        val fileName = required[0].stringValue()
        val fileType = opt.bindings[BindingName("type", BindingCase.SENSITIVE)]?.stringValue() ?: "ion"
        val handler = readHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")
        val fileInput = File(fileName)
        return handler(fileInput, opt.bindings)
    }
}
