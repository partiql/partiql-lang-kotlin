package org.partiql.runner.test

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

private val PARTIQL_EVAL_TEST_DATA_DIR = System.getenv("PARTIQL_EVAL_TESTS_DATA")
private val PARTIQL_EVAL_EQUIV_TEST_DATA_DIR = System.getenv("PARTIQL_EVAL_EQUIV_TESTS_DATA")

/**
 * Reduces some of the boilerplate associated with the style of parameterized testing frequently
 * utilized in this package.
 *
 * Since JUnit5 requires `@JvmStatic` on its `@MethodSource` argument factory methods, this requires all
 * of the argument lists to reside in the companion object of a test class.  This can be annoying since it
 * forces the test to be separated from its tests cases.
 *
 * Classes that derive from this class can be defined near the `@ParameterizedTest` functions instead.
 */
sealed class TestProvider(private val root: String) : ArgumentsProvider {

    @Throws(Exception::class)
    override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments>? {
        return TestLoader.load(root).map { Arguments.of(it) }.stream()
    }

    /**
     * Evaluation tests
     *
     */
    class Eval : TestProvider(PARTIQL_EVAL_TEST_DATA_DIR)

    /**
     * Equivalence tests
     */
    class Equiv : TestProvider(PARTIQL_EVAL_EQUIV_TEST_DATA_DIR)
}
