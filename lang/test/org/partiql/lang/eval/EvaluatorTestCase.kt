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

import org.partiql.lang.CompilerPipeline

/**
 * Determines the format and equivalnce that is used to perform assertions on the expected result of
 * an [EvaluatorTestCase].
 *
 * TODO: ideally, all the test cases actually use a variation of Ion equivalence that recognizes the proper semantics
 * regarding `$partiql_bag::[]` (the order of items in bags is not relevant).  PARTIQL's equivalence supports bags
 * but is otherwise too "loosey-goosey" in that it coerces values of mismatched but similar data types before comparison
 * (i.e. blobs and clobs, strings and symbols, as well as ints, floats and decimals).  Today, unfortunately, the
 * expected values of *all* of our tests utilize one of these two sub-optimal approaches--something that is currently
 * outside of the scope of current work.  Thus, [ExpectedResultMode] was created.
 */
enum class ExpectedResultMode {
    /**
     * The expected value is expressed in Ion but does not contain the `$partiql_bag` or `$partiql_missing` annotations.
     * Otherwise, standard Ion equivalence is used to assert the expected result matches.
     *
     * This is for older test cases that existed prior to these attributes and which not been updated yet.  Yes, this is
     * technical debt, but at this time it is easier to support them than it is to migrate each test individually.
     */
    ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS,

    /**
     * The expected value is expressed using PartiQL syntax, which is evaluated under the same pipeline and compile
     * options and session as the query under test. PartiQL equivalence is used to compare the result.
     */
    PARTIQL
}

/**
 * Defines a test case for query evaluation.
 *
 * TODO: reorder these properties.
 */
data class EvaluatorTestCase(

    /** The "group" of the tests--this only appears in the IDE's test runner and can be used to identify where in the
     * source code the test is defined.
     */
    val groupName: String? = null,

    /**
     * The query to be evaluated.
     */
    val query: String,

    /**
     * AN expression which will be evaluated to determine the set of expected values that should match the result
     * of [query].
     * DL TODO: rename to expectedLegacyModeResult.
     */
    val expectedResult: String,

    /**
     * Builder block for building [CompileOptions].
     */
    val compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },

    /**
     * Allows each test to configure its pipeline.
     */
    val compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },

    /**
     * How to handle the expected result.
     *
     * @see [ExpectedResultMode]
     */
    val expectedResultMode: ExpectedResultMode = ExpectedResultMode.PARTIQL,
    val expectedPermissiveModeResult: String = expectedResult,
    val excludeLegacySerializerAssertions: Boolean = false,
    val extraResultAssertions: (ExprValue) -> Unit = { }
) {
    // DL TODO: delete this constructor (but reorder the properties and this constructor
    // to match first).
    constructor(
        query: String,
        expectedResult: String,
        expectedResultMode: ExpectedResultMode = ExpectedResultMode.PARTIQL,
        expectedPermissiveModeResult: String = expectedResult,
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        excludeLegacySerializerAssertions: Boolean = false,
        extraResultAssertions: (ExprValue) -> Unit = { }
    ) : this(
        groupName = null,
        query = query,
        expectedResult = expectedResult,
        compileOptionsBuilderBlock = compileOptionsBuilderBlock,
        compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
        expectedResultMode = expectedResultMode,
        expectedPermissiveModeResult = expectedPermissiveModeResult,
        excludeLegacySerializerAssertions = excludeLegacySerializerAssertions,
        extraResultAssertions = extraResultAssertions
    )

    /** This will show up in the IDE's test runner. */
    override fun toString() = when {
        groupName != null -> "$groupName : $query"
        else -> "$query"
    }
}
