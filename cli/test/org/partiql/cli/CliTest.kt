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

import com.amazon.ion.IonException
import com.amazon.ion.system.IonSystemBuilder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.ProjectionIterationBehavior
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.UndefinedVariableBehavior
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

class CliTest {
    private val ion = IonSystemBuilder.standard().build()
    private val output = ByteArrayOutputStream()
    private val testFile = File("test.ion")

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
        val expected = "$BAG_ANNOTATION::[{a: 1}]"

        val ionInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION)
        val partiqlInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.PARTIQL)

        assertAsIon(expected, ionInputResult)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun runQueryOnMultipleIonValuesFailure() {
        val query = "SELECT * FROM input_data"
        val input = "1 2"
        makeCliAndGetResult(query, input)
    }

    @Test
    fun runQueryOnMultipleIonValuesSuccess() {
        val query = "SELECT * FROM input_data"
        val input = "{a:1} {a:2}"
        val expected = "$BAG_ANNOTATION::[{a:1}, {a:2}]"

        val result = makeCliAndGetResult(query, input, wrapIon = true)

        assertAsIon(expected, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun specifyingWrapIonWithPartiQLInput() {
        val query = "SELECT * FROM input_data"
        val input = "{a:1} {a:2}"

        makeCliAndGetResult(query, input, wrapIon = true, inputFormat = InputFormat.PARTIQL)
    }

    @Test
    fun runQueryOnMultipleValues() {
        val query = "SELECT * FROM input_data"
        val input = "[{'a': 1},{'a': 2},{'a': 3}]"
        val expected = "$BAG_ANNOTATION::[{a: 1},{a: 2},{a: 3}]"

        val ionInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION)
        val partiqlInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.PARTIQL)

        assertAsIon(expected, ionInputResult)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test
    fun caseInsensitiveBindingName() {
        val query = "SELECT * FROM input_dAta"
        val input = "[{'a': 1}]"
        val expected = "$BAG_ANNOTATION::[{a: 1}]"

        val ionInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION)
        val partiqlInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.PARTIQL)

        assertAsIon(expected, ionInputResult)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test
    fun withBinding() {
        val query = "SELECT v, d FROM bound_value v, input_data d"
        val input = "[{'a': 1}]"
        val wrappedInput = "{'a': 1}"
        val bindings = mapOf("bound_value" to "{b: 1}").asBinding()
        val expected = "$BAG_ANNOTATION::[{v: {b: 1}, d: {a: 1}}]"

        val wrappedInputResult = makeCliAndGetResult(query, wrappedInput, bindings = bindings, wrapIon = true)
        val ionInputResult = makeCliAndGetResult(query, input, bindings = bindings)
        val partiqlInputResult = makeCliAndGetResult(query, input, bindings = bindings, inputFormat = InputFormat.PARTIQL)

        assertAsIon(expected, wrappedInputResult)
        assertAsIon(expected, ionInputResult)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test
    fun withShadowingBinding() {
        val query = "SELECT * FROM input_data"
        val input = "[{'a': 1}]"
        val wrappedInput = "{'a': 1}"
        val bindings = mapOf("input_data" to "{b: 1}").asBinding()
        val expected = "$BAG_ANNOTATION::[{a: 1}]"

        val wrappedInputResult = makeCliAndGetResult(query, wrappedInput, bindings = bindings, wrapIon = true)
        val ionInputResult = makeCliAndGetResult(query, input, bindings = bindings)
        val partiqlInputResult = makeCliAndGetResult(query, input, bindings = bindings, inputFormat = InputFormat.PARTIQL)

        assertAsIon(expected, wrappedInputResult)
        assertAsIon(expected, ionInputResult)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test
    fun withPartiQLOutput() {
        val query = "SELECT * FROM input_data"
        val input = "[{a: 1}]"
        val wrappedInput = "{a: 1}"
        val expected = "<<{'a': 1}>>"

        val wrappedInputResult = makeCliAndGetResult(query, wrappedInput, wrapIon = true, outputFormat = OutputFormat.PARTIQL)
        val ionInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION, outputFormat = OutputFormat.PARTIQL)

        assertEquals(expected, wrappedInputResult)
        assertEquals(expected, ionInputResult)
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
        val expected = "$BAG_ANNOTATION::[{a:1}\n,{b:1}\n]"

        val actual = makeCliAndGetResult(query, input, inputFormat = InputFormat.ION, outputFormat = OutputFormat.ION_TEXT)

        assertAsIon(expected, actual)
    }

    @Test
    fun withIonTextOutputToFile() {
        val query = "SELECT * FROM input_data"
        val input = "[{'a': 1}, {'b': 1}]"
        val expected = "$BAG_ANNOTATION::[{a:1}\n,{b:1}\n]"

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

        assertAsIon("$MISSING_ANNOTATION::null", actual)
    }

    @Test
    fun runWithTypedOpBehaviorLegacy() {
        val pipeline = CompilerPipeline.build(ion) {
            compileOptions {
                typedOpBehavior(TypedOpBehavior.LEGACY)
            }
        }
        val query = "CAST('abcde' as VARCHAR(3));"
        val actual = makeCliAndGetResult(query, compilerPipeline = pipeline)

        assertAsIon("\"abcde\"", actual)
    }

    @Test
    fun runWithTypedOpBehaviorHonorParameters() {
        val pipeline = CompilerPipeline.build(ion) {
            compileOptions {
                typedOpBehavior(TypedOpBehavior.HONOR_PARAMETERS)
            }
        }
        val query = "CAST('abcde' as VARCHAR(3));"
        val actual = makeCliAndGetResult(query, compilerPipeline = pipeline)

        assertAsIon("\"abc\"", actual)
    }

    @Test(expected = EvaluationException::class)
    fun runWithProjectionIterationFilterMissingFailure() {
        val input = "<<{'a': null, 'b': missing, 'c': 1}>>"
        val pipeline = CompilerPipeline.build(ion) {
            compileOptions {
                projectionIteration(ProjectionIterationBehavior.FILTER_MISSING)
            }
        }
        val query = "SELECT a, b, c FROM input_data"
        makeCliAndGetResult(query, input, compilerPipeline = pipeline, inputFormat = InputFormat.PARTIQL)
    }

    @Test()
    fun runWithProjectionIterationFilterMissingSuccess() {
        val input = "<<{'a': null, 'b': missing, 'c': 1}>>"
        val pipeline = CompilerPipeline.build(ion) {
            compileOptions {
                projectionIteration(ProjectionIterationBehavior.FILTER_MISSING)
            }
        }
        val query = "SELECT * FROM input_data"
        val actual = makeCliAndGetResult(query, input, compilerPipeline = pipeline, inputFormat = InputFormat.PARTIQL)
        assertAsIon("$BAG_ANNOTATION::[{a:null,c:1}]", actual)
    }

    @Test
    fun runWithProjectionIterationUnfiltered() {
        val input = "<<{'a': null, 'b': missing, 'c': 1}>>"
        val pipeline = CompilerPipeline.build(ion) {
            compileOptions {
                projectionIteration(ProjectionIterationBehavior.UNFILTERED)
            }
        }
        val query = "SELECT a, b, c FROM input_data"
        val actual = makeCliAndGetResult(query, input, compilerPipeline = pipeline, inputFormat = InputFormat.PARTIQL)

        assertAsIon("$BAG_ANNOTATION::[{a:null,c:1}]", actual)
    }

    @Test(expected = EvaluationException::class)
    fun runWithUndefinedVariableError() {
        val input = "<<{'a': 1}>>"
        val pipeline = CompilerPipeline.build(ion) {
            compileOptions {
                undefinedVariable(UndefinedVariableBehavior.ERROR)
            }
        }
        val query = "SELECT * FROM undefined_variable"
        makeCliAndGetResult(query, input, compilerPipeline = pipeline, inputFormat = InputFormat.PARTIQL)
    }

    @Test()
    fun runWithUndefinedVariableMissing() {
        val input = "<<{'a': 1}>>"
        val pipeline = CompilerPipeline.build(ion) {
            compileOptions {
                undefinedVariable(UndefinedVariableBehavior.MISSING)
            }
        }
        val query = "SELECT * FROM undefined_variable"
        val actual = makeCliAndGetResult(query, input, compilerPipeline = pipeline, inputFormat = InputFormat.PARTIQL)
        assertAsIon("$BAG_ANNOTATION::[{}]", actual)
    }

    @Test
    fun partiqlInputSuccess() {
        val query = "SELECT * FROM input_data"
        val input = "<<{'a': 1}, {'b': 1}>>"
        val expected = "$BAG_ANNOTATION::[{a:1}\n,{b:1}\n]"

        val partiqlInputResult = makeCliAndGetResult(query, input, inputFormat = InputFormat.PARTIQL)
        assertAsIon(expected, partiqlInputResult)
    }

    @Test(expected = IonException::class)
    fun partiqlInputFailure() {
        val query = "SELECT * FROM input_data"
        val input = "<<{'a': 1}, {'b': 1}>>"

        makeCliAndGetResult(query, input, inputFormat = InputFormat.ION)
    }
}
