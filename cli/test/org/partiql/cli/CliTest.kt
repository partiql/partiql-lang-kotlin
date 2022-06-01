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

import com.amazon.ion.system.IonSystemBuilder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.TypingMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

class CliTest {
    private val ion = IonSystemBuilder.standard().build()
    private val output = ByteArrayOutputStream()
    private val testFile = File("test.ion")
    private val partiqlBagAnnotation = "\$partiql_bag::"

    @Before
    fun setUp() {
        output.reset()
    }

    @After
    fun cleanTestFile() {
        Files.deleteIfExists(testFile.toPath())
    }

    @Test
    fun runQueryOnSingleValue() {
        val query = "SELECT * FROM input_data"
        val input = "[{'a': 1}]"
        val expected = "$partiqlBagAnnotation[{a: 1}]"

        val ionInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION)
        val partiqlInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.PARTIQL)

        assertAsIon(expected, ionInputResult)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun runQueryOnBadValue() {
        val query = "SELECT * FROM input_data"
        val input = "1 2"
        makeCliAndGetResult(query, input)
    }

    @Test
    fun runQueryOnMultipleValues() {
        val query = "SELECT * FROM input_data"
        val input = "[{'a': 1},{'a': 2},{'a': 3}]"
        val expected = "$partiqlBagAnnotation[{a: 1},{a: 2},{a: 3}]"

        val ionInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION)
        val partiqlInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.PARTIQL)

        assertAsIon(expected, ionInputResult)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test
    fun caseInsensitiveBindingName() {
        val query = "SELECT * FROM input_dAta"
        val input = "[{'a': 1}]"
        val expected = "$partiqlBagAnnotation[{a: 1}]"

        val ionInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION)
        val partiqlInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.PARTIQL)

        assertAsIon(expected, ionInputResult)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test
    fun withBinding() {
        val query = "SELECT v, d FROM bound_value v, input_data d"
        val input = "[{'a': 1}]"
        val bindings = mapOf("bound_value" to "{b: 1}").asBinding()
        val expected = "$partiqlBagAnnotation[{v: {b: 1}, d: {a: 1}}]"

        val ionInputResult = makeCliAndGetResult(query, input, bindings = bindings, inputFormat = InputFormat.ION)
        val partiqlInputResult = makeCliAndGetResult(query, input, bindings = bindings, inputFormat = InputFormat.PARTIQL)

        assertAsIon(expected, ionInputResult)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test
    fun withShadowingBinding() {
        val query = "SELECT * FROM input_data"
        val input = "[{'a': 1}]"
        val bindings = mapOf("input_data" to "{b: 1}").asBinding()
        val expected = "$partiqlBagAnnotation[{a: 1}]"

        val ionInputResult = makeCliAndGetResult(query, input, bindings = bindings, inputFormat = InputFormat.ION)
        val partiqlInputResult = makeCliAndGetResult(query, input, bindings = bindings, inputFormat = InputFormat.PARTIQL)

        assertAsIon(expected, ionInputResult)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test
    fun withPartiQLOutput() {
        val query = "SELECT * FROM input_data"
        val input = "[{a: 1}]"
        val expected = "<<{'a': 1}>>"

        val actual = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION, outputFormat = OutputFormat.PARTIQL)

        assertEquals(expected, actual)
    }

    @Test
    fun withPartiQLPrettyOutput() {
        val query = "SELECT * FROM input_data"
        val input = "[{a: 1, b: 2}]"
        val expected = "<<\n  {\n    'a': 1,\n    'b': 2\n  }\n>>"

        val actual = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION, outputFormat = OutputFormat.PARTIQL_PRETTY)

        assertEquals(expected, actual)
    }

    @Test
    fun withIonTextOutput() {
        val query = "SELECT * FROM input_data"
        val input = "[{a: 1}, {b: 1}]"
        val expected = "$partiqlBagAnnotation[{a:1}\n,{b:1}\n]"

        val actual = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION, outputFormat = OutputFormat.ION_TEXT)

        assertAsIon(expected, actual)
    }

    @Test
    fun withIonTextOutputToFile() {
        val query = "SELECT * FROM input_data"
        val input = "[{'a': 1}, {'b': 1}]"
        val expected = "$partiqlBagAnnotation[{a:1}\n,{b:1}\n]"

        makeCliAndGetResult(query, input, inputFormat = InputFormat.ION, outputFormat = OutputFormat.ION_TEXT, output = FileOutputStream(testFile))
        val ionInputResult = testFile.bufferedReader().use { it.readText() }
        assertAsIon(expected, ionInputResult)

        makeCliAndGetResult(query, input, inputFormat = InputFormat.PARTIQL, outputFormat = OutputFormat.ION_TEXT, output = FileOutputStream(testFile))
        val partiqlInputResult = testFile.bufferedReader().use { it.readText() }
        assertAsIon(expected, partiqlInputResult)
    }

    @Test
    fun withoutInput() {
        val query = "1"
        val expected = "1"

        val actual = makeCliAndGetResult(query)

        assertAsIon(expected, actual)
    }

    @Test(expected = EvaluationException::class)
    fun withoutInputWithInputDataBindingThrowsException() {
        val query = "SELECT * FROM input_data"
        makeCliAndGetResult(query)
    }

    @Test
    fun runQueryInPermissiveMode() {
        val permissiveModeCP = CompilerPipeline.build(ion) {
            compileOptions {
                typingMode(TypingMode.PERMISSIVE)
            }
        }
        val query = "1 + 'foo'"
        val actual = makeCliAndGetResult(query, compilerPipeline = permissiveModeCP)

        assertAsIon("\$partiql_missing::null", actual)
    }
}
