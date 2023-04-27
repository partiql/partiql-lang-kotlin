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

package org.partiql.cli.query

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.cli.format.ExplainFormatter
import org.partiql.cli.pico.PartiQLCommand
import org.partiql.cli.pipeline.AbstractPipeline
import org.partiql.cli.utils.EmptyInputStream
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.delegate
import org.partiql.lang.eval.toIonValue
import org.partiql.lang.util.ConfigurableExprValueFormatter
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

internal class Cli(
    private val ion: IonSystem,
    private val input: InputStream,
    private val inputFormat: PartiQLCommand.InputFormat,
    private val output: OutputStream,
    private val outputFormat: PartiQLCommand.OutputFormat,
    private val compilerPipeline: AbstractPipeline,
    private val globals: Bindings<ExprValue>,
    private val query: String,
    private val wrapIon: Boolean
) {
    private val currentUser = System.getProperty("user.name")

    init {
        if (wrapIon && inputFormat != PartiQLCommand.InputFormat.ION) {
            throw IllegalArgumentException("Specifying --wrap-ion requires that the input format be ${PartiQLCommand.InputFormat.ION}.")
        }
    }

    companion object {
        val ionTextWriterBuilder: IonTextWriterBuilder = IonTextWriterBuilder.standard()
            .withWriteTopLevelValuesOnNewLines(true)
    }

    internal fun run() {
        when (inputFormat) {
            PartiQLCommand.InputFormat.ION -> runWithIonInput()
            PartiQLCommand.InputFormat.PARTIQL -> runWithPartiQLInput()
        }
    }

    private fun runWithIonInput() {
        when (wrapIon) {
            true -> runWithIonInputWrapped()
            false -> runWithIonInputDefault()
        }
    }

    private fun runWithIonInputDefault() {
        IonReaderBuilder.standard().build(input).use { reader ->
            val bindings = when (reader.next()) {
                null -> Bindings.buildLazyBindings<ExprValue> {}.delegate(globals)
                else -> getBindingsFromIonValue(ExprValue.newFromIonReader(ion, reader))
            }
            if (reader.next() != null) {
                val message = "As of v0.7.0, PartiQL requires that Ion files contain only a single Ion value for " +
                    "processing. Please consider wrapping multiple values in a list, or consider passing in the " +
                    "--wrap-ion flag. Use --help for more information."
                throw IllegalStateException(message)
            }
            val result = compilerPipeline.compile(
                query,
                EvaluationSession.build {
                    globals(bindings)
                    user(currentUser)
                }
            )
            outputResult(result)
        }
    }

    private fun runWithIonInputWrapped() {
        IonReaderBuilder.standard().build(input).use { reader ->
            val inputIonValue = ion.iterate(reader).asSequence().map { ExprValue.of(it) }
            val inputExprValue = ExprValue.newBag(inputIonValue)
            val bindings = getBindingsFromIonValue(inputExprValue)
            val result = compilerPipeline.compile(
                query,
                EvaluationSession.build {
                    globals(bindings)
                    user(currentUser)
                }
            )
            outputResult(result)
        }
    }

    private fun runWithPartiQLInput() {
        val inputEnvironment = compilerPipeline.compile(
            input.readBytes().toString(Charsets.UTF_8),
            EvaluationSession.build {
                user(currentUser)
            }
        ) as PartiQLResult.Value
        val bindings = getBindingsFromIonValue(inputEnvironment.value)
        val result = compilerPipeline.compile(
            query,
            EvaluationSession.build {
                globals(bindings)
                user(currentUser)
            }
        )
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
            PartiQLCommand.OutputFormat.ION_TEXT -> ionTextWriterBuilder.build(output).use { result.toIonValue(ion).writeTo(it) }
            PartiQLCommand.OutputFormat.ION_BINARY -> ion.newBinaryWriter(output).use { result.toIonValue(ion).writeTo(it) }
            PartiQLCommand.OutputFormat.PARTIQL -> OutputStreamWriter(output).use { it.write(result.toString()) }
            PartiQLCommand.OutputFormat.PARTIQL_PRETTY -> OutputStreamWriter(output).use {
                ConfigurableExprValueFormatter.pretty.formatTo(result, it)
            }
        }
    }

    private fun outputResult(result: PartiQLResult) {
        when (result) {
            is PartiQLResult.Value -> outputResult(result.value)
            is PartiQLResult.Delete,
            is PartiQLResult.Replace,
            is PartiQLResult.Insert -> TODO("Delete, Replace, and Insert do not have CLI support yet.")
            is PartiQLResult.Explain.Domain -> {
                OutputStreamWriter(output).use {
                    it.append(ExplainFormatter.format(result))
                }
            }
        }
    }
}
