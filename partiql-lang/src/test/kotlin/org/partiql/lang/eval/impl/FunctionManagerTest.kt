package org.partiql.lang.eval.impl

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.eval.ArityMismatchException
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.FunctionNotFoundException
import org.partiql.lang.eval.builtins.SCALAR_BUILTINS_DEFAULT
import org.partiql.types.StaticType

class FunctionManagerTest : EvaluatorTestBase() {

    val functions = SCALAR_BUILTINS_DEFAULT
    private val functionManager = FunctionManager(functions)

    @Test
    fun functionManagerPassTests() {
        val function_1 = functionManager.get("trim", 1, listOf(StaticType.TEXT))
        val function_2 = functionManager.get("trim", 2, listOf(StaticType.TEXT, StaticType.STRING))
        val function_3 = functionManager.get("trim", 3, listOf(StaticType.TEXT, StaticType.STRING, StaticType.STRING))

        assertEquals(function_1.signature.name, "trim")
        assertEquals(function_1.signature.arity, 1..1)
        assertEquals(function_1.signature.requiredParameters, listOf(StaticType.TEXT))
        assertEquals(function_2.signature.name, "trim")
        assertEquals(function_2.signature.arity, 2..2)
        assertEquals(function_2.signature.requiredParameters, listOf(StaticType.TEXT, StaticType.STRING))
        assertEquals(function_3.signature.name, "trim")
        assertEquals(function_3.signature.arity, 3..3)
        assertEquals(function_3.signature.requiredParameters, listOf(StaticType.TEXT, StaticType.STRING, StaticType.STRING))
    }

    @Test
    fun functionManagerInvalidFunctionNameTest() {
        assertThrows<FunctionNotFoundException> { functionManager.get("trim_", 1, listOf(StaticType.TEXT)) }
    }

    @Test
    fun functionManagerInvalidArityTest() {
        assertThrows<ArityMismatchException> { functionManager.get("trim", 0, listOf(StaticType.INT)) }
    }

    @Test
    fun functionManagerInvalidArgTypeTest() {
        assertThrows<EvaluationException> { functionManager.get("trim", 1, listOf(StaticType.INT)) }
    }
}
