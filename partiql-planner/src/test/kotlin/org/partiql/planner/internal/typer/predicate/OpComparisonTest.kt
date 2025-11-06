package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccess
import org.partiql.planner.util.allBooleanPType
import org.partiql.planner.util.allCharStringPType
import org.partiql.planner.util.allDatePType
import org.partiql.planner.util.allNumberPType
import org.partiql.planner.util.allSupportedPType
import org.partiql.planner.util.allTimePType
import org.partiql.planner.util.allTimeStampPType
import org.partiql.planner.util.cartesianProduct
import org.partiql.spi.types.PType
import java.util.stream.Stream

// TODO : Behavior when Missing is one operand needs to be finalized
//  For now, equal function does not propagates MISSING.
class OpComparisonTest : PartiQLTyperTestBase() {
    @TestFactory
    fun eq(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-07", // Equal
            "expr-08", // Not Equal !=
            "expr-09", // Not Equal <>
        ).map { inputs.get("basics", it)!! }
        val argsMap: Map<TestResult, Set<List<PType>>> = buildMap {
            val successArgs = cartesianProduct(allSupportedPType, allSupportedPType)
            successArgs.forEach { args: List<PType> ->
                accumulateSuccess(PType.bool(), args)
            }
        }

        return super.testGenPType("eq", tests, argsMap)
    }

    @TestFactory
    fun comparison(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-03", // Less than TODO: Less than currently only support numeric type
            "expr-04", // Less than or equal TODO: Less than or equal currently only support numeric type
            "expr-05", // Bigger than TODO: Bigger than currently only support numeric type
            "expr-06", // Bigger than or equal TODO: Bigger than or equal currently only support numeric type

        ).map { inputs.get("basics", it)!! }

        /**
         * 8.2 Comparison Predicate — SQL-99 page 287
         */
        val argsMap = buildMap {
            val successArgs =
                cartesianProduct(
                    allNumberPType,
                    allNumberPType,
                ) + cartesianProduct(
                    allCharStringPType,
                    allCharStringPType,
                ) + cartesianProduct(
                    allBooleanPType,
                    allBooleanPType,
                ) + cartesianProduct(
                    allDatePType,
                    allDatePType,
                ) + cartesianProduct(
                    allTimePType,
                    allTimePType,
                ) + cartesianProduct(
                    allTimeStampPType,
                    allTimeStampPType,
                ) + cartesianProduct(
                    allDatePType,
                    allTimeStampPType,
                ) + cartesianProduct(
                    allTimeStampPType,
                    allDatePType,
                )

            val failureArgs = cartesianProduct(
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

        return super.testGenPType("comparison", tests, argsMap)
    }
}
