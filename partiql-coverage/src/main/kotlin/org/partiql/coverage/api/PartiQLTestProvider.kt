/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.coverage.api

import org.partiql.lang.CompilerPipeline

/**
 * Provides test cases ([getTestCases]) for a particular PartiQL [statement]. This is used by the [PartiQLTest]
 * annotation to test PartiQL source and generate coverage reports. Each [PartiQLTestProvider] should represent a
 * single PartiQL statement and all of its associated tests.
 */
public interface PartiQLTestProvider {

    /**
     * The PartiQL statement to test.
     */
    public val statement: String

    /**
     * Test cases to test the PartiQL [statement].
     */
    public fun getTestCases(): Iterable<PartiQLTestCase>

    /**
     * If this function returns a non-null [CompilerPipeline.Builder], the [PartiQLTest] annotation will use the builder
     * to build a [CompilerPipeline] with coverage statistics enabled. If this is specified to be null, the default
     * [CompilerPipeline.builder] will be used.
     */
    public fun getPipelineBuilder(): CompilerPipeline.Builder?
}
