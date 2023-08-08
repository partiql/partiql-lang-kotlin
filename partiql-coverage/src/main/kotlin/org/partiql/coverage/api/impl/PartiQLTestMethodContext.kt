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
import java.lang.Exception
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.Optional

/**
 * Encapsulates access to the parameters of a parameterized test method and
 * caches the converters and aggregators used to resolve them.
 *
 * @since 5.3
 */
internal class PartiQLTestMethodContext(testMethod: Method) {
    private val parameters: Array<Parameter> = testMethod.parameters
    private val resolvers: Array<Resolver?> = arrayOfNulls(parameters.size)

    /**
     * Determine if the [Method] represented by this context has a
     * *potentially* valid signature (i.e., formal parameter
     * declarations) with regard to aggregators.
     *
     * This method takes a best-effort approach at enforcing the following
     * policy for parameterized test methods that accept aggregators as arguments.
     *
     *
     *  1. zero or more *indexed arguments* come first.
     *  1. zero or more *aggregators* come next.
     *  1. zero or more arguments supplied by other `ParameterResolver`
     * implementations come last.
     *
     *
     * @return `true` if the method has a potentially valid signature
     */
    fun hasPotentiallyValidSignature(): Boolean {
        if (parameters.size != 2) { return false }
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
            return (ArgumentsAccessor::class.java.isAssignableFrom(parameter.type)
                || AnnotationUtils.isAnnotated(parameter, AggregateWith::class.java))
        }

        private fun parameterResolutionException(
            message: String, cause: Exception,
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