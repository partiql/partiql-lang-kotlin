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

package org.partiql.testframework.testcar.benchmark

import com.amazon.ion.*
import com.amazon.ion.system.*
import org.partiql.testframework.contracts.*
import org.partiql.lang.util.*
import org.junit.*
import org.junit.Assert.*
import java.io.*
import java.math.*

private const val FAKE_QUERY_RESULT = "fake result"

class BenchmarkExecutorTest {
    private val ion = IonSystemBuilder.standard().build()


    @Test
    fun executeTest() {
        val name = "benchmark name"
        val sql = "benchmark query"
        val environment = ion.newEmptyStruct().apply {
            add("env_1", ion.newInt(11))
            add("env_2", ion.newInt(12))
        }

        val session = ion.newEmptyStruct().apply {
            add("session_1", ion.newInt(21))
            add("session_2", ion.newInt(22))
        }

        val compileOptions = ion.newEmptyStruct().apply {
            add("cOpt_1", ion.newInt(31))
            add("cOpt_2", ion.newInt(32))
        }

        val rawRunnerOutputAsIonStub = """ {"raw_jmh": "json benchmarkOutput"} """
        val resultStub1 = BenchmarkRunResultStub("r1",
                                                 BigDecimal(10),
                                                 BigDecimal(11),
                                                 12,
                                                 BigDecimal(13),
                                                 BigDecimal(14),
                                                 "r1Unit")
        val resultStub2 = BenchmarkRunResultStub("r2",
                                                 BigDecimal(20),
                                                 BigDecimal(21),
                                                 22,
                                                 BigDecimal(23),
                                                 BigDecimal(24),
                                                 "r2Unit")

        val fakeRunner: BenchmarkRunner = object : BenchmarkRunner {
            override fun run(): BenchmarkRunResults {
                val configDatagram = ion.loader.load(File(CONFIG_PATH))
                assertEquals(1, configDatagram.size)
                assertTrue(configDatagram[0] is IonStruct)
                val config = configDatagram[0] as IonStruct

                assertEquals(session, config["session"])
                assertEquals(compileOptions, config["compile_options"])
                assertEquals(environment, config["env"])
                assertEquals(sql, config["sql"].stringValue())

                ion.newTextWriter(FileOutputStream(File(QUERY_OUTPUT_PATH))).use { w -> w.writeString(FAKE_QUERY_RESULT) }

                return BenchmarkRunResults(rawRunnerOutputAsIonStub, listOf(resultStub1, resultStub2))
            }
        }

        val subject = BenchmarkExecutor(ion, fakeRunner)

        val result = subject.execute(name, sql, environment, session, compileOptions) as BenchmarkSuccess
        val benchmarkOutput = result.benchmarkResult as IonStruct

        assertStructField(name, "name", benchmarkOutput)
        assertEquals(ion.loader.load(rawRunnerOutputAsIonStub)[0], benchmarkOutput["raw"])

        val benchmarks = benchmarkOutput["benchmarks"] as IonStruct
        assertEquals(2, benchmarks.size())
        assertRunResult(resultStub1, benchmarks["r1"] as IonStruct)
        assertRunResult(resultStub2, benchmarks["r2"] as IonStruct)

        val queryResult = result.queryResult
        assertEquals(FAKE_QUERY_RESULT, queryResult.stringValue())

        // check that config file was reset
        val configFile = File(CONFIG_PATH)
        assertFalse(configFile.exists())

        val queryResultFile = File(QUERY_OUTPUT_PATH)
        assertFalse(queryResultFile.exists())
    }

    private fun assertRunResult(expected: BenchmarkRunResultStub, actual: IonStruct) {
        assertEquals(expected.label, actual.fieldName)
        val throughput = actual["profiles"]?.get("throughput") as IonStruct

        assertStructField(expected.mean, "mean", throughput)
        assertStructField(expected.standardDeviation, "stdev", throughput)
        assertStructField(expected.n, "n", throughput)
        assertStructField(expected.min, "min", throughput)
        assertStructField(expected.max, "max", throughput)
        assertStructField(expected.unit, "unit", throughput)
    }

    private fun assertStructField(expected: String, fieldName: String, actual: IonStruct) = when (actual[fieldName]) {
        null -> throw AssertionError("struct does not contain a field $fieldName")
        else -> assertEquals(expected, actual[fieldName].stringValue())
    }

    private fun assertStructField(expected: Number, fieldName: String, actual: IonStruct) = when (actual[fieldName]) {
        null -> throw AssertionError("struct does not contain a field $fieldName")
        else -> assertEquals(expected, actual[fieldName].numberValue())
    }
}

private data class BenchmarkRunResultStub(override val label: String,
                                          override val mean: BigDecimal,
                                          override val standardDeviation: BigDecimal,
                                          override val n: Long,
                                          override val min: BigDecimal,
                                          override val max: BigDecimal,
                                          override val unit: String) : BenchmarkRunResult
