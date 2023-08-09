package org.partiql.coverage.api.impl

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.jupiter.params.support.AnnotationConsumerInitializer
import org.junit.platform.commons.JUnitException
import org.junit.platform.commons.util.AnnotationUtils
import org.junit.platform.commons.util.ExceptionUtils
import org.junit.platform.commons.util.Preconditions
import org.junit.platform.commons.util.ReflectionUtils
import org.partiql.coverage.api.PartiQLTest
import org.partiql.coverage.api.PartiQLTestCase
import org.partiql.coverage.api.PartiQLTestProvider
import org.partiql.lang.CompilerPipeline.Companion.builder
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.util.ConfigurableExprValueFormatter
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * JUnit extension that is invoked on test methods annotated with [PartiQLTest].
 */
internal class PartiQLTestExtension : TestTemplateInvocationContextProvider {
    override fun supportsTestTemplate(context: ExtensionContext): Boolean {
        if (!context.testMethod.isPresent) { return false }

        val testMethod = context.testMethod.get()
        if (!AnnotationUtils.isAnnotated(testMethod, PartiQLTest::class.java)) { return false }

        val methodContext = PartiQLTestMethodContext(testMethod)
        Preconditions.condition(methodContext.hasPotentiallyValidSignature()) {
            "@PartiQLTest method [${testMethod.toGenericString()}] has an invalid signature."
        }
        getStore(context).put(METHOD_CONTEXT_KEY, methodContext)
        return true
    }

    override fun provideTestTemplateInvocationContexts(
        extensionContext: ExtensionContext
    ): Stream<TestTemplateInvocationContext> {
        val templateMethod = extensionContext.requiredTestMethod
        val methodContext = getStore(extensionContext)[METHOD_CONTEXT_KEY, PartiQLTestMethodContext::class.java]
        val invocationCount = AtomicLong(0)

        // Get Test/Provider Information
        val annotation = AnnotationUtils.findAnnotation(
            templateMethod,
            PartiQLTest::class.java
        ).get()
        val prov = instantiateArgumentsProvider(annotation.provider.java)

        // Create Pipeline and Compile
        val pipelineBuilder = prov.getPipelineBuilder() ?: builder()
        val pipeline = pipelineBuilder.withCoverageStatistics(true).build()
        val expression = pipeline.compile(prov.statement)

        // Initialize Report
        val report: MutableMap<String, String> = HashMap()

        // Get Provider Name
        val packageName = prov.javaClass.getPackage().name
        val className = prov.javaClass.name
        val classNames = className.split("\\.").toTypedArray()
        val actualClassName = classNames[classNames.size - 1]
        report[ReportKey.PACKAGE_NAME] = packageName
        report[ReportKey.PROVIDER_NAME] = actualClassName

        // Get Static Coverage Statistics
        val coverageStructure = expression.coverageStructure ?: error("Expected to find a CoverageStructure, however, none was provided.")

        // Add Total Decision Count to Coverage Report
        val branchCount = coverageStructure.branches.size
        report[ReportKey.BRANCH_COUNT] = branchCount.toString()
        val conditionCount = coverageStructure.branchConditions.size
        report[ReportKey.BRANCH_CONDITION_COUNT] = conditionCount.toString()

        // Original Query
        report[ReportKey.ORIGINAL_STATEMENT] = prov.statement

        // Add Branch Information to Report
        coverageStructure.branches.entries.forEach { (key, value) ->
            report[ReportKey.LINE_NUMBER_OF_TARGET_PREFIX + ReportKey.DELIMITER + key] = value.line.toString()
            report[ReportKey.OUTCOME_OF_TARGET_PREFIX + ReportKey.DELIMITER + key] = value.outcome.toString()
            report[ReportKey.TYPE_OF_TARGET_PREFIX + ReportKey.DELIMITER + key] = value.type.toString()
            report[ReportKey.COVERAGE_TARGET_PREFIX + ReportKey.DELIMITER + key] = ReportKey.CoverageTarget.BRANCH.toString()
        }

        // Add Branch Condition Information to Report
        coverageStructure.branchConditions.entries.forEach { (key, value) ->
            report[ReportKey.LINE_NUMBER_OF_TARGET_PREFIX + ReportKey.DELIMITER + key] = value.line.toString()
            report[ReportKey.OUTCOME_OF_TARGET_PREFIX + ReportKey.DELIMITER + key] = value.outcome.toString()
            report[ReportKey.TYPE_OF_TARGET_PREFIX + ReportKey.DELIMITER + key] = value.type.toString()
            report[ReportKey.COVERAGE_TARGET_PREFIX + ReportKey.DELIMITER + key] = ReportKey.CoverageTarget.BRANCH_CONDITION.toString()
        }

        // Compute Coverage Metrics
        val tests: Stream<Pair<PartiQLTestCase, PartiQLResult>> = Stream.of(prov)
            .map { provider: PartiQLTestProvider -> AnnotationConsumerInitializer.initialize(templateMethod, provider) }
            .flatMap { provider: PartiQLTestProvider -> arguments(provider) }
            .map { testCase: PartiQLTestCase ->
                val result = expression.evaluate(testCase.session)

                // NOTE: This is a hack to materialize data, then retrieve CoverageData.
                val str = when (result) {
                    is PartiQLResult.Value -> ConfigurableExprValueFormatter.standard.format(result.value)
                    is PartiQLResult.Delete -> TODO("@PartiQLTest does not yet support unit testing of Delete.")
                    is PartiQLResult.Explain.Domain -> TODO("@PartiQLTest does not yet support unit testing of Explain.")
                    is PartiQLResult.Insert -> TODO("@PartiQLTest does not yet support unit testing of Insert.")
                    is PartiQLResult.Replace -> TODO("@PartiQLTest does not yet support unit testing of Replace.")
                }
                assert(str.length > -1)

                val stats = result.getCoverageData() ?: error("Expected to find CoverageData, however, none was provided.")

                // Add Executed Decisions (Size) to Coverage Report
                // NOTE: This only works because we share the same CoverageCompiler. Therefore, we overwrite some things.
                stats.branchConditionCount.forEach { (key, value) ->
                    report[ReportKey.TARGET_COUNT_PREFIX + ReportKey.DELIMITER + key] = value.toString()
                }
                stats.branchCount.forEach { (key, value) ->
                    report[ReportKey.TARGET_COUNT_PREFIX + ReportKey.DELIMITER + key] = value.toString()
                }
                testCase to result
            }

        // Invoke Test Methods
        return tests.map { (tc, result) ->
            invocationCount.incrementAndGet()
            createInvocationContext(methodContext, arrayOf(tc, result), invocationCount.toInt())
        }.onClose {
            Preconditions.condition(invocationCount.get() > 0, "Config Error: At least one test case required for @PartiQLTest")
        }.onClose {
            // Publish Coverage Metrics
            extensionContext.publishReportEntry(report)
        }
    }

