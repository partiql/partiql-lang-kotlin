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
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.io.DelimitedValues
import org.partiql.lang.eval.io.DelimitedValues.ConversionMode
import org.partiql.lang.eval.stringValue
import org.partiql.lang.util.booleanValue
import org.partiql.lang.util.stringValue
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader

internal class ReadFile(valueFactory: ExprValueFactory) : BaseFunction(valueFactory) {
    override val name = "read_file"

    private fun conversionModeFor(name: String) =
        ConversionMode.values().find { it.name.toLowerCase() == name } ?:
        throw IllegalArgumentException( "Unknown conversion: $name")

    private fun delimitedReadHandler(delimiter: Char): (InputStream, IonStruct) -> ExprValue = { input, options ->
        val encoding = options["encoding"]?.stringValue() ?: "UTF-8"
        val conversion = options["conversion"]?.stringValue() ?: "none"
        val hasHeader = options["header"]?.booleanValue() ?: false

        val reader = InputStreamReader(input, encoding)

        DelimitedValues.exprValue(valueFactory, reader, delimiter, hasHeader, conversionModeFor(conversion))
    }

    private fun ionReadHandler(): (InputStream, IonStruct) -> ExprValue = { input, _ ->
        valueFactory.newBag(valueFactory.ion.iterate(input).asSequence().map { valueFactory.newFromIonValue(it) })
    }

    private val readHandlers = mapOf(
        "ion" to ionReadHandler(),
        "tsv" to delimitedReadHandler('\t'),
        "csv" to delimitedReadHandler(','))

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        val options = optionsStruct(1, args)
        val fileName = args[0].stringValue()
        val fileType = options["type"]?.stringValue() ?: "ion"
        val handler = readHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")
        val seq = Sequence {
            // TODO we should take care to clean this up properly
            val fileInput = FileInputStream(fileName)
            handler(fileInput, options).iterator()
        }
        return valueFactory.newBag(seq)
    }
}

