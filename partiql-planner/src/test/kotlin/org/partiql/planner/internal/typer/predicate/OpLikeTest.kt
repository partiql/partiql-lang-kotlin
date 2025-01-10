package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccess
import org.partiql.planner.util.allCharStringPType
import org.partiql.planner.util.allSupportedPType
import org.partiql.planner.util.cartesianProduct
import org.partiql.spi.types.PType
import java.util.stream.Stream

class OpLikeTest : PartiQLTyperTestBase() {
    @TestFactory
    fun likeDoubleArg(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-32", // t1 LIKE t2
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = (allCharStringPType)
                .let { cartesianProduct(it, it) }
            val failureArgs = cartesianProduct(
                allSupportedPType,
                allSupportedPType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<PType> ->
                accumulateSuccess(PType.bool(), args)
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGenPType("like", tests, argsMap)
    }

    @TestFactory
    fun likeTripleArg(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-33", // t1 LIKE t2 ESCAPE t3
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = (allCharStringPType)
                .let { cartesianProduct(it, it, it) }
            val failureArgs = cartesianProduct(
                allSupportedPType,
                allSupportedPType,
                allSupportedPType,
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<PType> ->
                accumulateSuccess(PType.bool(), args)
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGenPType("like", tests, argsMap)
    }
}
