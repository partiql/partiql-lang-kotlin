package org.partiql.ionschema.util

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

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
abstract class ArgumentsProviderBase : ArgumentsProvider {

    abstract fun getParameters(): List<Any>

    @Throws(Exception::class)
    override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments>? {
        return getParameters().map { Arguments.of(it) }.stream()
    }
}
