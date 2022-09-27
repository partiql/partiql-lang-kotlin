/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.visitors

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.util.ArgumentsProviderBase

class OrderBySortSpecVisitorTransformTests : VisitorTransformTestBase() {

    class ArgsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // Simplest Case
            TransformTestCase(
                """
                SELECT a AS b 
                FROM foo
                ORDER BY b
            """,
                """
                SELECT a AS b 
                FROM foo
                ORDER BY a
            """
            ),
            // Different Projection Aliases
            TransformTestCase(
                """
                SELECT a AS b 
                FROM (
                  SELECT c AS d
                  FROM e
                  ORDER BY d
                )
                ORDER BY b
            """,
                """
                SELECT a AS b 
                FROM (
                  SELECT c AS d
                  FROM e
                  ORDER BY c
                )
                ORDER BY a
            """
            ),
            // Same projection alias
            TransformTestCase(
                """
                SELECT a AS b 
                FROM (
                  SELECT c AS b
                  FROM e
                  ORDER BY b
                )
                ORDER BY b
            """,
                """
                SELECT a AS b 
                FROM (
                  SELECT c AS b
                  FROM e
                  ORDER BY c
                )
                ORDER BY a
            """
            ),
            // Complex projection expressions with same alias
            TransformTestCase(
                """
                SELECT a + b AS b 
                FROM (
                  SELECT b + a AS b
                  FROM e
                  ORDER BY b
                )
                ORDER BY b
            """,
                """
                SELECT a + b AS b 
                FROM (
                  SELECT b + a AS b
                  FROM e
                  ORDER BY b + a
                )
                ORDER BY a + b
            """
            ),
            // Projection Aliases are lower-case while ORDER BY sort spec is case sensitive
            TransformTestCase(
                """
                SELECT a + b AS b 
                FROM (
                  SELECT b + a AS b
                  FROM e
                  ORDER BY b
                )
                ORDER BY "b"
            """,
                """
                SELECT a + b AS b 
                FROM (
                  SELECT b + a AS b
                  FROM e
                  ORDER BY b + a
                )
                ORDER BY a + b
            """
            ),
            // Projection Aliases are Case Sensitive while ORDER BY sort spec is NOT
            TransformTestCase(
                """
                SELECT a + b AS "B"
                FROM (
                  SELECT b + a AS "B"
                  FROM e
                  ORDER BY b
                )
                ORDER BY b
            """,
                """
                SELECT a + b AS "B"
                FROM (
                  SELECT b + a AS "B"
                  FROM e
                  ORDER BY b + a
                )
                ORDER BY a + b
            """
            ),
            // Projection Aliases are Case Insensitive while ORDER BY sort spec is Sensitive
            TransformTestCase(
                """
                SELECT a + b AS b 
                FROM (
                  SELECT b + a AS b
                  FROM e
                  ORDER BY b
                )
                ORDER BY "B"
            """,
                """
                SELECT a + b AS b 
                FROM (
                  SELECT b + a AS b
                  FROM e
                  ORDER BY b + a
                )
                ORDER BY "B"
            """
            ),
            // Projection Aliases and ORDER BY sort specs are Case Insensitive, but sort specs have different cases
            TransformTestCase(
                """
                SELECT a + b AS b 
                FROM (
                  SELECT b + a AS b
                  FROM e
                  ORDER BY b
                )
                ORDER BY B
            """,
                """
                SELECT a + b AS b 
                FROM (
                  SELECT b + a AS b
                  FROM e
                  ORDER BY b + a
                )
                ORDER BY a + b
            """
            ),
            // Multiple Sort Specs
            TransformTestCase(
                """
                SELECT a + b AS b 
                FROM (
                  SELECT b + a AS b
                  FROM e
                  ORDER BY b
                )
                ORDER BY B, a, b
            """,
                """
                SELECT a + b AS b 
                FROM (
                  SELECT b + a AS b
                  FROM e
                  ORDER BY b + a
                )
                ORDER BY a + b, a, a + b
            """
            ),
            TransformTestCase(
                """
                SELECT (a * -1) AS a
                FROM << { 'a': 1 }, { 'a': 2 } >>
                ORDER BY a
            """,
                """
                SELECT (a * -1) AS a
                FROM << { 'a': 1 }, { 'a': 2 } >>
                ORDER BY (a * -1)
            """
            ),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProvider::class)
    fun test(tc: TransformTestCase) = runTestForIdempotentTransform(tc, OrderBySortSpecVisitorTransform())
}
