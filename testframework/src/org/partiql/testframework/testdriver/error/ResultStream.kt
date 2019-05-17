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

package org.partiql.testframework.testdriver.error

import com.amazon.ion.*
import org.partiql.testframework.testdriver.*
import org.partiql.testframework.testdriver.parser.*
import java.io.*
import java.util.*

private enum class Status { ERROR, FAILURE, SUCCESS }

/**
 * Collects command execution results and outputs execution information
 */
class ResultStream(ion: IonSystem,
                   output: PrintStream,
                   val logOutput: PrintStream) : AutoCloseable {

    private val writer = ion.newTextWriter(output as OutputStream)
    private val contextStack = Stack<String>()

    var passCount = 0
        private set
    var errorCount = 0
        private set
    var assertsFailedCount = 0

    /**
     * This is used to provide helpful context when reporting errors.
     */
    fun pushIncludeContext(currentLocation: ScriptLocation) = contextStack.push("In script included from $currentLocation")!!

    fun pushTemplateExpansionContext(currentLocation: ScriptLocation) = contextStack.push("In template expanded using variableSet at $currentLocation")!!

    fun pushContext(contextDescription: String) = contextStack.push(contextDescription)!!

    fun popContext() = contextStack.pop()!!

    fun error(location: ScriptLocation, message: String) {
        val errorMessage = "${contextAsString()}$location error: $message"

        logOutput.println(errorMessage)
        outputResult(location, Status.ERROR, errorMessage)

        errorCount++
    }

    fun assertFailed(location: ScriptLocation, message: String) {
        val failureMessage = "${contextAsString()}$location assertion failed: $message"

        outputResult(location, Status.FAILURE, failureMessage)
        logOutput.println(failureMessage)

        assertsFailedCount++
    }

    fun benchmarkSuccess(value: IonValue) {
        outputBenchmark(value)
    }

    fun pass(location: ScriptLocation) {
        passCount++
        outputResult(location, Status.SUCCESS)
    }

    fun errorIf(inError: Boolean, detailsFunc: () -> Pair<ScriptLocation, String>) {
        if (inError) {
            val (location, message) = detailsFunc()
            error(location, message)
        }
    }

    override fun close() {
        writer.close()
    }

    private fun contextAsString(): String = if (!contextStack.empty()) {
        contextStack.fold(StringBuilder()) { acc, el -> acc.append(el) }.append("\t").toString()
    }
    else {
        ""
    }

    private fun outputBenchmark(value: IonValue) {
        value.writeTo(writer)
    }

    private fun outputResult(location: ScriptLocation, status: Status, message: String? = null) {
        writer.stepIn(IonType.STRUCT)

        writer.setFieldName("location")
        writer.writeString(location.toString())

        writer.setFieldName("status")
        writer.writeString(status.name.toLowerCase())

        if(message != null){
            writer.setFieldName("message")
            writer.writeString(message)
        }

        writer.stepOut()
    }
}
