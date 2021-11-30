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

import com.amazon.ion.*
import com.amazon.ion.system.*
import org.partiql.lang.eval.*
import org.partiql.lang.eval.io.*
import org.partiql.lang.util.*
import java.io.*

internal class WriteFile(valueFactory: ExprValueFactory) : BaseFunction(valueFactory) {
    override val name = "write_file"
    companion object {
        @JvmStatic private val PRETTY_ION_WRITER: (ExprValue, OutputStream, IonStruct) -> Unit = { results, out, _ ->
            IonTextWriterBuilder.pretty().build(out).use { w ->
                results.forEach { it.ionValue.writeTo(w) }
            }
        }
    }

    private fun delimitedWriteHandler(delimiter: Char): (ExprValue, OutputStream, IonStruct) -> Unit = { results, out, options ->
        val encoding = options["encoding"]?.stringValue() ?: "UTF-8"
        val writeHeader = options["header"]?.booleanValue() ?: false
        val nl = options["nl"]?.stringValue() ?: "\n"

        val writer = OutputStreamWriter(out, encoding)
        writer.use {
            DelimitedValues.writeTo(valueFactory.ion, writer, results, delimiter, nl, writeHeader)
        }
    }

    private val writeHandlers = mapOf(
        "tsv" to delimitedWriteHandler('\t'),
        "csv" to delimitedWriteHandler(','),
        "ion" to PRETTY_ION_WRITER)

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        val options = optionsStruct(2, args, optionsIndex = 1)
        val fileName = args[0].stringValue()
        val fileType = options["type"]?.stringValue() ?: "ion"
        val resultsIndex = when (args.size) {
            2    -> 1
            else -> 2
        }
        val results = args[resultsIndex]
        val handler = writeHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")

        return try {
            FileOutputStream(fileName).use {
                handler(results, it, options)
            }
            valueFactory.newBoolean(true)
        }
        catch (e: Exception) {
            e.printStackTrace()
            valueFactory.newBoolean(false)
        }
    }
}
