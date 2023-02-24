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
import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.io.DelimitedValues
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.toIonValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.spi.types.StaticType
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

internal class WriteFile(private val ion: IonSystem) : ExprFunction {
    override val signature = FunctionSignature(
        name = "write_file",
        requiredParameters = listOf(StaticType.STRING, StaticType.ANY),
        optionalParameter = StaticType.STRUCT,
        returnType = StaticType.BOOL
    )

    private val PRETTY_ION_WRITER: (ExprValue, OutputStream, Bindings<ExprValue>) -> Unit = { results, out, _ ->
        IonTextWriterBuilder.pretty().build(out).use { w ->
            results.toIonValue(ion).writeTo(w)
        }
    }

    private fun delimitedWriteHandler(delimiter: Char): (ExprValue, OutputStream, Bindings<ExprValue>) -> Unit = { results, out, bindings ->
        val encoding = bindings[BindingName("encoding", BindingCase.SENSITIVE)]?.stringValue() ?: "UTF-8"
        val writeHeader = bindings[BindingName("header", BindingCase.SENSITIVE)]?.booleanValue() ?: false
        val nl = bindings[BindingName("nl", BindingCase.SENSITIVE)]?.stringValue() ?: "\n"

        val writer = OutputStreamWriter(out, encoding)
        writer.use {
            DelimitedValues.writeTo(ion, writer, results, delimiter, nl, writeHeader)
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
                handler(results, it, Bindings.empty())
            }
            ExprValue.newBoolean(true)
        } catch (e: Exception) {
            e.printStackTrace()
            ExprValue.newBoolean(false)
        }
    }

    override fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        val fileName = required[0].stringValue()
        val fileType = opt.bindings[BindingName("type", BindingCase.SENSITIVE)]?.stringValue() ?: "ion"
        val results = required[1]
        val handler = writeHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")

        return try {
            FileOutputStream(fileName).use {
                handler(results, it, opt.bindings)
            }
            ExprValue.newBoolean(true)
        } catch (e: Exception) {
            e.printStackTrace()
            ExprValue.newBoolean(false)
        }
    }
}
