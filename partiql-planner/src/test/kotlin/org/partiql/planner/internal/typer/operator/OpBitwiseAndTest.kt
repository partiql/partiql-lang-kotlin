package org.partiql.planner.typer.operator

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.typer.PartiQLTyperTestBase
import org.partiql.planner.util.CastType
import org.partiql.planner.util.allIntType
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.planner.util.castTable
import org.partiql.types.StaticType
import java.util.stream.Stream

class OpBitwiseAndTest : PartiQLTyperTestBase() {
    @TestFactory
    fun bitwiseAnd(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-36"
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
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
                if (args.contains(StaticType.MISSING)) {
                    (this[TestResult.Success(StaticType.MISSING)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.MISSING), it + setOf(args))
                    }
                } else if (args.contains(StaticType.NULL)) {
                    (this[TestResult.Success(StaticType.NULL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.NULL), it + setOf(args))
                    }
                } else if (arg0 == arg1) {
                    (this[TestResult.Success(arg1)] ?: setOf(args)).let {
                        put(TestResult.Success(arg1), it + setOf(args))
                    }
                } else if (castTable(arg1, arg0) == CastType.COERCION) {
                    (this[TestResult.Success(arg0)] ?: setOf(args)).let {
                        put(TestResult.Success(arg0), it + setOf(args))
                    }
                } else {
                    (this[TestResult.Success(arg1)] ?: setOf(args)).let {
                        put(TestResult.Success(arg1), it + setOf(args))
                    }
                }
                Unit
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("bitwise_and", tests, argsMap)
    }
}
