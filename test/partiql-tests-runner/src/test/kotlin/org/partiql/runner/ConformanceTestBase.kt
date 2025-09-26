package org.partiql.runner

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.TestInstance
import org.partiql.runner.test.TestRunner

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Annotation is need avoid allTests and skiplist recreated for each test.
abstract class ConformanceTestBase<T, V> {
    abstract val runner: TestRunner<T, V>
    private var allTests = emptySet<String>() // for duplicate detection

    companion object {
        val COERCE_EVAL_MODE_COMPILE_OPTIONS = CompileType.PERMISSIVE
        val ERROR_EVAL_MODE_COMPILE_OPTIONS = CompileType.STRICT
    }

    @BeforeEach
    fun afterEach(testInfo: TestInfo) {
        if (allTests.contains(testInfo.displayName)) {
            // Fail the test if duplicate name is detected.
            // Note: TestInfo.displayName is truncated if it is too long in the callback. However, there is no official document mentioned how it works
            throw IllegalStateException("DUPLICATE TESTS DETECTED, PLEASE RENAME: ${testInfo.displayName}")
        } else {
            allTests += testInfo.displayName
        }
    }

    protected fun getSkipList(path: String): Set<Pair<String, CompileType>> {
        val reader = this::class.java.getResourceAsStream(path)?.bufferedReader() ?: error("Could not find skip list file.")
        val skipList = mutableSetOf<Pair<String, CompileType>>()
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
                "STRICT" -> ERROR_EVAL_MODE_COMPILE_OPTIONS
                else -> throw IllegalArgumentException("Unknown typing mode: ${parts[0]}")
            }
            skipList.add(Pair(parts[1], compileOptions))
        }
        reader.close()
        return skipList
    }
}
