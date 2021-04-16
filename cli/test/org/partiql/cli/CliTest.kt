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

import com.amazon.ion.system.*
import org.partiql.lang.eval.*
import org.junit.*
import org.junit.Assert.*
import org.partiql.lang.*
import java.io.*

class CliTest {
    private val ion = IonSystemBuilder.standard().build()
    private val valueFactory = ExprValueFactory.standard(ion)
    private val output = ByteArrayOutputStream()
    private val compilerPipeline = CompilerPipeline.standard(ion)

    @Before
    fun setUp() {
        output.reset()
    }

    private fun makeCli(query: String,
                        input: String, bindings: Bindings<ExprValue> = Bindings.empty(),
                        outputFormat: OutputFormat = OutputFormat.ION_TEXT) =
        Cli(
            valueFactory,
            input.byteInputStream(Charsets.UTF_8),
            output,
            outputFormat,
            compilerPipeline,
            bindings,
            query)

    private fun Cli.runAndOutput(): String {
        run()
        return output.toString(Charsets.UTF_8.name())
    }

    private fun String.singleIonExprValue() = valueFactory.newFromIonValue(ion.singleValue(this))
    private fun Map<String, String>.asBinding() =
        Bindings.ofMap(this.mapValues { it.value.singleIonExprValue() })

    private fun assertAsIon(expected: String, actual: String) =
        assertEquals(ion.loader.load(expected), ion.loader.load(actual))

    @Test
    fun runQueryOnSingleValue() {
        val subject = makeCli("SELECT * FROM input_data", "{a: 1}")
        val actual = subject.runAndOutput()

        assertAsIon("{a: 1}", actual)
    }

    @Test
    fun runQueryOnMultipleValues() {
        val subject = makeCli("SELECT * FROM input_data", "{a: 1}{a: 2}{a: 3}")
        val actual = subject.runAndOutput()

        assertAsIon("{a: 1} {a: 2} {a: 3}", actual)
    }

    @Test
    fun caseInsensitiveBindingName() {
        val subject = makeCli("SELECT * FROM input_DAta", "{a: 1}")
        val actual = subject.runAndOutput()

        assertAsIon("{a: 1}", actual)
    }

    @Test
    fun caseSensitiveBindingName() = try {
        val subject = makeCli("SELECT * FROM \"input_DAta\"", "{a: 1}")
        subject.run()
    } catch (e: EvaluationException) {
        assertEquals("No such binding: input_DAta", e.message)
    }

    @Test
    fun withBinding() {
        val subject = makeCli("SELECT v, d FROM bound_value v, input_data d", "{a: 1}", mapOf("bound_value" to "{b: 1}").asBinding())
        val actual = subject.runAndOutput()

        assertAsIon("{v: {b: 1}, d: {a: 1}}", actual)
    }

    @Test
    fun withShadowingBinding() {
        val subject = makeCli("SELECT * FROM input_data", "{a: 1}", mapOf("input_data" to "{b: 1}").asBinding())

        val actual = subject.runAndOutput()

        assertAsIon("{a: 1}", actual)
    }

    @Test
    fun withPartiQLOutput() {
        val subject = makeCli("SELECT * FROM input_data", "{a: 1}", outputFormat = OutputFormat.PARTIQL)
        val actual = subject.runAndOutput()

        assertEquals("<<{'a': 1}>>", actual)
    }

    @Test
    fun withPartiQLPrettyOutput() {
        val subject = makeCli("SELECT * FROM input_data", "{a: 1, b: 2}", outputFormat = OutputFormat.PARTIQL_PRETTY)
        val actual = subject.runAndOutput()

        assertEquals("<<\n  {\n    'a': 1,\n    'b': 2\n  }\n>>", actual)
    }

    @Test
    fun withIonTextOutput() {
        val subject = makeCli("SELECT * FROM input_data", "{a: 1} {b: 1}", outputFormat = OutputFormat.ION_TEXT)
        val actual = subject.runAndOutput()

        assertEquals("{a:1}\n{b:1}\n", actual)
    }
}
