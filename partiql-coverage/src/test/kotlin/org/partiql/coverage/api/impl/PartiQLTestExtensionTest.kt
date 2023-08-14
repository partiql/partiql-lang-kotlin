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

package org.partiql.coverage.api.impl

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExecutableInvoker
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstances
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.platform.commons.PreconditionViolationException
import org.partiql.coverage.api.PartiQLTest
import org.partiql.coverage.api.PartiQLTestCase
import org.partiql.coverage.api.PartiQLTestProvider
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.PartiQLResult
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.util.Optional
import java.util.function.Function
import java.util.stream.Stream

class PartiQLTestExtensionTest {
    private val extension = PartiQLTestExtension()

    @ParameterizedTest
    @ArgumentsSource(InvalidSignaturesProvider::class)
    fun testInvalidSignatures(extensionContext: ExtensionContext) {
        assertThrows<PreconditionViolationException> {
            extension.supportsTestTemplate(extensionContext)
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValidSignaturesProvider::class)
    fun testValidSignatures(extensionContext: ExtensionContext) {
        assert(extension.supportsTestTemplate(extensionContext))
    }

    class MockProvider(override val statement: String) : PartiQLTestProvider {
        override fun getTestCases(): Iterable<PartiQLTestCase> = listOf()
        override fun getPipelineBuilder(): CompilerPipeline.Builder? = null
    }

    class ValidSignaturesProvider : ArgumentsProvider {
        @Disabled
        @PartiQLTest(provider = MockProvider::class)
        @JvmName("test1")
        @Suppress("UNUSED")
        fun test1(tc: PartiQLTestCase, result: PartiQLResult) {
        }

        @Disabled
        @PartiQLTest(provider = MockProvider::class)
        @JvmName("test2")
        @Suppress("UNUSED")
        fun test2(tc: PartiQLTestCase, result: PartiQLResult.Value) {
        }

        @Disabled
        @PartiQLTest(provider = MockProvider::class)
        @JvmName("test3")
        @Suppress("UNUSED")
        fun test3(tc: ValidTestCase, result: PartiQLResult.Delete) {
        }

        class ValidTestCase(override val session: EvaluationSession) : PartiQLTestCase

        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> = listOf(
            AbstractExtensionContext(ValidSignaturesProvider::class.java, "test1", PartiQLTestCase::class.java, PartiQLResult::class.java),
            AbstractExtensionContext(ValidSignaturesProvider::class.java, "test2", PartiQLTestCase::class.java, PartiQLResult.Value::class.java),
            AbstractExtensionContext(ValidSignaturesProvider::class.java, "test3", ValidTestCase::class.java, PartiQLResult.Delete::class.java),
        ).map { Arguments.of(it) }.stream()
    }

    class InvalidSignaturesProvider : ArgumentsProvider {

        @Disabled
        @PartiQLTest(provider = MockProvider::class)
        @JvmName("test1")
        @Suppress("UNUSED")
        fun test1() {
        }

        @Disabled
        @PartiQLTest(provider = MockProvider::class)
        @JvmName("test2")
        @Suppress("UNUSED")
        fun test2(tc: InvalidTestCase, result: InvalidResult) {
        }

        @Disabled
        @PartiQLTest(provider = MockProvider::class)
        @JvmName("test3")
        @Suppress("UNUSED")
        fun test3(tc: PartiQLTestCase, result: InvalidResult) {
        }

        @Disabled
        @PartiQLTest(provider = MockProvider::class)
        @JvmName("test4")
        @Suppress("UNUSED")
        fun test4(tc: InvalidTestCase, result: PartiQLResult) {
        }

        @Disabled
        @PartiQLTest(provider = MockProvider::class)
        @JvmName("test5")
        @Suppress("UNUSED")
        fun test5(result: PartiQLResult, tc: PartiQLTestCase) {
        }

        @Disabled
        @PartiQLTest(provider = MockProvider::class)
        @JvmName("test6")
        @Suppress("UNUSED")
        fun test6(tc: PartiQLTestCase, result: PartiQLResult, other: Int) {
        }

        class InvalidTestCase(val id: Int)
        class InvalidResult(val id: Int)

        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> = listOf(
            AbstractExtensionContext(InvalidSignaturesProvider::class.java, "test1"),
            AbstractExtensionContext(
                InvalidSignaturesProvider::class.java,
                "test2",
                InvalidTestCase::class.java,
                InvalidResult::class.java
            ),
            AbstractExtensionContext(
                InvalidSignaturesProvider::class.java,
                "test3",
                PartiQLTestCase::class.java,
                InvalidResult::class.java
            ),
            AbstractExtensionContext(
                InvalidSignaturesProvider::class.java,
                "test4",
                InvalidTestCase::class.java,
                PartiQLResult::class.java
            ),
            AbstractExtensionContext(
                InvalidSignaturesProvider::class.java,
                "test5",
                PartiQLResult::class.java,
                PartiQLTestCase::class.java
            ),
            AbstractExtensionContext(
                InvalidSignaturesProvider::class.java,
                "test6",
                PartiQLTestCase::class.java,
                PartiQLResult::class.java,
                Int::class.java
            )
        ).map { Arguments.of(it) }.stream()
    }

    /**
     * Implementation of [ExtensionContext] that only cares to return the wrapped test [Method].
     */
    private class AbstractExtensionContext(clazz: Class<*>, methodName: String, vararg args: Class<*>) :
        ExtensionContext {
        val method: Method = clazz.getDeclaredMethod(methodName, *args)
        override fun getTestMethod(): Optional<Method> = Optional.of(method)

        override fun getParent(): Optional<ExtensionContext> {
            TODO("Not yet implemented")
        }

        override fun getRoot(): ExtensionContext {
            TODO("Not yet implemented")
        }

        override fun getUniqueId(): String {
            TODO("Not yet implemented")
        }

        override fun getDisplayName(): String {
            TODO("Not yet implemented")
        }

        override fun getTags(): MutableSet<String> {
            TODO("Not yet implemented")
        }

        override fun getElement(): Optional<AnnotatedElement> {
            TODO("Not yet implemented")
        }

        override fun getTestClass(): Optional<Class<*>> {
            TODO("Not yet implemented")
        }

        override fun getTestInstanceLifecycle(): Optional<TestInstance.Lifecycle> {
            TODO("Not yet implemented")
        }

        override fun getTestInstance(): Optional<Any> {
            TODO("Not yet implemented")
        }

        override fun getTestInstances(): Optional<TestInstances> {
            TODO("Not yet implemented")
        }

        override fun getExecutionException(): Optional<Throwable> {
            TODO("Not yet implemented")
        }

        override fun getConfigurationParameter(key: String?): Optional<String> {
            TODO("Not yet implemented")
        }

        override fun <T : Any?> getConfigurationParameter(
            key: String?,
            transformer: Function<String, T>?
        ): Optional<T> {
            TODO("Not yet implemented")
        }

        override fun publishReportEntry(map: MutableMap<String, String>?) {
            TODO("Not yet implemented")
        }

        override fun getStore(namespace: ExtensionContext.Namespace?): ExtensionContext.Store {
            return object : ExtensionContext.Store {
                override fun get(key: Any?): Any {
                    TODO("Not yet implemented")
                }

                override fun <V : Any?> get(key: Any?, requiredType: Class<V>?): V {
                    TODO("Not yet implemented")
                }

                override fun <K : Any?, V : Any?> getOrComputeIfAbsent(key: K, defaultCreator: Function<K, V>?): Any {
                    TODO("Not yet implemented")
                }

                override fun <K : Any?, V : Any?> getOrComputeIfAbsent(
                    key: K,
                    defaultCreator: Function<K, V>?,
                    requiredType: Class<V>?
                ): V {
                    TODO("Not yet implemented")
                }

                override fun put(key: Any?, value: Any?) {
                    // Do nothing
                }

                override fun remove(key: Any?): Any {
                    TODO("Not yet implemented")
                }

                override fun <V : Any?> remove(key: Any?, requiredType: Class<V>?): V {
                    TODO("Not yet implemented")
                }
            }
        }

        override fun getExecutionMode(): ExecutionMode {
            TODO("Not yet implemented")
        }

        override fun getExecutableInvoker(): ExecutableInvoker {
            TODO("Not yet implemented")
        }
    }
}