    private fun instantiateArgumentsProvider(clazz: Class<out PartiQLTestProvider>): PartiQLTestProvider {
        return try {
            ReflectionUtils.newInstance(clazz)
        } catch (ex: Exception) {
            if (ex is NoSuchMethodException) {
                val message = String.format(
                    "Failed to find a no-argument constructor for PartiQLTestProvider [%s]. " +
                        "Please ensure that a no-argument constructor exists and " +
                        "that the class is either a top-level class or a static nested class",
                    clazz.name
                )
                throw JUnitException(message, ex)
            }
            throw ex
        }
    }

    companion object {
        private const val METHOD_CONTEXT_KEY = "context"
        private fun getStore(context: ExtensionContext): ExtensionContext.Store {
            return context.getStore(
                ExtensionContext.Namespace.create(
                    PartiQLTestExtension::class.java, context.requiredTestMethod
                )
            )
        }

        private fun createInvocationContext(
            methodContext: PartiQLTestMethodContext,
            arguments: Array<Any>,
            invocationIndex: Int
        ): TestTemplateInvocationContext {
            return PartiQLTestInvocationContext(methodContext, arguments, invocationIndex)
        }

        private fun arguments(
            provider: PartiQLTestProvider,
        ): Stream<out PartiQLTestCase> {
            return try {
                StreamSupport.stream(provider.getTestCases().spliterator(), false)
            } catch (e: Exception) {
                throw ExceptionUtils.throwAsUncheckedException(e)
            }
        }
    }
}
