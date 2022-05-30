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
import com.amazon.ion.system.IonReaderBuilder
import org.apache.commons.csv.CSVFormat
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.io.DelimitedValues
import org.partiql.lang.eval.io.DelimitedValues.ConversionMode
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
                val message = "As of June 2022, PartiQL requires that Ion files contain only a single Ion value for " +
                    "processing. Please consider wrapping multiple values in a list."
                throw IllegalStateException(message)
            }
            value
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
