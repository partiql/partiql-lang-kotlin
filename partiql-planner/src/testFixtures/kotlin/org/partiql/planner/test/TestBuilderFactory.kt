package org.partiql.planner.test

/**
 * A factory for creating [TestBuilder] instances based on the type of test.
 */
interface TestBuilderFactory {

    /**
     * Creates a [TestBuilder] instance based on the given type.
     *
     * @param type The type of test to create a [TestBuilder] for.
     * @return The [TestBuilder] instance or `null` if the type is not supported.
     */
    infix operator fun get(type: String): TestBuilder?
}
