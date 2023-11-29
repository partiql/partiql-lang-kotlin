package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.allTextType
import org.partiql.planner.util.cartesianProduct
import org.partiql.types.StaticType
import java.util.stream.Stream

class OpLikeTest : PartiQLTyperTestBase() {
    @TestFactory
    fun likeDoubleArg(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-32", // t1 LIKE t2
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = (allTextType + listOf(StaticType.NULL))
                .let { cartesianProduct(it, it) }
            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<StaticType> ->
                if (args.contains(StaticType.NULL)) {
                    (this[TestResult.Success(StaticType.NULL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.NULL), it + setOf(args))
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

        return super.testGen("like", tests, argsMap)
    }

    @TestFactory
    fun likeTripleArg(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-33", // t1 LIKE t2 ESCAPE t3
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = (allTextType + listOf(StaticType.NULL))
                .let { cartesianProduct(it, it, it) }
            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType,
                allSupportedType,
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<StaticType> ->
                if (args.contains(StaticType.NULL)) {
                    (this[TestResult.Success(StaticType.NULL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.NULL), it + setOf(args))
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

        return super.testGen("like", tests, argsMap)
    }
}
