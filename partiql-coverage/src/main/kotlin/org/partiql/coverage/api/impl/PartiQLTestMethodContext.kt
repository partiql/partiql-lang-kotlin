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

import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolutionException
import org.junit.jupiter.params.aggregator.AggregateWith
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.converter.ArgumentConverter
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.converter.DefaultArgumentConverter
import org.junit.jupiter.params.support.AnnotationConsumerInitializer
import org.junit.platform.commons.util.AnnotationUtils
import org.junit.platform.commons.util.ReflectionUtils
import org.junit.platform.commons.util.StringUtils
import org.partiql.coverage.api.PartiQLTestCase
import org.partiql.lang.eval.PartiQLResult
import java.lang.Exception
import java.lang.reflect.Method
import java.lang.reflect.Parameter

/**
 * Encapsulates access to the parameters of a parameterized test method and
 * caches the converters used to resolve them.
 */
internal class PartiQLTestMethodContext(testMethod: Method) {
    private val parameters: Array<Parameter> = testMethod.parameters
    private val resolvers: Array<Resolver?> = arrayOfNulls(parameters.size)

    /**
     * @return `true` if the method has a potentially valid signature
     */
    fun hasPotentiallyValidSignature(): Boolean {
        if (parameters.size != 2) { return false }
        if (PartiQLTestCase::class.java.isAssignableFrom(parameters[0].type).not()) {
            return false
        }
        if (PartiQLResult::class.java.isAssignableFrom(parameters[1].type).not()) {
            return false
        }
        if (parameters.any { isAggregator(it) }) { return false }
        return true
    }

    /**
     * Resolve the parameter for the supplied context using the supplied
     * arguments.
     */
    fun resolve(parameterContext: ParameterContext, arguments: Array<Any?>, invocationIndex: Int): Any {
        return getResolver(parameterContext)!!.resolve(parameterContext, arguments, invocationIndex)
    }

    private fun getResolver(parameterContext: ParameterContext): Resolver? {
        val index = parameterContext.index
        if (resolvers[index] == null) {
            resolvers[index] = ResolverType.CONVERTER.createResolver(parameterContext)
        }
        return resolvers[index]
    }

    internal enum class ResolverType {
        CONVERTER {
            override fun createResolver(parameterContext: ParameterContext): Resolver? {
                return try { // @formatter:off
                    AnnotationUtils.findAnnotation(parameterContext.parameter, ConvertWith::class.java)
                        .map { obj: ConvertWith -> obj.value.java }
                        .map { clazz: Class<out ArgumentConverter> ->
                            ReflectionUtils.newInstance(clazz)
                        }
                        .map { converter: ArgumentConverter ->
                            AnnotationConsumerInitializer.initialize(
                                parameterContext.parameter,
                                converter
                            )
                        }
                        .map { argumentConverter: ArgumentConverter -> Converter(argumentConverter) }
                        .orElse(Converter.DEFAULT)
                } // @formatter:on
                catch (ex: Exception) {
                    throw parameterResolutionException("Error creating ArgumentConverter", ex, parameterContext)
                }
            }
        };

        abstract fun createResolver(parameterContext: ParameterContext): Resolver?
    }

    internal interface Resolver {
        fun resolve(parameterContext: ParameterContext, arguments: Array<Any?>, invocationIndex: Int): Any
    }

    internal class Converter(private val argumentConverter: ArgumentConverter) : Resolver {
        override fun resolve(parameterContext: ParameterContext, arguments: Array<Any?>, invocationIndex: Int): Any {
            val argument = arguments[parameterContext.index]
            return try {
                argumentConverter.convert(argument, parameterContext)
            } catch (ex: Exception) {
                throw parameterResolutionException("Error converting parameter", ex, parameterContext)
            }
        }

        companion object {
            val DEFAULT = Converter(DefaultArgumentConverter.INSTANCE)
        }
    }

    companion object {
        /**
         * Determine if the supplied [Parameter] is an aggregator (i.e., of
         * type [ArgumentsAccessor] or annotated with [AggregateWith]).
         *
         * @return `true` if the parameter is an aggregator
         */
        @JvmStatic
        private fun isAggregator(parameter: Parameter): Boolean {
            return (
                ArgumentsAccessor::class.java.isAssignableFrom(parameter.type) ||
                    AnnotationUtils.isAnnotated(parameter, AggregateWith::class.java)
                )
        }

        private fun parameterResolutionException(
            message: String,
            cause: Exception,
            parameterContext: ParameterContext
        ): ParameterResolutionException {
            var fullMessage = message + " at index " + parameterContext.index
            if (StringUtils.isNotBlank(cause.message)) {
                fullMessage += ": " + cause.message
            }
            return ParameterResolutionException(fullMessage, cause)
        }
    }
}
