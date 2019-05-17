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

package org.partiql.testframework

open class TestSuiteInternalException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)

abstract class TestSuiteException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
open class FatalException(msg: String? = null, val details: String? = null, cause: Throwable? = null) : TestSuiteException(msg, cause)
open class ValidationException(msg: String? = null, cause: Throwable? = null) : TestSuiteException(msg, cause)
open class ExecutionException(msg: String? = null, cause: Throwable? = null) : TestSuiteException(msg, cause)
