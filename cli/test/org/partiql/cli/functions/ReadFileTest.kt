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

import com.amazon.ion.system.*
import org.partiql.lang.eval.*
import org.junit.*
import org.junit.Assert.*
import java.io.*

class ReadFileTest {
    private val ion = IonSystemBuilder.standard().build()
    private val valueFactory = ExprValueFactory.standard(ion)
    private val function = ReadFile(valueFactory)
    private val env = Environment(locals = Bindings.empty(),
                                  session = EvaluationSession.standard())

    private fun String.exprValue() = valueFactory.newFromIonValue(ion.singleValue(this))
    private fun writeFile(path: String, content: String) = File(dirPath(path)).writeText(content)

    companion object {
        fun dirPath(fname: String = "") = "tst-resources/$fname"

        @BeforeClass
        @JvmStatic
        fun setUp() {
            File(dirPath()).mkdir()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            File(dirPath()).deleteRecursively()
        }
    }

    @Test
    fun readIonAsDefault() {
        writeFile("data.ion", "1 2")

        val args = listOf("\"${dirPath("data.ion")}\"").map { it.exprValue() }
        val actual = function.call(env, args).ionValue
        val expected = "[1, 2]"

        assertEquals(actual, ion.singleValue(expected))
    }

    @Test
    fun readIon() {
        writeFile("data.ion", "1 2")

        val args = listOf("\"${dirPath("data.ion")}\"", "{type:\"ion\"}").map { it.exprValue() }
        val actual = function.call(env, args).ionValue
        val expected = "[1, 2]"

        assertEquals(actual, ion.singleValue(expected))
    }

    @Test
    fun readCsv() {
        writeFile("data.csv", "1,2")

        val args = listOf("\"${dirPath("data.csv")}\"", "{type:\"csv\"}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{_1:\"1\",_2:\"2\"}]"

        assertEquals(actual, ion.singleValue(expected))
    }

    @Test
    fun readTsv() {
        writeFile("data.tsv", "1\t2")

        val args = listOf("\"${dirPath("data.tsv")}\"", "{type:\"tsv\"}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{_1:\"1\",_2:\"2\"}]"

        assertEquals(actual, ion.singleValue(expected))
    }
}
