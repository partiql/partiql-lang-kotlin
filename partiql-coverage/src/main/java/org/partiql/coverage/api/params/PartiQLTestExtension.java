package org.partiql.coverage.api.params;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.*;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.params.support.AnnotationConsumerInitializer;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.partiql.coverage.api.*;
import org.partiql.coverage.api.impl.*;
import org.partiql.lang.*;
import org.partiql.lang.eval.*;

/**
 * TODO
 */
class PartiQLTestExtension implements TestTemplateInvocationContextProvider {

    static final String ARGUMENT_MAX_LENGTH_KEY = "junit.jupiter.params.displayname.argument.maxlength";
    static final String DISPLAY_NAME_PATTERN_KEY = "junit.jupiter.params.displayname.default";
    private static final String METHOD_CONTEXT_KEY = "context";
    private static final String DEFAULT_DISPLAY_NAME = "{default_display_name}";

    private static ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(PartiQLTestExtension.class, context.getRequiredTestMethod()));
    }

    private static TestTemplateInvocationContext createInvocationContext(ParameterizedTestNameFormatter formatter,
                                                                         ParameterizedTestMethodContext methodContext, Object[] arguments, int invocationIndex) {
        return new ParameterizedTestInvocationContext(formatter, methodContext, arguments, invocationIndex);
    }

    private static ParameterizedTestNameFormatter createNameFormatter(ExtensionContext extensionContext, Method templateMethod,
                                                                      ParameterizedTestMethodContext methodContext, String displayName, int argumentMaxLength) {
        // PartiQLTest parameterizedTest = findAnnotation(templateMethod, PartiQLTest.class).get();
        String pattern = "[{index}]";
        pattern = Preconditions.notBlank(pattern.trim(),
                () -> String.format(
                        "Configuration error: @ParameterizedTest on method [%s] must be declared with a non-empty name.",
                        templateMethod));
        return new ParameterizedTestNameFormatter(pattern, displayName, methodContext, argumentMaxLength);
    }

    protected static Stream<? extends PartiQLTestCase> arguments(PartiQLTestProvider provider, ExtensionContext context) {
        try {
            return StreamSupport.stream(provider.getTestCases().spliterator(), false);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsUncheckedException(e);
        }
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        if (!context.getTestMethod().isPresent()) {
            return false;
        }

        Method testMethod = context.getTestMethod().get();
        if (!isAnnotated(testMethod, PartiQLTest.class)) {
            return false;
        }

        ParameterizedTestMethodContext methodContext = new ParameterizedTestMethodContext(testMethod);

        Preconditions.condition(methodContext.hasPotentiallyValidSignature(),
                () -> String.format(
                        "@ParameterizedTest method [%s] declares formal parameters in an invalid order: "
                                + "argument aggregators must be declared after any indexed arguments "
                                + "and before any arguments resolved by another ParameterResolver.",
                        testMethod.toGenericString()));

        getStore(context).put(METHOD_CONTEXT_KEY, methodContext);

        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext) {

        Method templateMethod = extensionContext.getRequiredTestMethod();
        String displayName = extensionContext.getDisplayName();
        ParameterizedTestMethodContext methodContext = getStore(extensionContext).get(METHOD_CONTEXT_KEY, ParameterizedTestMethodContext.class);
        int argumentMaxLength = extensionContext.getConfigurationParameter(ARGUMENT_MAX_LENGTH_KEY, Integer::parseInt).orElse(512);
        ParameterizedTestNameFormatter formatter = createNameFormatter(extensionContext, templateMethod, methodContext, displayName, argumentMaxLength);
        AtomicLong invocationCount = new AtomicLong(0);

        // Get Test/Provider Information
        PartiQLTest annotation = findAnnotation(templateMethod, PartiQLTest.class).get();
        PartiQLTestProvider prov = instantiateArgumentsProvider(annotation.provider());

        // Create Pipeline and Compile
        CompilerPipeline pipeline = CompilerPipeline.standard();
        Expression expression = pipeline.compile(prov.getQuery());

        // Initialize Report
        Map<String, String> report = new HashMap<>();

        // Get Provider Name
        String packageName = prov.getClass().getPackage().getName();
        String className = prov.getClass().getName();
        String[] classNames = className.split("\\.");
        String actualClassName = classNames[classNames.length - 1];
        report.put(ReportKey.PACKAGE_NAME, packageName);
        report.put(ReportKey.PROVIDER_NAME, actualClassName);

        // Get Static Coverage Statistics
        CoverageStructure coverageStructure = expression.getCoverageStructure();

        // Add Total Decision Count to Coverage Report
        int branchCount = coverageStructure != null ? coverageStructure.getBranchCount() : 0;
        report.put(ReportKey.BRANCH_COUNT, String.valueOf(branchCount));
        int conditionCount = coverageStructure != null ? coverageStructure.getConditionCount() : 0;
        report.put(ReportKey.CONDITION_COUNT, String.valueOf(conditionCount));

        // Original Query
        report.put(ReportKey.ORIGINAL_STATEMENT, prov.getQuery());

        // Add Location Information to Report
        Map<String, Integer> branchLocations = coverageStructure != null ? coverageStructure.getBranchLocations() : Collections.emptyMap();
        for (Map.Entry<String, Integer> entry : branchLocations.entrySet()) {
            String key = ReportKey.LINE_NUMBER_OF_BRANCH_PREFIX + entry.getKey();
            report.put(key, String.valueOf(entry.getValue()));
        }

        // Add Location Information to Report
        Map<String, Integer> conditionLocations = coverageStructure != null ? coverageStructure.getConditionLocations() : Collections.emptyMap();
        for (Map.Entry<String, Integer> entry : conditionLocations.entrySet()) {
            String key = ReportKey.LINE_NUMBER_OF_CONDITION_PREFIX + entry.getKey();
            report.put(key, String.valueOf(entry.getValue()));
        }

        // Compute Coverage Metrics
        Stream<Object[]> tests = Stream.of(prov)
                .map(provider -> AnnotationConsumerInitializer.initialize(templateMethod, provider))
                .flatMap(provider -> arguments(provider, extensionContext))
                .map(testCase -> {
                    PartiQLResult result = expression.evaluate(testCase.getSession());
                    CoverageData stats = result.getCoverageData();

                    // Add Executed Decisions (Size) to Coverage Report
                    // NOTE: This only works because we share the same CoverageCompiler. Therefore, we override some things.
                    Map<String, Integer> executedConditions = stats != null ? stats.getConditionCount() : Collections.emptyMap();
                    for (Map.Entry<String, Integer> entry : executedConditions.entrySet()) {
                        String key = ReportKey.RESULT_OF_CONDITION_PREFIX + entry.getKey();
                        report.put(key, String.valueOf(entry.getValue()));
                    }

                    Map<String, Integer> executedBranches = stats != null ? stats.getBranchCount() : Collections.emptyMap();
                    for (Map.Entry<String, Integer> entry : executedBranches.entrySet()) {
                        String key = ReportKey.RESULT_OF_BRANCH_PREFIX + entry.getKey();
                        report.put(key, String.valueOf(entry.getValue()));
                    }

                    // Return the Test Methods' Arguments
                    return new Object[]{testCase, result};
                });

        // Invoke Test Methods
        return tests.map(arguments -> {
                    invocationCount.incrementAndGet();
                    return createInvocationContext(formatter, methodContext, arguments, invocationCount.intValue());
                }
        ).onClose(() ->
                Preconditions.condition(
                        invocationCount.get() > 0,
                        "Config Error: At least one test case required for @PartiQLTest"
                )
        ).onClose(() ->
                // Publish Coverage Metrics
                extensionContext.publishReportEntry(report)
        );
    }

    @SuppressWarnings("ConstantConditions")
    private PartiQLTestProvider instantiateArgumentsProvider(Class<? extends PartiQLTestProvider> clazz) {
        try {
            return ReflectionUtils.newInstance(clazz);
        } catch (Exception ex) {
            if (ex instanceof NoSuchMethodException) {
                String message = String.format("Failed to find a no-argument constructor for ArgumentsProvider [%s]. "
                                + "Please ensure that a no-argument constructor exists and "
                                + "that the class is either a top-level class or a static nested class",
                        clazz.getName());
                throw new JUnitException(message, ex);
            }
            throw ex;
        }
    }
}
