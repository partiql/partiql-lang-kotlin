package org.partiql.runner

import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.runner.schema.TestCase
import org.partiql.runner.test.TestProvider
import org.partiql.runner.test.TestRunner
import org.partiql_v0_14_8.lang.eval.CompileOptions
import org.partiql_v0_14_8.lang.eval.TypingMode

abstract class ConformanceTestBase<T, V> {
    abstract val runner: TestRunner<T, V>
    abstract val skipListForEvaluation: Set<Pair<String, CompileOptions>>
    abstract val skipListForEquivalence: Set<Pair<String, CompileOptions>>

    companion object {
        val COERCE_EVAL_MODE_COMPILE_OPTIONS = CompileOptions.build { typingMode(TypingMode.PERMISSIVE) }
        val ERROR_EVAL_MODE_COMPILE_OPTIONS = CompileOptions.build { typingMode(TypingMode.LEGACY) }
    }

    // Tests the eval tests with the Kotlin implementation
    // Unit is second.
    // This is not a performance test. This is for stop long-running tests during development process in eval engine.
    // This number can be smaller, but to account for the cold start time and fluctuation of GitHub runner,
    // I decided to make this number a bit larger than needed.
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Eval::class)
    fun validatePartiQLEvalTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Eval -> runner.test(tc, skipListForEvaluation)
            else -> error("Unsupported test case category")
        }
    }

    // Tests the eval equivalence tests with the Kotlin implementation
    @Timeout(value = 500, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(TestProvider.Equiv::class)
    fun validatePartiQLEvalEquivTestData(tc: TestCase) {
        when (tc) {
            is TestCase.Equiv -> runner.test(tc, skipListForEquivalence)
            else -> error("Unsupported test case category")
        }
    }

    protected fun getSkipList(path: String): Set<Pair<String, CompileOptions>> {
        val reader = this::class.java.getResourceAsStream(path)?.bufferedReader() ?: error("Could not find skip list file.")
        val skipList = mutableSetOf<Pair<String, CompileOptions>>()
        reader.lines().forEach { line ->
            // Skip empty lines
            if (line.isEmpty()) {
                return@forEach
            }
            // Skip comments
            if (line.startsWith("//")) {
                return@forEach
            }
            val parts = line.split(":::")
            assert(parts.size == 2)
            val compileOptions = when (parts[0]) {
                "PERMISSIVE" -> COERCE_EVAL_MODE_COMPILE_OPTIONS
                "LEGACY" -> ERROR_EVAL_MODE_COMPILE_OPTIONS
                else -> throw IllegalArgumentException("Unknown typing mode: ${parts[0]}")
            }
            skipList.add(Pair(parts[1], compileOptions))
        }
        reader.close()
        return skipList
    }
}
