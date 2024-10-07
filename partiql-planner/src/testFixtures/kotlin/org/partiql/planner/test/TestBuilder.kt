package org.partiql.planner.test

import com.amazon.ionelement.api.StructElement

/**
 * A [TestBuilder] is used to construct a [Test] object.
 * @see Test
 * @see TestId
 * @see StructElement
 * @see PartiQLTestProvider
 */
interface TestBuilder {

    /**
     * Sets the unique identifier for a [Test].
     *
     * @see TestId
     */
    fun id(id: TestId): TestBuilder

    /**
     * Configures the [Test] with a [StructElement] and a [PartiQLTestProvider].
     *
     * @see StructElement
     * @see PartiQLTestProvider
     * @see Test
     */
    fun config(body: StructElement, statementProvider: PartiQLTestProvider): TestBuilder

    /**
     * Builds and returns a [Test] object.
     *
     * @see Test
     */
    fun build(): Test
}
