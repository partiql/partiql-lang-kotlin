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

import com.amazon.ion.*
import com.amazon.ion.system.*
import java.io.*
import org.partiql.cli.OutputFormat.*
import org.partiql.lang.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.ConfigurableExprValueFormatter

/**
 * TODO builder, kdoc
 */
internal class Cli(private val valueFactory: ExprValueFactory,
                   private val input: InputStream,
                   private val output: OutputStream,
                   private val format: OutputFormat,
                   private val compilerPipeline: CompilerPipeline,
                   private val globals: Bindings<ExprValue>,
                   private val query: String) : PartiQLCommand {

    companion object {
        val ionTextWriterBuilder: IonTextWriterBuilder = IonTextWriterBuilder.standard().withWriteTopLevelValuesOnNewLines(true)
    }

    override fun run() {
        IonReaderBuilder.standard().build(input).use { reader ->
            val inputIonValue = valueFactory.ion.iterate(reader).asSequence().map { valueFactory.newFromIonValue(it) }
            val inputExprValue = valueFactory.newBag(inputIonValue)
            val bindings = Bindings.buildLazyBindings<ExprValue> {
                addBinding("input_data") { inputExprValue }
            }.delegate(globals)

            val result = compilerPipeline.compile(query).eval(EvaluationSession.build { globals(bindings) })

            when (format) {
                ION_TEXT   -> {
                    ionTextWriterBuilder.build(output).use { printIon(it, result) }
                    output.write(System.lineSeparator().toByteArray(Charsets.UTF_8))
                }
                ION_BINARY -> valueFactory.ion.newBinaryWriter(output).use { printIon(it, result) }
                PARTIQL    -> OutputStreamWriter(output).use { it.write(result.toString()) }
                PARTIQL_PRETTY -> OutputStreamWriter(output).use {
                    ConfigurableExprValueFormatter.pretty.formatTo(result, it)
                }
            }
        }
    }

    private fun printIon(ionWriter: IonWriter, value: ExprValue) {
        when (value.type) {
            // writes top level bags as a datagram
            ExprValueType.BAG -> value.iterator().forEach { v -> v.ionValue.writeTo(ionWriter) }
            else              -> value.ionValue.writeTo(ionWriter)
        }
    }
}
