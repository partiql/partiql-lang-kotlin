package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccess
import org.partiql.planner.util.allCharStringPType
import org.partiql.planner.util.allDatePType
import org.partiql.planner.util.allNumberPType
import org.partiql.planner.util.allSupportedPType
import org.partiql.planner.util.allTimePType
import org.partiql.planner.util.allTimeStampPType
import org.partiql.planner.util.cartesianProduct
import org.partiql.types.PType
import java.util.stream.Stream

// TODO: Finalize the semantics for Between operator when operands contain MISSING
//  For now, Between propagates MISSING.
class OpBetweenTest : PartiQLTyperTestBase() {

    @TestFactory
    fun between(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-34",
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs =
                cartesianProduct(
                    allNumberPType,
                    allNumberPType,
                    allNumberPType,
                ) + cartesianProduct(
                    allCharStringPType,
                    allCharStringPType,
                    allCharStringPType
                ) + cartesianProduct(
                    allDatePType,
                    allDatePType,
                    allDatePType,
                ) + cartesianProduct(
                    allTimePType,
                    allTimePType,
                    allTimePType,
                ) + cartesianProduct(
                    allTimeStampPType,
                    allTimeStampPType,
                    allTimeStampPType,
                )

            val failureArgs = cartesianProduct(
                allSupportedPType,
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

        return super.testGenPType("between", tests, argsMap)
    }
}
