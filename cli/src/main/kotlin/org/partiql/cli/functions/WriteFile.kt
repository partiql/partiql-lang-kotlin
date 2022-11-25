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
import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.extensions.cli.functions.BaseFunction
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.io.DelimitedValues
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.asIonStruct
import org.partiql.lang.util.booleanValue
import org.partiql.lang.util.stringValue
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

internal class WriteFile(valueFactory: ExprValueFactory) : BaseFunction(valueFactory) {
    override val signature = FunctionSignature(
        name = "write_file",
        requiredParameters = listOf(StaticType.STRING, StaticType.ANY),
        optionalParameter = StaticType.STRUCT,
        returnType = StaticType.BOOL
    )

    companion object {
        @JvmStatic private val PRETTY_ION_WRITER: (ExprValue, OutputStream, IonStruct) -> Unit = { results, out, _ ->
            IonTextWriterBuilder.pretty().build(out).use { w ->
                results.ionValue.writeTo(w)
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
        "ion" to PRETTY_ION_WRITER
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val fileName = required[0].stringValue()
        val fileType = "ion"
        val results = required[1]
        val handler = writeHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")
        return try {
            FileOutputStream(fileName).use {
                handler(results, it, valueFactory.ion.newEmptyStruct())
            }
            valueFactory.newBoolean(true)
        } catch (e: Exception) {
            e.printStackTrace()
            valueFactory.newBoolean(false)
        }
    }

    override fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        val options = opt.ionValue.asIonStruct()
        val fileName = required[0].stringValue()
        val fileType = options["type"]?.stringValue() ?: "ion"
        val results = required[1]
        val handler = writeHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")

        return try {
            FileOutputStream(fileName).use {
                handler(results, it, options)
            }
            valueFactory.newBoolean(true)
        } catch (e: Exception) {
            e.printStackTrace()
            valueFactory.newBoolean(false)
        }
    }
}
