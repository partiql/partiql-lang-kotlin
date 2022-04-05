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
     * Builder block for building [CompileOptions].
     */
    val compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },

    /**
     * Allows each test to configure its pipeline.
     */
    val compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
) {
    constructor(
        query: String,
        expectedSql: String,
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
    ) : this(null, query, expectedSql, compileOptionsBuilderBlock, compilerPipelineBuilderBlock)

    /** This will show up in the IDE's test runner. */
    override fun toString() = when {
        groupName != null -> "$groupName : $sqlUnderTest"
        else -> "$sqlUnderTest"
    }


}
