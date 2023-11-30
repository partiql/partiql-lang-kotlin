package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.CastType
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.planner.util.castTable
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
                if (args.contains(StaticType.MISSING)) {
                    (this[TestResult.Success(StaticType.NULL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.NULL), it + setOf(args))
                    }
                } else if (args.contains(StaticType.NULL)) {
                    (this[TestResult.Success(StaticType.NULL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.NULL), it + setOf(args))
                    }
                } else {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
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
                    StaticType.NUMERIC.allTypes + listOf(StaticType.NULL),
                    StaticType.NUMERIC.allTypes + listOf(StaticType.NULL)
                )
            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType,
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
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                } else if (castTable(arg1, arg0) == CastType.COERCION) {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                } else {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                }
                Unit
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("comparison", tests, argsMap)
    }
}
