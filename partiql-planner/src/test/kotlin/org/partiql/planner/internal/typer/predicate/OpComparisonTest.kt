package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccess
import org.partiql.planner.internal.typer.accumulateSuccessNullCall
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.StaticType
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
        val argsMap: Map<TestResult, Set<List<StaticType>>> = buildMap {
            val successArgs = cartesianProduct(allSupportedType, allSupportedType)
            successArgs.forEach { args: List<StaticType> ->
                when {
                    args.any { it is MissingType } && args.any { it is NullType } -> accumulateSuccess(StaticType.BOOL, args)
                    args.any { it is MissingType } && args.any { it is NullType } -> accumulateSuccess(StaticType.BOOL, args)
                }
            }
        }

        return super.testGen("eq", tests, argsMap)
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
                    StaticType.NUMERIC.allTypes + listOf(StaticType.NULL),
                    StaticType.NUMERIC.allTypes + listOf(StaticType.NULL)
                ) + cartesianProduct(
                    StaticType.TEXT.allTypes + listOf(StaticType.NULL),
                    StaticType.TEXT.allTypes + listOf(StaticType.NULL)
                ) + cartesianProduct(
                    listOf(StaticType.BOOL, StaticType.NULL),
                    listOf(StaticType.BOOL, StaticType.NULL)
                ) + cartesianProduct(
                    listOf(StaticType.DATE, StaticType.NULL),
                    listOf(StaticType.DATE, StaticType.NULL)
                ) + cartesianProduct(
                    listOf(StaticType.TIME, StaticType.NULL),
                    listOf(StaticType.TIME, StaticType.NULL)
                ) + cartesianProduct(
                    listOf(StaticType.TIMESTAMP, StaticType.NULL),
                    listOf(StaticType.TIMESTAMP, StaticType.NULL)
                )

            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType,
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<StaticType> ->
                accumulateSuccessNullCall(StaticType.BOOL, args)
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("comparison", tests, argsMap)
    }
}
