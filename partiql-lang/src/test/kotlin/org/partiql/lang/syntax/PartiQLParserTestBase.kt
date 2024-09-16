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

package org.partiql.lang.syntax

import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.CUSTOM_TEST_TYPES
import org.partiql.lang.TestBase
import org.partiql.lang.domains.PartiqlAst

abstract class PartiQLParserTestBase : TestBase() {

    companion object {
        val testCache = mutableMapOf<String, ParserTest>()
    }

    /**
     * We can change the parser target for an entire test suite by overriding this list.
     */
    open val targets = arrayOf(ParserTarget.DEFAULT)

    /**
     * Executes a test block for each target.
     */
    inline fun forEachTarget(block: ParserTarget.() -> Unit) = targets.forEach { it.block() }

    enum class ParserTarget(val parser: Parser) {
        DEFAULT(PartiQLParserBuilder().customTypes(CUSTOM_TEST_TYPES).build()),
        EXPERIMENTAL(PartiQLParserBuilder.experimental().customTypes(CUSTOM_TEST_TYPES).build()),
    }

    /**
     * This method is used by test cases for parsing a string.
     * The test are performed with PIG AST.
     * The expected PIG AST is a string.
     */
    protected fun assertExpression(
        source: String,
        expectedPigAst: String,
    ): Unit = forEachTarget {
        assert(ParserTest(source, source, true))
    }

    /**
     * This method is used by test cases for parsing a string.
     * The test are performed with only PIG AST.
     * The expected PIG AST is a PIG builder.
     */
    protected fun assertExpression(
        source: String,
        expectedPigBuilder: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode,
    ) {
        assert(ParserTest(source, source, true))
    }

    protected fun checkInputThrowingParserException(
        input: String,
        errorCode: ErrorCode,
        expectErrorContextValues: Map<Property, Any>,
        assertContext: Boolean = true,
    ): Unit = forEachTarget {
        assert(ParserTest(input, input, false))
    }

    protected fun assertFailure(input: String) = forEachTarget {
        assert(ParserTest(input, input, false))
    }

    private fun assert(test: ParserTest) {
        val cache = testCache[test.name]
        if (cache != null) {
            // If test exists, assert that the expectation is the same
            assertEquals(test, cache)
        } else {
            // Print
            test.write("all.ion")
            // Insert into cache
            testCache[test.name] = test
        }
    }
}
