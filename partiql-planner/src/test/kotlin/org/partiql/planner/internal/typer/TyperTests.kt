package org.partiql.planner.internal.typer

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.planner.TyperTestBuilder
import org.partiql.planner.test.Test
import org.partiql.planner.test.TestBuilder
import org.partiql.planner.test.TestBuilderFactory
import org.partiql.planner.test.TestProviderBuilder
import java.util.stream.Stream

class TyperTests {

    @ParameterizedTest
    @ArgumentsSource(TestProvider::class)
    fun test(test: Test) {
        test.assert()
    }

    class TestProvider : ArgumentsProvider {
        private val provider = TestProviderBuilder().factory(Factory).build()
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return provider.map { Arguments.of(it) }.stream()
        }
    }

    private object Factory : TestBuilderFactory {
        override fun get(type: String): TestBuilder? {
            return when (type.trim().lowercase()) {
                "type" -> TyperTestBuilder()
                else -> null
            }
        }
    }
}
