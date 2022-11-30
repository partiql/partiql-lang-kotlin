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

import com.amazon.ion.system.IonSystemBuilder
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.partiql.cli.assertAsIon
import org.partiql.cli.makeCliAndGetResult
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.toIonValue
import org.partiql.pipeline.AbstractPipeline
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.UUID

class WriteFileTest {
    private val ion = IonSystemBuilder.standard().build()
    private val valueFactory = ExprValueFactory.standard(ion)
    private val function = WriteFile(valueFactory)
    private val session = EvaluationSession.standard()
    private val pipeline = AbstractPipeline.create(
        AbstractPipeline.PipelineOptions(
            functions = listOf { valueFactory ->
                WriteFile(valueFactory)
            }
        )
    )

    private fun String.exprValue() = valueFactory.newFromIonValue(ion.singleValue(this))

    private fun dirPath(fname: String = "") = "/tmp/partiqltest/$fname"
    private fun readFileFromPath(path: String) = File(path).readText()
    private fun createRandomTmpFilePath(extension: String? = "ion"): String {
        val prefix: UUID = UUID.randomUUID()
        val name = if (extension != null) "$prefix.$extension" else prefix.toString()
        return dirPath(name)
    }

    private val outputStream: OutputStream = ByteArrayOutputStream()

    companion object {
        private const val TRUE_STRING: String = "true"
    }

    /**
     *
     * CONFIG
     *
     */

    @Before
    fun setUp() {
        File(dirPath()).mkdir()
    }

    @After
    fun tearDown() {
        File(dirPath()).deleteRecursively()
    }

    /**
     *
     * UNIT TESTS
     *
     */

    @Test
    fun unit_success_writeIonAsDefault() {
        val filePath = createRandomTmpFilePath()
        val args = listOf("\"$filePath\"", "[1, 2]").map { it.exprValue() }
        function.callWithRequired(session, args).toIonValue(ion)

        val expected = "[1, 2]"

        Assert.assertEquals(ion.loader.load(expected), ion.loader.load(readFileFromPath(filePath)))
    }

    @Test
    fun unit_success_readIon() {
        val filePath = createRandomTmpFilePath()
        val args = listOf("\"$filePath\"", "[1, 2]").map { it.exprValue() }
        val additionalOptions = """{type: "ion"}""".exprValue()
        function.callWithOptional(session, args, additionalOptions).toIonValue(ion)

        val expected = "[1, 2]"

        Assert.assertEquals(ion.loader.load(expected), ion.loader.load(readFileFromPath(filePath)))
    }

    /**
     *
     * INTEGRATION TESTS
     *
     */

    @Test
    fun integration_success_singleValueStruct() {
        // Arrange
        val filePath = createRandomTmpFilePath()
        val query = "write_file('$filePath', SELECT * FROM input_data)"
        val input = "{a: 1}"
        val expected = "$BAG_ANNOTATION::[{a: 1}]"

        // Act
        val cliResponse =
            makeCliAndGetResult(query = query, input = input, output = outputStream, pipeline = pipeline)

        // Assert
        assertAsIon(TRUE_STRING, cliResponse)
        Assert.assertEquals(ion.loader.load(expected), ion.loader.load(readFileFromPath(filePath)))
    }

    @Test
    fun integration_success_nestedValueStruct() {
        // Arrange
        val filePath = createRandomTmpFilePath()
        val query = "write_file('$filePath', SELECT a.b FROM input_data)"
        val input = "{a: {b: 1}}"
        val expected = "$BAG_ANNOTATION::[{b: 1}]"

        // Act
        val cliResponse =
            makeCliAndGetResult(query = query, input = input, output = outputStream, pipeline = pipeline)

        // Assert
        assertAsIon(TRUE_STRING, cliResponse)
        Assert.assertEquals(ion.loader.load(expected), ion.loader.load(readFileFromPath(filePath)))
    }

    @Test
    fun integration_success_nestedValue() {
        // Arrange
        val filePath = createRandomTmpFilePath()
        val query = "write_file('$filePath', SELECT VALUE a FROM input_data)"
        val input = "{a: {b: 1}}"
        val expected = "$BAG_ANNOTATION::[{b: 1}]"

        // Act
        val cliResponse =
            makeCliAndGetResult(query = query, input = input, output = outputStream, pipeline = pipeline)

        // Assert
        assertAsIon(TRUE_STRING, cliResponse)
        Assert.assertEquals(ion.loader.load(expected), ion.loader.load(readFileFromPath(filePath)))
    }

    @Test
    fun integration_success_nestedValueInt() {
        // Arrange
        val filePath = createRandomTmpFilePath()
        val query = "write_file('$filePath', SELECT VALUE a.b FROM input_data)"
        val input = "{a: {b: 1}}"
        val expected = "$BAG_ANNOTATION::[1]"

        // Act
        val cliResponse =
            makeCliAndGetResult(query = query, input = input, output = outputStream, pipeline = pipeline)

        // Assert
        assertAsIon(TRUE_STRING, cliResponse)
        Assert.assertEquals(ion.loader.load(expected), ion.loader.load(readFileFromPath(filePath)))
    }

    @Test
    fun integration_success_nestedValueList() {
        // Arrange
        val filePath = createRandomTmpFilePath()
        val query = "write_file('$filePath', SELECT VALUE a.b FROM input_data)"
        val input = "{a: {b: [ 1, 2 ]}}"
        val expected = "$BAG_ANNOTATION::[[ 1, 2 ]]"

        // Act
        val cliResponse =
            makeCliAndGetResult(query = query, input = input, output = outputStream, pipeline = pipeline)

        // Assert
        assertAsIon(TRUE_STRING, cliResponse)
        Assert.assertEquals(ion.loader.load(expected), ion.loader.load(readFileFromPath(filePath)))
    }

    @Test
    fun integration_success_int() {
        // Arrange
        val filePath = createRandomTmpFilePath()
        val query = "write_file('$filePath', SELECT VALUE a FROM input_data)"
        val input = "{a : 5}"
        val expected = "$BAG_ANNOTATION::[5]"

        // Act
        val cliResponse =
            makeCliAndGetResult(query = query, input = input, output = outputStream, pipeline = pipeline)

        // Assert
        assertAsIon(TRUE_STRING, cliResponse)
        Assert.assertEquals(ion.loader.load(expected), ion.loader.load(readFileFromPath(filePath)))
    }
}
