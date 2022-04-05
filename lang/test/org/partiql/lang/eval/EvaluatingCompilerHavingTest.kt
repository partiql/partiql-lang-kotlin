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

import junitparams.Parameters
import org.junit.Test
import org.partiql.lang.errors.ErrorCode

class EvaluatingCompilerHavingTest : EvaluatorTestBase() {
    val session = mapOf(
        "repeating_things" to """[
            { thingId: 1,  attributeId: 10 },

            { thingId: 2,  attributeId: 20 },
            { thingId: 3,  attributeId: 20 },

            { thingId: 4,  attributeId: 30 },
            { thingId: 5,  attributeId: 30 },
            { thingId: 6,  attributeId: 30 },

            { thingId: 7,  attributeId: 40 },
            { thingId: 8,  attributeId: 40 },
            { thingId: 9,  attributeId: 40 },
            { thingId: 10,  attributeId: 40 },

            { thingId: 11, attributeId: 50 },
            { thingId: 12, attributeId: 50 },
            { thingId: 13, attributeId: 50 },
            { thingId: 14, attributeId: 50 },
            { thingId: 15, attributeId: 50 },

            { thingId: 16, attributeId: null },
            { thingId: 17, attributeId: null },
            { thingId: 18, attributeId: null },
            { thingId: 19, attributeId: null },
            { thingId: 20, attributeId: null },
            { thingId: 21, attributeId: null },
        ]"""
    ).toSession()

    @Test
    @Parameters
    fun groupByHavingTest(tc: EvaluatorTestCase) = runTestCaseInLegacyAndPermissiveModes(tc, session)

    fun parametersForGroupByHavingTest() =
        listOf(
            EvaluatorTestCase(
                "GROUP BY with HAVING - all rows",
                """
                    SELECT attributeId, COUNT(*) as the_count
                    FROM repeating_things
                    GROUP BY attributeId GROUP AS g
                    HAVING 1 = 1
                """,
                """<<
                  { 'attributeId': 10,   'the_count': 1 },
                  { 'attributeId': 20,   'the_count': 2 },
                  { 'attributeId': 30,   'the_count': 3 },
                  { 'attributeId': 40,   'the_count': 4 },
                  { 'attributeId': 50,   'the_count': 5 },
                  { 'attributeId': null, 'the_count': 6 }
                >>"""
            ),
            EvaluatorTestCase(
                "GROUP BY with HAVING and WHERE",
                """
                    SELECT attributeId, COUNT(*) as the_count
                    FROM repeating_things
                    WHERE thingId >= 9
                    GROUP BY attributeId GROUP AS g
                    HAVING 1 = 1
                """,
                """<<
                  { 'attributeId': 40,   'the_count': 2 },
                  { 'attributeId': 50,   'the_count': 5 },
                  { 'attributeId': null, 'the_count': 6 }
                >>"""
            ),
            EvaluatorTestCase(
                "GROUP BY with HAVING - no rows",
                """
                    SELECT attributeId, COUNT(*) as the_count
                    FROM repeating_things
                    GROUP BY attributeId GROUP AS g
                    HAVING 1 = 0
                """,
                """<<>>"""
            ),
            EvaluatorTestCase(
                "GROUP BY with HAVING",
                """
                    SELECT attributeId, COUNT(*) as the_count
                    FROM repeating_things
                    GROUP BY attributeId
                    HAVING attributeId > 30
                """,
                """<<
                  { 'attributeId': 40, 'the_count': 4 },
                  { 'attributeId': 50, 'the_count': 5 }
                >>"""
            ),
            EvaluatorTestCase(
                "GROUP BY with HAVING and WHERE",
                """
                    SELECT attributeId, COUNT(*) as the_count
                    FROM repeating_things
                    WHERE thingId >= 9
                    GROUP BY attributeId
                    HAVING attributeId > 30
                """,
                """<<
                  { 'attributeId': 40, 'the_count': 2 },
                  { 'attributeId': 50, 'the_count': 5 }
                >>"""
            ),
            EvaluatorTestCase(
                "GROUP BY with HAVING that calls COUNT(*)",
                """
                    SELECT attributeId, COUNT(*) as the_count
                    FROM repeating_things
                    GROUP BY attributeId
                    HAVING COUNT(*) >= 3
                """,
                """<<
                  { 'attributeId': 30,   'the_count': 3 },
                  { 'attributeId': 40,   'the_count': 4 },
                  { 'attributeId': 50,   'the_count': 5 },
                  { 'attributeId': null, 'the_count': 6 }
                >>"""
            ),
            EvaluatorTestCase(
                "GROUP BY with HAVING that calls SUM(*)",
                """
                    SELECT attributeId, SUM(attributeId) as the_count
                    FROM repeating_things
                    GROUP BY attributeId
                    HAVING SUM(attributeId) >= 160
                """,
                """<<
                  { 'attributeId': 40, 'the_count': 160 },
                  { 'attributeId': 50, 'the_count': 250 }
                >>"""
            ),
            EvaluatorTestCase(
                "GROUP BY with HAVING that references GROUP AS variable",
                """
                    SELECT attributeId, COUNT(*) as the_count
                    FROM repeating_things
                    GROUP BY attributeId GROUP AS g
                    HAVING g IS NOT MISSING AND g IS NOT NULL
                """,
                """<<
                  { 'attributeId': 10,   'the_count': 1 },
                  { 'attributeId': 20,   'the_count': 2 },
                  { 'attributeId': 30,   'the_count': 3 },
                  { 'attributeId': 40,   'the_count': 4 },
                  { 'attributeId': 50,   'the_count': 5 },
                  { 'attributeId': null, 'the_count': 6 }
                >>"""
            )
        )

    @Test
    fun havingWithoutGroupBy() {
        evalAssertThrowsSqlException("SELECT foo.bar FROM bat HAVING 1 = 1") {
            assertEquals(it.errorCode, ErrorCode.SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY)
        }
    }
}
