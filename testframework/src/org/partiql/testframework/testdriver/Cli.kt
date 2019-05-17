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

package org.partiql.testframework.testdriver

import com.amazon.ion.system.*
import org.partiql.lang.eval.*
import org.partiql.testframework.testcar.*
import org.partiql.testframework.testdriver.ast.*
import org.partiql.testframework.testdriver.error.*
import org.partiql.testframework.testdriver.parser.*
import org.partiql.testframework.testdriver.visitors.*
import org.partiql.testframework.*
import org.partiql.testframework.testdriver.rewriters.*
import java.io.*

internal val TEST_SCRIPT_FILTER = FileFilter { file ->
    (file.isFile && file.name.endsWith(".sqlts")) || file.isDirectory
}

class Cli(private val out: PrintStream, private val logOut: PrintStream) {

    private val ion = IonSystemBuilder.standard().build()!!
    private val valueFactory = ExprValueFactory.standard(ion)

    private val resultStream = ResultStream(ion, out, logOut)

    private val validationVisitor = ScriptValidatorVisitor(resultStream)
    private val executionVisitor = ScriptExecuteVisitor(ReferenceSqlCar(ion), resultStream, valueFactory)

    val passCount: Int
        get() = resultStream.passCount

    val assertsFailedCount: Int
        get() = resultStream.assertsFailedCount

    val commandSendCount: Int
        get() = executionVisitor.commandSendCount

    fun run(testScriptPaths: List<File>) {
        val testScripts = parseTestScripts(testScriptPaths)

        testScripts.validateAll()

        val forUnrollingRewriter = ForUnrollingRewriter(ion, resultStream)
        val unrolledTestScripts = testScripts.map { forUnrollingRewriter.rewriteTestScript(it) }

        if (resultStream.errorCount > 0) {
            testScriptError()
        }

        val startMillis = System.currentTimeMillis()
        unrolledTestScripts.executeAll()
        val durationMillis = System.currentTimeMillis() - startMillis

        printSummary(durationMillis)

        if (resultStream.errorCount > 0) {
            testScriptError()
        }

        if (resultStream.assertsFailedCount > 0) {
            throw ExecutionException("Your PartiQL implementation has issues.")
        }

        logOut.println("Your PartiQL implementation is golden.")
    }

    private fun testScriptError() {
        throw ExecutionException("Your test suite has issues.")
    }

    private fun List<TestScript>.validateAll() {
        this.forEach { it.accept(validationVisitor) }
        if (resultStream.errorCount > 0) {
            throw ValidationException("Encountered ${resultStream.errorCount} test script validation errors - aborting test run.\n")
        }

        logOut.println("Test script passed validation.")
    }

    private fun List<TestScript>.executeAll() {
        logOut.println("Beginning execution.")
        this.forEach { it.accept(executionVisitor) }
    }

    private fun printSummary(durationMillis: Long) {
        logOut.println("*******************************************************************************")
        logOut.println("Test execution complete!")
        logOut.println("*******************************************************************************")

        logOut.println("duration          : ${String.format("%.2f", durationMillis / 1000.0)} seconds")
        logOut.println("tests passed      : ${resultStream.passCount}")
        logOut.println("assertions failed : ${resultStream.assertsFailedCount}")
        logOut.println("commands sent     : ${executionVisitor.commandSendCount}")
    }

    private fun parseTestScripts(testScriptPaths: List<File>): List<TestScript> {
        val testScripts = testScriptPaths.map { scriptPath ->
            scriptPath.listRecursive(TEST_SCRIPT_FILTER).map { file ->
                FileInputStream(file).use { inputStream ->
                    val parser = ScriptParser(ion.newReader(inputStream), file.absolutePath, ion, resultStream)
                    val testScript = parser.parse()
                    logOut.println("Loaded and parsed test script $file")
                    testScript
                }
            }
        }.flatMap { it }

        if (testScripts.isEmpty()) {
            throw ValidationException("No test scripts were found on ${testScriptPaths.joinToString(", ")}")
        }

        return testScripts
    }
}
