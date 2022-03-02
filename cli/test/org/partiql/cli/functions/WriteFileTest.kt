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
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValueFactory
import java.io.File

class WriteFileTest {
    private val ion = IonSystemBuilder.standard().build()
    private val valueFactory = ExprValueFactory.standard(ion)
    private val function = WriteFile(valueFactory)
    private val env = Environment(session = EvaluationSession.standard())

    private fun String.exprValue() = valueFactory.newFromIonValue(ion.singleValue(this))
    private fun readFile(path: String) = File(dirPath(path)).readText()

    private fun dirPath(fname: String = "") = "/tmp/partiqltest/$fname"

    @Before
    fun setUp() {
        File(dirPath()).mkdir()
    }

    @After
    fun tearDown() {
        File(dirPath()).deleteRecursively()
    }

    @Test
    fun writeIonAsDefault() {
        val args = listOf(""" "${dirPath("data.ion")}" """, "[1, 2]").map { it.exprValue() }
        function.callWithRequired(env, args).ionValue

        val expected = "1 2"

        Assert.assertEquals(ion.loader.load(expected), ion.loader.load(readFile("data.ion")))
    }

    @Test
    fun readIon() {
        val args = listOf(""" "${dirPath("data1.ion")}" """, "[1, 2]").map { it.exprValue() }
        val additionalOptions = """{type: "ion"}""".exprValue()
        function.callWithOptional(env, args, additionalOptions).ionValue

        val expected = "1 2"

        Assert.assertEquals(ion.loader.load(expected), ion.loader.load(readFile("data1.ion")))
    }
}
