package org.partiql.planner.typer.operator

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.typer.PartiQLTyperTestBase
import org.partiql.planner.util.CastType
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.allTextType
import org.partiql.planner.util.cartesianProduct
import org.partiql.planner.util.castTable
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
                val arg0 = args.first()
                val arg1 = args[1]
                if (args.contains(StaticType.NULL)) {
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

        return super.testGen("like", tests, argsMap)
    }

    @TestFactory
    fun likeTripleArg(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-33", // t1 NOT LIKE t2 ESCAPE t3
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
                val arg0 = args.first()
                val arg1 = args[1]
                val arg2 = args[2]
                if (args.contains(StaticType.NULL)) {
                    (this[TestResult.Success(StaticType.NULL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.NULL), it + setOf(args))
                    }
                } else if (arg0 == arg1 && arg0 == arg2) {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                } else if (castTable(arg1, arg0) == CastType.COERCION && castTable(arg2, arg0) == CastType.COERCION) {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                } else if (castTable(arg0, arg1) == CastType.COERCION && castTable(arg2, arg1) == CastType.COERCION) {
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

        return super.testGen("like", tests, argsMap)
    }
}
