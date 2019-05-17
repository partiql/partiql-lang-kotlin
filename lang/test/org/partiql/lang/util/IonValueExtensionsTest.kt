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

package org.partiql.lang.util

import com.amazon.ion.*
import org.partiql.lang.*
import junitparams.*
import org.junit.*
import org.junit.runner.*


@RunWith(JUnitParamsRunner::class)
class IonValueExtensionsTest : TestBase() {

    data class TestCase(val expected: String, val original: String)

    fun parametersForFilterTermNodesTest() =
        listOf(
            TestCase(
                 "(lit 1)",
                 """
                 (term
                     (exp
                        (lit 1))
                     (meta d e f))
                 """
            ),
            TestCase(
                "(foo (bar))",
                """
                (term
                        (exp
                            (foo
                                (term
                                    (exp (bar)
                                    (meta a b c))
                        (meta d e f)))))
                """))

    @Test
    @Parameters
    fun filterTermNodesTest(testCase: TestCase) {
        val original = ion.singleValue(testCase.original) as IonSexp
        val expectedSexp = ion.singleValue(testCase.expected) as IonSexp

        val filteredSexp = original.filterTermNodes()

        assertSexpEquals(expectedSexp, filteredSexp)
    }

}