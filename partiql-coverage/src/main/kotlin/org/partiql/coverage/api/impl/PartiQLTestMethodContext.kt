package org.partiql.coverage.api.impl

import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolutionException
import org.junit.jupiter.params.aggregator.AggregateWith
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.ArgumentsAggregator
import org.junit.jupiter.params.aggregator.DefaultArgumentsAccessor
import org.junit.jupiter.params.converter.ArgumentConverter
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.converter.DefaultArgumentConverter
import org.junit.jupiter.params.support.AnnotationConsumerInitializer
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.commons.util.AnnotationUtils
import org.junit.platform.commons.util.ReflectionUtils
import org.junit.platform.commons.util.StringUtils
import java.lang.Exception
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.ArrayList
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
    private val resolverTypes: MutableList<ResolverType>

    /**
     * Determine if the [Method] represented by this context has a
     * *potentially* valid signature (i.e., formal parameter
     * declarations) with regard to aggregators.
     *
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
        var indexOfPreviousAggregator = -1
        for (i in 0 until getParameterCount()) {
            if (isAggregator(i)) {
                if (indexOfPreviousAggregator != -1 && i != indexOfPreviousAggregator + 1) {
                    return false
                }
                indexOfPreviousAggregator = i
            }
        }
        return true
    }

    /**
     * Get the number of parameters of the [Method] represented by this
     * context.
     */
    fun getParameterCount(): Int {
        return parameters.size
    }

    /**
     * Get the name of the [Parameter] with the supplied index, if
     * it is present and declared before the aggregators.
     *
     * @return an `Optional` containing the name of the parameter
     */
    fun getParameterName(parameterIndex: Int): Optional<String> {
        if (parameterIndex >= getParameterCount()) {
            return Optional.empty()
        }
        val parameter = parameters[parameterIndex]
        if (!parameter.isNamePresent) {
            return Optional.empty()
        }
        return if (hasAggregator() && parameterIndex >= indexOfFirstAggregator()) {
            Optional.empty()
        } else Optional.of(parameter.name)
    }

    /**
     * Determine if the [Method] represented by this context declares at
     * least one [Parameter] that is an
     * [aggregator][.isAggregator].
     *
     * @return `true` if the method has an aggregator
     */
    fun hasAggregator(): Boolean {
        return resolverTypes.contains(ResolverType.AGGREGATOR)
    }

    /**
     * Determine if the [Parameter] with the supplied index is an
     * aggregator (i.e., of type [ArgumentsAccessor] or annotated with
     * [AggregateWith]).
     *
     * @return `true` if the parameter is an aggregator
     */
    fun isAggregator(parameterIndex: Int): Boolean {
        return resolverTypes[parameterIndex] === ResolverType.AGGREGATOR
    }

    /**
     * Find the index of the first [aggregator][.isAggregator]
     * [Parameter] in the [Method] represented by this context.
     *
     * @return the index of the first aggregator, or `-1` if not found
     */
    fun indexOfFirstAggregator(): Int {
        return resolverTypes.indexOf(ResolverType.AGGREGATOR)
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
            resolvers[index] = resolverTypes[index].createResolver(parameterContext)
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
        },
        AGGREGATOR {
            override fun createResolver(parameterContext: ParameterContext): Resolver? {
                return try { // @formatter:off
                    AnnotationUtils.findAnnotation(parameterContext.parameter, AggregateWith::class.java)
                        .map { obj: AggregateWith -> obj.value.java }
                        .map { clazz: Class<out ArgumentsAggregator> -> ReflectionSupport.newInstance(clazz) }
                        .map { argumentsAggregator: ArgumentsAggregator -> Aggregator(argumentsAggregator) }
                        .orElse(Aggregator.DEFAULT)
                } // @formatter:on
                catch (ex: Exception) {
                    throw parameterResolutionException("Error creating ArgumentsAggregator", ex, parameterContext)
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

    internal class Aggregator(private val argumentsAggregator: ArgumentsAggregator) : Resolver {
        override fun resolve(parameterContext: ParameterContext, arguments: Array<Any?>, invocationIndex: Int): Any {
            val accessor: ArgumentsAccessor = DefaultArgumentsAccessor(parameterContext, invocationIndex, arguments)
            return try {
                argumentsAggregator.aggregateArguments(accessor, parameterContext)
            } catch (ex: Exception) {
                throw parameterResolutionException("Error aggregating arguments for parameter", ex, parameterContext)
            }
        }

        companion object {
            val DEFAULT = Aggregator { accessor: ArgumentsAccessor?, context: ParameterContext? -> accessor }
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

    init {
        resolverTypes = ArrayList(parameters.size)
        for (parameter in parameters) {
            resolverTypes.add(if (isAggregator(parameter)) ResolverType.AGGREGATOR else ResolverType.CONVERTER)
        }
    }
}