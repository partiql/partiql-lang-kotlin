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

package org.partiql.lang.eval

/**
 * Defines a test case for query evaluation.
 */
data class EvaluatorTestCase(
    /** The "group" of the tests--this only appears in the IDE's test runner and can be used to identify where in the
     * source code the test is defined.
     */
    val groupName: String?,
    /**
     * The query to be evaluated.
     */
    val sqlUnderTest: String,
    /**
     * A expression which will be evaluated to determine the set of expected values that should match the result
     * of [sqlUnderTest].
     */
    val expectedSql: String,
    /**
     * The [CompOptions] containing the [CompileOptions] to use when executing [sqlUnderTest] and [expectedSql].
     */
    val compOptions: CompOptions = CompOptions.STANDARD) {

    constructor(
        query: String,
        expectedSql: String,
        compOptions: CompOptions = CompOptions.STANDARD
    ) : this(null, query, expectedSql, compOptions)

    /** This will show up in the IDE's test runner. */
    override fun toString() = when {
        groupName != null -> "$groupName : $sqlUnderTest : $compOptions"
        else              -> "$sqlUnderTest : $compOptions"
    }
}


