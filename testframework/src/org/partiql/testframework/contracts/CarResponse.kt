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

package org.partiql.testframework.contracts

import com.amazon.ion.*

sealed class CarResponse

/** A response from the car that indicates successful execution of a command but does not otherwise have any result. */
class Ok : CarResponse() {
    override fun toString(): String = "Car command executed successfully but did not include a result."
}

/** A response from the car returned after successful query execution with the result stored in the [value] field. */
class ExecuteSuccess(val value : IonValue) : CarResponse() {
    override fun toString(): String =
        "Query execution completed successfully\n" +
        "result:    ${this.value}"
}

class BenchmarkSuccess(val benchmarkResult: IonValue, val queryResult: IonValue) : CarResponse()

/** A response from the car indicating an invalid command was sent from the driver. */
class Error(val message: String, val details: String? = null) : CarResponse() {
    override fun toString(): String =
        "Car reported an error\n" +
        "message:    ${this.message}\n" +
        "details:    ${this.details ?: "(no details were included)"}\n"
}

/**
 * A response from the car indicating that an error occurred during an attempt to execute a query.
 * This might have been expected, depending on the test.
 */
class ExecuteFailed(val message: String, val errorCode: String, val properties: IonStruct? = null) : CarResponse() {
    override fun toString(): String =
        "Query execution failed!\n" +
        "message:    ${this.message}\n" +
        "code:       ${this.errorCode}\n" +
        "properties: ${if(this.properties != null) this.properties.toPrettyString()!!.trim() else "<no error context"}\n"
}

