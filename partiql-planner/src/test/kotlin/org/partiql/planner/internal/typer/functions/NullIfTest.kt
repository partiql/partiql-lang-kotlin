package org.partiql.planner.internal.typer.functions

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.types.StaticType
import java.util.stream.Stream

/**
 * The NULLIF() function returns NULL if two expressions are equal, otherwise it returns the first expression
 *
 * The type of NULLIF(arg_0: T_0, arg_1: arg_1) should be (null|T_0).
 *
 * CASE
 *   WHEN x = y THEN NULL
 *   ELSE x
 * END
 *
 * TODO: Model handling of Truth Value in typer better.
 */
class NullIfTest : PartiQLTyperTestBase() {

    @TestFactory
    fun nullIf(): Stream<DynamicContainer> {

        val tests = listOf("func-00").map { inputs.get("basics", it)!! }
        val argsMap = mutableMapOf<TestResult, Set<List<StaticType>>>()

        // Generate all success cases
        cartesianProduct(allSupportedType, allSupportedType).forEach { args ->
            val expected = StaticType.unionOf(args[0], StaticType.NULL).flatten()
            val result = TestResult.Success(expected)
            argsMap[result] = setOf(args)
        }
        // No failure case
        argsMap[TestResult.Failure] = emptySet()

        return super.testGen("nullIf", tests, argsMap)
    }
}
