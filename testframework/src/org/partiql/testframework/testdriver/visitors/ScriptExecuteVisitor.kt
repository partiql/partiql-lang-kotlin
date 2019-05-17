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

package org.partiql.testframework.testdriver.visitors

import com.amazon.ion.*
import org.partiql.lang.eval.*
import org.partiql.testframework.contracts.*
import org.partiql.testframework.testdriver.*
import org.partiql.testframework.testdriver.ast.*
import org.partiql.testframework.testdriver.error.*
import org.partiql.testframework.testdriver.parser.*
import org.partiql.testframework.*
import org.partiql.testframework.util.*
import org.partiql.lang.util.*

class ScriptExecuteVisitor(
    private val car: Car,
    resultStream: ResultStream,
    private val valueFactory: ExprValueFactory) : AstVisitor(resultStream) {
    var commandSendCount = 0

    override fun visitTestCommand(cmd: TestCommand) {
        val response = sendCmd(cmd)
        val expectation = cmd.expectation!!

        when (response) {
            is ExecuteSuccess   -> handleExecuteTestCommandSuccess(expectation, response, cmd.location)
            is ExecuteFailed    -> handleExecuteTestCommandFailed(expectation, cmd.location, response)
            is Ok               -> handleUnexpectedOk(cmd.location)
            is Error            -> throw IllegalStateException("Should have caught an error response in sendCmd instead of here.")
            is BenchmarkSuccess -> throw IllegalStateException("BenchmarkSuccess is not valid for tests.")
        }
    }

    override fun visitBenchmark(cmd: BenchmarkCommand) {
        resultStream.logOutput.print("Starting benchmark: ${cmd.name}...")
        val benchmarkStartTime = System.currentTimeMillis()

        val response = sendCmd(cmd)
        val expectation = cmd.expectation!!

        when (response) {
            is BenchmarkSuccess -> handleExecuteBenchmarkCommandSuccess(expectation, response, cmd.location)
            is ExecuteFailed    -> throw IllegalStateException("ExecuteFailed is not a valid car response for benchmarks.")
            is Ok               -> handleUnexpectedOk(cmd.location)
            is Error            -> throw IllegalStateException("Should have caught an error response in sendCmd instead of here.")
            is ExecuteSuccess   -> throw IllegalStateException("ExecuteSuccess is not valid for tests.")
        }

        resultStream.logOutput.println(" done.  Duration: ${(System.currentTimeMillis() - benchmarkStartTime) / 1000.0}s")
    }

    override fun visitSetDefaultEnvironment(cmd: SetDefaultEnvironment) = sendCmdExpectOK(cmd.location, cmd)
    override fun visitSetDefaultCompileOptions(cmd: SetDefaultCompileOptions) = sendCmdExpectOK(cmd.location, cmd)
    override fun visitSetDefaultSession(cmd: SetDefaultSession) = sendCmdExpectOK(cmd.location, cmd)

    private fun handleUnexpectedOk(location: ScriptLocation) {
        resultStream.error(location, "incorrect response received from ExecuteCarCommand. expected [ExecuteSuccessCarResponse | ExecuteFailed] but received Ok.")
    }

    private fun handleExecuteBenchmarkCommandSuccess(expectation: Expectation, response: BenchmarkSuccess, location: ScriptLocation) {
        when (expectation) {
            is SuccessExpectation     -> when {
                expectation.expectedResult != response.queryResult -> {
                    val assertMessage = "expected and actual results did not match" +
                                        "\nexpected: ${expectation.expectedResult.toPrettyString().trim()}" +
                                        "\nactual:   " + response.queryResult.toPrettyString().trim()
                    resultStream.assertFailed(location, assertMessage)
                }
                else                                         -> {
                    resultStream.benchmarkSuccess(response.benchmarkResult)
                }
            }
            is CountExpectation -> when {
                expectation.expectedResult != response.queryResult.size -> {
                    val assertMessage = "expected and actual result size did not match" +
                                        "\nexpected: ${expectation.expectedResult}" +
                                        "\nactual:   ${response.queryResult.size}"
                    resultStream.assertFailed(location, assertMessage)
                }
                else                                                    -> {
                    resultStream.benchmarkSuccess(response.benchmarkResult)
                }
            }
            is ErrorExpectation       -> {
                resultStream.assertFailed(location, "benchmark execution unexpectedly succeeded and had an IonValue")
            }
            is VariableRefExpectation -> {
                //VariableRefExpectation should be rewritten out by the ForUnrollingRewriter or an
                //error should have been raised and execution should never have gotten this far.
                throw IllegalStateException("Unresolved variable reference: `${expectation.variableName}`")
            }
        }
    }

    private fun handleExecuteTestCommandFailed(expectation: Expectation, location: ScriptLocation, response: ExecuteFailed) {
        when (expectation) {
            is SuccessExpectation -> {
                val failureMessage = "unexpected execution failure\nresponse from car was:\n$response"
                resultStream.assertFailed(location, failureMessage)
            }
            is ErrorExpectation                        -> {
                if (expectation.expectedErrorCode!!.stringValue() != response.errorCode) {

                    val failureMessage = "the error code did not match the expected value" +
                                         "\nexpected: ${expectation.expectedErrorCode}" +
                                         "\nactual:   ${response.errorCode}"
                    resultStream.assertFailed(location, failureMessage)
                }

                if (!compareErrorContextProperties(expectation, response.properties)) {
                    resultStream.pass(location)
                }
            }
            is CountExpectation                        -> throw IllegalStateException("'expected_count' not allowed for test commands") // should have been blocked by validation
            is VariableRefExpectation                  -> {
                //VariableRefExpectation should be rewritten out by the ForUnrollingRewriter or an
                //error should have been raised and execution should never have gotten this far.
                throw IllegalStateException("Unresolved variable reference: `${expectation.variableName}`")
            }
        }
    }

    private fun handleExecuteTestCommandSuccess(expectation: Expectation, response: ExecuteSuccess, location: ScriptLocation) {
        when (expectation) {
            is SuccessExpectation     -> {
                val expectedExprValue = deserializeExprValue(expectation.expectedResult, valueFactory)
                val actualExprValue = deserializeExprValue(response.value, valueFactory)

                if (!expectedExprValue.exprEquals(actualExprValue)) {
                    val assertMessage = "expected and actual results did not match" +
                                        "\nexpected: ${expectation.expectedResult.toPrettyString().trim()}" +
                                        "\nactual:   " + response.value.toPrettyString().trim()
                    resultStream.assertFailed(location, assertMessage)
                } else {
                    resultStream.pass(location)
                }
            }
            is ErrorExpectation       ->
                resultStream.assertFailed(location, "test execution unexpectedly succeeded and had an IonValue")

            is CountExpectation -> throw IllegalStateException("'expected_count' not allowed for test commands") // should have been blocked by validation
            is VariableRefExpectation -> {
                //VariableRefExpectation should be rewritten out by the ForUnrollingRewriter or an
                //error should have been raised and execution should never have gotten this far.
                throw IllegalStateException("Unresolved variable reference: `${expectation.variableName}`")
            }
        }
    }




    private fun compareErrorContextProperties(expectation: ErrorExpectation, actualProperties: IonStruct?): Boolean {
        val expected = expectation.expectedProperties

        // TODO: https://github.com/partiql/partiql-lang-kotlin/issues/33
        // we don't have meta nodes all the time so, depending on the query, some error codes may not have any
        // properties. We should assume all error codes will have at least the source location when we start adding
        // meta nodes to all nodes, i.e. after AST refactor
        return when {
            expected == null && actualProperties == null -> true
            expected != null && actualProperties == null -> {
                resultStream.error(expectation.location, "error response doesn't contain properties")
                false
            }
            expected == null && actualProperties != null -> {
                resultStream.error(expectation.location, "error expected doesn't contain error properties while actual is $actualProperties")
                false
            }
            else                                         -> {
                val failureMessage = StringBuilder()

                expectation.expectedProperties!!.filter { !actualProperties!!.containsKey(it.fieldName) }.forEach {
                    failureMessage.appendln("error context does not contain expected property '${it.fieldName}'")
                }

                actualProperties!!.filter { !expectation.expectedProperties.containsKey(it.fieldName) }.forEach {
                    failureMessage.appendln("error context contains unexpected property '${it.fieldName}'")
                }

                expectation.expectedProperties.forEach {
                    val actualValue = actualProperties.get(it.fieldName)
                    if(actualValue != null && it != actualValue) {
                        failureMessage.appendln("error context property '${it.fieldName}' did not match expected value (" +
                                                "expected `${it.toPrettyString().trim()}` " +
                                                "but was `${actualValue.toPrettyString().trim()}`)")
                    }
                }

                val hasFailure = failureMessage.isNotBlank()

                if(hasFailure) {
                    failureMessage.appendln("expected properties:\n${expectation.expectedProperties.toPrettyString().trim()}")
                    failureMessage.appendln("actual properties:\n${actualProperties.toPrettyString().trim()}")

                    resultStream.assertFailed(expectation.location, failureMessage.toString())
                }

                hasFailure
            }
        }
    }

    private fun sendCmd(cmd: AstNode): CarResponse {
        commandSendCount++

        val response = car.executeCmd(cmd.toCarCommand())

        if(response is Error) {
            throw FatalException("${cmd.location}: car reported error: ${response.message}", response.details)
        }

        return response
    }

    private fun sendCmdExpectOK(location: ScriptLocation, cmd: AstNode) {
        val response = sendCmd(cmd)

        if (response !is Ok) {
            resultStream.assertFailed(location, "response was:\n$response)")
        }
    }
}
