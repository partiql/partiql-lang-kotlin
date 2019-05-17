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
import org.partiql.lang.*
import org.partiql.testframework.contracts.*
import org.partiql.testframework.testcar.*
import org.partiql.testframework.util.decimal
import org.partiql.testframework.util.integer
import org.partiql.testframework.util.string
import org.partiql.testframework.util.struct
import org.partiql.testframework.util.writeNextFieldFromReader
import java.io.*

const val TEMP_PATH = "tmp"
const val CONFIG_PATH = "$TEMP_PATH/current-run-config.ion"
const val QUERY_OUTPUT_PATH = "$TEMP_PATH/current-query-output.ion"

internal class BenchmarkExecutor(private val ion: IonSystem,
                                 private val benchmarkRunner: BenchmarkRunner = JmhRunner()) {

    fun execute(name: String,
                sql: String,
                environmentStruct: IonStruct,
                sessionStruct: IonStruct,
                compileOptionsStruct: IonStruct): CarResponse = try {
        resetFileStructure()

        writeConfigFile(sql, environmentStruct, sessionStruct, compileOptionsStruct)

        val results = benchmarkRunner.run()
        val resultValue = buildIonOutput(name, results)
        val queryResult = readQueryResult()

        BenchmarkSuccess(resultValue, queryResult)
    }
    catch (ex: SqlException) {
        ExecuteFailed(ex.message, ex.errorCode.toString(), ex.errorContext?.toStruct(ion))
    }
    catch (ex: Exception) {
        Error(ex.message!!, ex.getStackTraceString())
    }
    finally {
        resetFileStructure()
    }

    private fun resetFileStructure() {
        File(TEMP_PATH).mkdir()
        File(CONFIG_PATH).delete()
        File(QUERY_OUTPUT_PATH).delete()
    }

    /**
     * Builds the aggregated benchmark results as an ion value, specification below
     *
     * ```
     *  {
     *      name: <benchmark command name>,
     *      benchmarks: {
     *          <benchmarkType>: {
     *              profiles: {
     *                  <prof name>: {
     *                      mean: <median>,
     *                      stdev: <standard deviation>,
     *                      n: <number of iterations>,
     *                      min: <minimum value>,
     *                      max: <maximum value>,
     *                      unit: <unit used>
     *                  }
     *              }
     *          },
     *      },
     *      raw: <raw benchmark engine output as JSON>
     *  }
     * ```
     */
    private fun buildIonOutput(name: String, benchmarkRunResults: BenchmarkRunResults): IonValue {
        val topContainer = ion.newEmptyStruct()

        ion.newWriter(topContainer).use { writer ->
            writer.string("name", name)
            writer.writeNextFieldFromReader("raw", ion.newReader(benchmarkRunResults.rawRunnerOutputAsIon))

            // benchmark results
            writer.struct("benchmarks") {
                benchmarkRunResults.results.forEach { result ->
                    struct(result.label) {
                        struct("profiles") {

                            // fixed by the primary mode
                            struct("throughput") {
                                decimal("mean", result.mean)
                                decimal("stdev", result.standardDeviation)
                                integer("n", result.n)
                                decimal("min", result.min)
                                decimal("max", result.max)
                                string("unit", result.unit)
                            }
                        }
                    }
                }
            }
        }

        return topContainer
    }

    /**
     * Writes config file in `./tmp/current-run-config.ion`, specification below
     *
     * ```
     *  {
     *      session: <struct with session options, optional>,
     *      compile_options: <struct with compile options, optional>,
     *      env: <struct with global environment to be set, optional>,
     *      sql: <query as string, required>
     *  }
     * ```
     */
    private fun writeConfigFile(sql: String,
                                environmentStruct: IonStruct,
                                sessionStruct: IonStruct,
                                compileOptionsStruct: IonStruct) {
        fun IonWriter.writeNonEmptyStructField(name: String, value: IonStruct) {
            if (!value.isEmpty) {
                writeNextFieldFromReader(name, ion.newReader(value))
            }
        }

        val out = FileOutputStream(File(CONFIG_PATH))
        ion.newTextWriter(out).use { writer ->
            writer.stepIn(IonType.STRUCT)

            writer.string("sql", sql)
            writer.writeNonEmptyStructField("env", environmentStruct)
            writer.writeNonEmptyStructField("session", sessionStruct)
            writer.writeNonEmptyStructField("compile_options", compileOptionsStruct)

            writer.stepOut()
        }
    }

    private fun readQueryResult(): IonValue = ion.loader.load(File(QUERY_OUTPUT_PATH))[0]
}


