package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
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
        val argsMap = buildMap {
            val successArgs = cartesianProduct(allSupportedType, allSupportedType)

            successArgs.forEach { args: List<StaticType> ->
                (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                    put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                }
                put(TestResult.Failure, emptySet<List<StaticType>>())
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

        val argsMap = buildMap {
            val successArgs =
                cartesianProduct(
                    StaticType.NUMERIC.allTypes,
                    StaticType.NUMERIC.allTypes
                ) + cartesianProduct(
                    StaticType.TEXT.allTypes + listOf(StaticType.CLOB),
                    StaticType.TEXT.allTypes + listOf(StaticType.CLOB)
                ) + cartesianProduct(
                    listOf(StaticType.BOOL),
                    listOf(StaticType.BOOL)
                ) + cartesianProduct(
                    listOf(StaticType.DATE),
                    listOf(StaticType.DATE)
                ) + cartesianProduct(
                    listOf(StaticType.TIME),
                    listOf(StaticType.TIME)
                ) + cartesianProduct(
                    listOf(StaticType.TIMESTAMP),
                    listOf(StaticType.TIMESTAMP)
                )

            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType,
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<StaticType> ->
                (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                    put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                }
                Unit
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("comparison", tests, argsMap)
    }
}
