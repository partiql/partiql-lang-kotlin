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

import org.junit.jupiter.api.Named
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolutionException
import org.junit.jupiter.api.extension.ParameterResolver
import java.lang.AutoCloseable
import java.util.Arrays
import java.util.concurrent.atomic.AtomicInteger

internal class PartiQLTestParameterResolver(
    private val methodContext: PartiQLTestMethodContext,
    private val arguments: Array<Any>,
    private val invocationIndex: Int
) : ParameterResolver, AfterTestExecutionCallback {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        val declaringExecutable = parameterContext.declaringExecutable
        val testMethod = extensionContext.testMethod.orElse(null)
        val parameterIndex = parameterContext.index

        // Not a @PartiQLTest method?
        if (declaringExecutable != testMethod) { return false }

        return parameterIndex < arguments.size
    }

    @Throws(ParameterResolutionException::class)
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return methodContext.resolve(parameterContext, extractPayloads(arguments), invocationIndex)
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val store = context.getStore(NAMESPACE)
        val argumentIndex = AtomicInteger()
        Arrays.stream(arguments) //
            .filter { obj: Any? -> AutoCloseable::class.java.isInstance(obj) } //
            .map { obj: Any? -> AutoCloseable::class.java.cast(obj) } //
            .map { autoCloseable: AutoCloseable -> CloseableArgument(autoCloseable) } //
            .forEach { closeable: CloseableArgument? ->
                store.put(
                    "closeableArgument#" + argumentIndex.incrementAndGet(),
                    closeable
                )
            }
    }

    private class CloseableArgument(private val autoCloseable: AutoCloseable) :
        ExtensionContext.Store.CloseableResource {
        @Throws(Throwable::class)
        override fun close() {
            autoCloseable.close()
        }
    }

    private fun extractPayloads(arguments: Array<Any>): Array<Any?> {
        return Arrays.stream(arguments) //
            .map { argument: Any? ->
                if (argument is Named<*>) {
                    return@map argument.payload
                }
                argument
            } //
            .toArray()
    }

    companion object {
        private val NAMESPACE = ExtensionContext.Namespace.create(
            PartiQLTestParameterResolver::class.java
        )
    }
}
