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

package org.partiql.cli

import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.delegate
import org.partiql.lang.util.ConfigurableExprValueFormatter
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * TODO builder, kdoc
 */
internal class Cli(
    private val valueFactory: ExprValueFactory,
    private val input: InputStream,
    private val inputFormat: InputFormat,
    private val output: OutputStream,
    private val outputFormat: OutputFormat,
    private val compilerPipeline: CompilerPipeline,
    private val globals: Bindings<ExprValue>,
    private val query: String
) : PartiQLCommand {

    companion object {
        val ionTextWriterBuilder: IonTextWriterBuilder = IonTextWriterBuilder.standard()
            .withWriteTopLevelValuesOnNewLines(true)
    }

    override fun run() {
        when (inputFormat) {
            InputFormat.ION -> runWithIonInput()
            InputFormat.PARTIQL -> runWithPartiQLInput()
        }
    }

    private fun runWithIonInput() {
        IonReaderBuilder.standard().build(input).use { reader ->
            val bindings = when (reader.next()) {
                null -> Bindings.buildLazyBindings<ExprValue> {}.delegate(globals)
                else -> getBindingsFromIonValue(valueFactory.newFromIonReader(reader))
            }
            if (reader.next() != null) {
                val message = "As of June 2022, PartiQL requires that Ion files contain only a single Ion value for " +
                    "processing. Please consider wrapping multiple values in a list."
                throw IllegalStateException(message)
            }
            val result = compilerPipeline.compile(query).eval(EvaluationSession.build { globals(bindings) })
            outputResult(result)
        }
    }

    private fun runWithPartiQLInput() {
        val partiql =
            compilerPipeline.compile(input.readBytes().toString(Charsets.UTF_8)).eval(EvaluationSession.standard())
        val bindings = getBindingsFromIonValue(partiql)
        val result = compilerPipeline.compile(query).eval(EvaluationSession.build { globals(bindings) })
        outputResult(result)
    }

    private fun getBindingsFromIonValue(value: ExprValue): Bindings<ExprValue> {
        return Bindings.buildLazyBindings<ExprValue> {
            // If `input` is a class of `EmptyInputStream`, it means there is no input data provided by user.
            if (input !is EmptyInputStream) {
                addBinding("input_data") { value }
            }
        }.delegate(globals)
    }

    private fun outputResult(result: ExprValue) {
        when (outputFormat) {
            OutputFormat.ION_TEXT -> ionTextWriterBuilder.build(output).use { result.ionValue.writeTo(it) }
            OutputFormat.ION_BINARY -> valueFactory.ion.newBinaryWriter(output).use { result.ionValue.writeTo(it) }
            OutputFormat.PARTIQL -> OutputStreamWriter(output).use { it.write(result.toString()) }
            OutputFormat.PARTIQL_PRETTY -> OutputStreamWriter(output).use {
                ConfigurableExprValueFormatter.pretty.formatTo(result, it)
            }
        }
    }
}
