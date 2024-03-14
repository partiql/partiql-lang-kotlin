package org.partiql.planner.internal.typer.operator

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccess
import org.partiql.planner.util.CastType
import org.partiql.planner.util.allIntType
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.planner.util.castTable
import org.partiql.types.NullType
import org.partiql.types.StaticType
import java.util.stream.Stream

class OpBitwiseAndTest : PartiQLTyperTestBase() {
    @TestFactory
    fun bitwiseAnd(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-36"
        ).map { inputs.get("basics", it)!! }

        val argsMap: Map<TestResult, Set<List<StaticType>>> = buildMap {
            val successArgs = (allIntType + listOf(StaticType.NULL))
                .let { cartesianProduct(it, it) }
            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<StaticType> ->
                val arg0 = args.first()
                val arg1 = args[1]
                val output = when {
                    arg0 is NullType && arg1 is NullType -> StaticType.INT2
                    arg0 == arg1 -> arg1
                    castTable(arg1, arg0) == CastType.COERCION -> arg0
                    castTable(arg0, arg1) == CastType.COERCION -> arg1
                    else -> error("Arguments do not conform to parameters. Args: $args")
                }
                accumulateSuccess(output, args)
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("bitwise_and", tests, argsMap)
    }
}
