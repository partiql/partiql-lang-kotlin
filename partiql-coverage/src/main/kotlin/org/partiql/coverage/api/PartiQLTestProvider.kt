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
