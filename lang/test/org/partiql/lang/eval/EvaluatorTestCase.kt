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

    /**
     * The query's expected result when executed in permissive mode.  Defaults to [expectedResult].
     *
     * Some semantics of permissive mode have changed--namely, in permissive mode, MISSING propagates as NULL.
     *
     * Thus, even positive test cases may have a different result.
     */
    val expectedPermissiveModeResult: String = expectedResult,
    val excludeLegacySerializerAssertions: Boolean = false,

    /**
     * When true, after running the test once with compile options unmodified, run the test again in permissive mode.
     *
     * The default is `true` to ensure that permissive mode is tested as thoroughly as legacy mode.  However, some
     * tests explicitly set legacy or permissive mode.  Such tests should set [implicitPermissiveModeTest] to
     * `false`.  Note that, when `false`, [expectedPermissiveModeResult] is ignored.
     */
    val implicitPermissiveModeTest: Boolean = true,

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
        implicitPermissiveModeTest: Boolean = true,
        extraResultAssertions: (ExprValue) -> Unit = { },
    ) : this(
        groupName = null,
        query = query,
        expectedResult = expectedResult,
        compileOptionsBuilderBlock = compileOptionsBuilderBlock,
        compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
        expectedResultMode = expectedResultMode,
        expectedPermissiveModeResult = expectedPermissiveModeResult,
        excludeLegacySerializerAssertions = excludeLegacySerializerAssertions,
        implicitPermissiveModeTest = implicitPermissiveModeTest,
        extraResultAssertions = extraResultAssertions
    )

    /** This will show up in the IDE's test runner. */
    override fun toString() = when {
        groupName != null -> "$groupName : $query"
        else -> "$query"
    }
}
