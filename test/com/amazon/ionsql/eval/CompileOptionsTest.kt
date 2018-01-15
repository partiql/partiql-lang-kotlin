package com.amazon.ionsql.eval

import org.junit.*
import org.junit.Assert.*

class CompileOptionsTest {
    private fun assertDefault(actual: CompileOptions) {
        assertEquals(UndefinedVariableBehavior.ERROR, actual.undefinedVariable)
    }

    @Test
    fun default() = assertDefault(CompileOptions.standard())

    @Test
    fun emptyKotlinBuilder() = assertDefault(CompileOptions.build {})

    @Test
    fun emptyJavaBuilder() = assertDefault(CompileOptions.builder().build())

    @Test
    fun changingUndefinedVariable() {
        val compileOptions = CompileOptions.builder().undefinedVariable(UndefinedVariableBehavior.MISSING).build()

        assertEquals(UndefinedVariableBehavior.MISSING, compileOptions.undefinedVariable)
    }
}
