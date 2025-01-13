package org.partiql.planner.internal.typer.operator

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccess
import org.partiql.planner.util.CastType
import org.partiql.planner.util.allIntPType
import org.partiql.planner.util.allNumberPType
import org.partiql.planner.util.allSupportedPType
import org.partiql.planner.util.cartesianProduct
import org.partiql.planner.util.castTablePType
import org.partiql.spi.types.PType
import java.util.stream.Stream

class OpBitwiseAndTest : PartiQLTyperTestBase() {
    @TestFactory
    fun bitwiseAnd(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-36"
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = allNumberPType.let { cartesianProduct(it, it) }
            val failureArgs = cartesianProduct(
                allSupportedPType,
                allSupportedPType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<PType> ->
                val arg0 = args.first()
                val arg1 = args[1]
                val output = when {
                    arg0 !in allIntPType && arg1 !in allIntPType -> PType.bigint()
                    arg0 in allIntPType && arg1 !in allIntPType -> PType.bigint()
                    arg0 !in allIntPType && arg1 in allIntPType -> PType.bigint()
                    arg0 == arg1 -> arg1
                    castTablePType(arg1, arg0) == CastType.COERCION -> arg0
                    castTablePType(arg0, arg1) == CastType.COERCION -> arg1
                    else -> error("Arguments do not conform to parameters. Args: $args")
                }
                accumulateSuccess(output, args)
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGenPType("bitwise_and", tests, argsMap)
    }
}
