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
import org.partiql.lang.*
import org.partiql.lang.eval.*
import org.partiql.cli.OutputFormat.*
import java.io.*

/**
 * TODO builder, kdoc
 */
internal class Cli(private val valueFactory: ExprValueFactory,
                   private val input: InputStream,
                   private val output: OutputStream,
                   private val format: OutputFormat,
                   private val compilerPipeline: CompilerPipeline,
                   private val globals: Bindings,
                   private val query: String) : SqlCommand() {
    override fun run() {
        val inputExprValue = valueFactory.newBag(valueFactory.ion.iterate(input).asSequence().map { valueFactory.newFromIonValue(it) })

        val bindings = Bindings.buildLazyBindings {
            addBinding("input_data") { inputExprValue }
        }.delegate(globals)

        val writer = when (format) {
            TEXT   -> valueFactory.ion.newTextWriter(output)
            BINARY -> valueFactory.ion.newBinaryWriter(output)
        }

        val result = compilerPipeline.compile(query).eval(EvaluationSession.build { globals(bindings) })

        writer.use { writeResult(result, it) }
    }
}
