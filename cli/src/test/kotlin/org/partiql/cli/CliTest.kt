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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.ProjectionIterationBehavior
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.UndefinedVariableBehavior
import org.partiql.pipeline.AbstractPipeline
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

class CliTest {
    private val output = ByteArrayOutputStream()
    private var testFile: File? = null

    @BeforeEach
    fun setUp() {
        output.reset()
        testFile = Files.createTempFile("test", "ion").toFile()
    }

    @AfterEach
    fun cleanTestFile() {
        Files.deleteIfExists(testFile!!.toPath())
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

    @Test
    fun runQueryOnMultipleIonValuesFailure() {
        val query = "SELECT * FROM input_data"
        val input = "1 2"
        assertThrows<java.lang.IllegalStateException> {
            makeCliAndGetResult(query, input)
        }
    }

    @Test
    fun runQueryOnMultipleIonValuesSuccess() {
        val query = "SELECT * FROM input_data"
        val input = "{a:1} {a:2}"
        val expected = "$BAG_ANNOTATION::[{a:1}, {a:2}]"

        val result = makeCliAndGetResult(query, input, wrapIon = true)

        assertAsIon(expected, result)
    }

    @Test
    fun specifyingWrapIonWithPartiQLInput() {
        val query = "SELECT * FROM input_data"
        val input = "{a:1} {a:2}"
        assertThrows<IllegalArgumentException> {
            makeCliAndGetResult(query, input, wrapIon = true, inputFormat = InputFormat.PARTIQL)
        }
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
        val ionInputResult = testFile!!.bufferedReader().use { it.readText() }
        assertAsIon(expected, ionInputResult)

        makeCliAndGetResult(query, input, inputFormat = InputFormat.PARTIQL, outputFormat = OutputFormat.ION_TEXT, output = FileOutputStream(testFile))
        val partiqlInputResult = testFile!!.bufferedReader().use { it.readText() }
        assertAsIon(expected, partiqlInputResult)
    }

    @Test
    fun withoutInput() {
        val query = "1"
        val expected = "1"

        val actual = makeCliAndGetResult(query)

        assertAsIon(expected, actual)
    }

    @Test
    fun withoutInputWithInputDataBindingThrowsException() {
        val query = "SELECT * FROM input_data"
        assertThrows<EvaluationException> {
            makeCliAndGetResult(query)
        }
    }

    @Test
    fun runQueryInPermissiveMode() {
        val pipeline = AbstractPipeline.create(AbstractPipeline.PipelineOptions(typingMode = TypingMode.PERMISSIVE))
        val query = "1 + 'foo'"
        val actual = makeCliAndGetResult(query, pipeline = pipeline)

        assertAsIon("$MISSING_ANNOTATION::null", actual)
    }

    @Test
    fun runWithTypedOpBehaviorLegacy() {
        val pipeline = AbstractPipeline.create(AbstractPipeline.PipelineOptions(typedOpBehavior = TypedOpBehavior.LEGACY))
        val query = "CAST('abcde' as VARCHAR(3));"
        val actual = makeCliAndGetResult(query, pipeline = pipeline)

        assertAsIon("\"abcde\"", actual)
    }

    @Test
    fun runWithTypedOpBehaviorHonorParameters() {
        val pipeline = AbstractPipeline.create(AbstractPipeline.PipelineOptions(typedOpBehavior = TypedOpBehavior.HONOR_PARAMETERS))
        val query = "CAST('abcde' as VARCHAR(3));"
        val actual = makeCliAndGetResult(query, pipeline = pipeline)

        assertAsIon("\"abc\"", actual)
    }

    @Test
    fun runWithProjectionIterationFilterMissingFailure() {
        val pipeline = AbstractPipeline.create(AbstractPipeline.PipelineOptions(projectionIterationBehavior = ProjectionIterationBehavior.FILTER_MISSING))
        val input = "<<{'a': null, 'b': missing, 'c': 1}>>"
        val query = "SELECT a, b, c FROM input_data"
        assertThrows<EvaluationException> {
            makeCliAndGetResult(query, input, pipeline = pipeline, inputFormat = InputFormat.PARTIQL)
        }
    }

    @Test()
    fun runWithProjectionIterationFilterMissingSuccess() {
        val pipeline = AbstractPipeline.create(AbstractPipeline.PipelineOptions(projectionIterationBehavior = ProjectionIterationBehavior.FILTER_MISSING))
        val input = "<<{'a': null, 'b': missing, 'c': 1}>>"
        val query = "SELECT * FROM input_data"
        val actual = makeCliAndGetResult(query, input, pipeline = pipeline, inputFormat = InputFormat.PARTIQL)
        assertAsIon("$BAG_ANNOTATION::[{a:null,c:1}]", actual)
    }

    @Test
    fun runWithProjectionIterationUnfiltered() {
        val pipeline = AbstractPipeline.create(AbstractPipeline.PipelineOptions(projectionIterationBehavior = ProjectionIterationBehavior.UNFILTERED))
        val input = "<<{'a': null, 'b': missing, 'c': 1}>>"
        val query = "SELECT a, b, c FROM input_data"
        val actual = makeCliAndGetResult(query, input, pipeline = pipeline, inputFormat = InputFormat.PARTIQL)
        assertAsIon("$BAG_ANNOTATION::[{a:null,c:1}]", actual)
    }

    @Test
    fun runWithUndefinedVariableError() {
        val pipeline = AbstractPipeline.create(AbstractPipeline.PipelineOptions(undefinedVariableBehavior = UndefinedVariableBehavior.ERROR))
        val input = "<<{'a': 1}>>"
        val query = "SELECT * FROM undefined_variable"
        assertThrows<EvaluationException> {
            makeCliAndGetResult(query, input, pipeline = pipeline, inputFormat = InputFormat.PARTIQL)
        }
    }

    @Test
    fun runWithUndefinedVariableMissing() {
        val pipeline = AbstractPipeline.create(AbstractPipeline.PipelineOptions(undefinedVariableBehavior = UndefinedVariableBehavior.MISSING))
        val input = "<<{'a': 1}>>"
        val query = "SELECT * FROM undefined_variable"
        val actual = makeCliAndGetResult(query, input, pipeline = pipeline, inputFormat = InputFormat.PARTIQL)
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

    @Test
    fun partiqlInputFailure() {
        val query = "SELECT * FROM input_data"
        val input = "<<{'a': 1}, {'b': 1}>>"
        assertThrows<IonException> {
            makeCliAndGetResult(query, input, inputFormat = InputFormat.ION)
        }
    }
}
