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
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.partiql.cli.assertAsIon
import org.partiql.cli.makeCliAndGetResult
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.toIonValue
import org.partiql.pipeline.AbstractPipeline
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class WriteFileTest {

    private val ion = IonSystemBuilder.standard().build()
    private val function = WriteFile(ion)
    private val session = EvaluationSession.standard()
    private val pipeline = AbstractPipeline.create(
        AbstractPipeline.PipelineOptions(
            functions = listOf(WriteFile(ion))
        )
    )

    private fun String.exprValue() = ExprValue.of(ion.singleValue(this))

    private val outputStream: OutputStream = ByteArrayOutputStream()

    companion object {

        private var tmp: Path? = null

        const val TRUE_STRING = "true"

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            try {
                tmp = Files.createTempDirectory("partiqltest")
            } catch (_: IllegalArgumentException) {
                // already existed
            }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            tmp!!.toFile().deleteRecursively()
        }

        fun createRandomTmpFilePath(): Path = Files.createTempFile(tmp!!, UUID.randomUUID().toString(), ".ion")
    }

    @Test
    fun unit_success_writeIonAsDefault() {
        val filePath = createRandomTmpFilePath()
        val args = listOf("\"$filePath\"", "[1, 2]").map { it.exprValue() }
        function.callWithRequired(session, args).toIonValue(ion)

        val expected = "[1, 2]"

        assertEquals(ion.loader.load(expected), ion.loader.load(filePath.toFile().readText()))
    }

    @Test
    fun unit_success_readIon() {
        val filePath = createRandomTmpFilePath()
        val args = listOf("\"$filePath\"", "[1, 2]").map { it.exprValue() }
        val additionalOptions = """{type: "ion"}""".exprValue()
        function.callWithOptional(session, args, additionalOptions).toIonValue(ion)

        val expected = "[1, 2]"

        assertEquals(ion.loader.load(expected), ion.loader.load(filePath.toFile().readText()))
    }

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
        assertEquals(ion.loader.load(expected), ion.loader.load(filePath.toFile().readText()))
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
        assertEquals(ion.loader.load(expected), ion.loader.load(filePath.toFile().readText()))
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
        assertEquals(ion.loader.load(expected), ion.loader.load(filePath.toFile().readText()))
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
        assertEquals(ion.loader.load(expected), ion.loader.load(filePath.toFile().readText()))
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
        assertEquals(ion.loader.load(expected), ion.loader.load(filePath.toFile().readText()))
    }

    @Test
    fun integration_success_int() {
        // Arrange
        val filePath = Files.createTempFile("integration_success_int", ".ion")
        val query = "write_file('$filePath', SELECT VALUE a FROM input_data)"
        val input = "{a : 5}"
        val expected = "$BAG_ANNOTATION::[5]"

        // Act
        val cliResponse =
            makeCliAndGetResult(query = query, input = input, output = outputStream, pipeline = pipeline)

        // Assert
        assertAsIon(TRUE_STRING, cliResponse)
        assertEquals(ion.loader.load(expected), ion.loader.load(filePath.toFile().readText()))
    }
}
