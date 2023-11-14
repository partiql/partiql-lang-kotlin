package org.partiql.planner.typer.operator

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.typer.PartiQLTyperTestBase
import org.partiql.planner.typer.isUnknown
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.types.BoolType
import org.partiql.types.StaticType
import java.util.stream.Stream

class OpLogicalTest : PartiQLTyperTestBase() {
    @TestFactory
    fun not(): Stream<DynamicContainer> {
        val supportedType = listOf<StaticType>(
            StaticType.BOOL,
            StaticType.NULL,
            StaticType.MISSING,
        )

        val unsupportedType = allSupportedType.filterNot {
            supportedType.contains(it)
        }

        val tests = listOf(
            "expr-02", // Not
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = supportedType.map { t -> listOf(t) }.toSet()
            successArgs.forEach { args: List<StaticType> ->
                val arg = args.first()
                if (arg.isUnknown()) {
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

            put(TestResult.Failure, unsupportedType.map { t -> listOf(t) }.toSet())
        }

        return super.testGen("not", tests, argsMap)
    }

    @TestFactory
    fun booleanConnective(): Stream<DynamicContainer> {
        val supportedType = listOf<StaticType>(
            StaticType.BOOL,
            StaticType.NULL,
            StaticType.MISSING
        )

        val tests = listOf(
            "expr-00", // OR
            "expr-01", // AND
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = cartesianProduct(supportedType, supportedType)
            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<StaticType> ->
                if (args.all { it is BoolType }) {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                } else if (args.all { it.isUnknown() }) {
                    (this[TestResult.Success(StaticType.NULL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.NULL), it + setOf(args))
                    }
                } else {
                    (this[TestResult.Success(StaticType.unionOf(StaticType.BOOL, StaticType.NULL))] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.unionOf(StaticType.BOOL, StaticType.NULL)), it + setOf(args))
                    }
                }
                Unit
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("booleanConnective", tests, argsMap)
    }

    @TestFactory
    fun isNull(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-10", // IS NULL
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = allSupportedType.flatMap { t ->
                setOf(listOf(t))
            }.toSet()

            put(TestResult.Success(StaticType.BOOL), successArgs)
            put(TestResult.Failure, emptySet<List<StaticType>>())
        }

        return super.testGen("isNull", tests, argsMap)
    }

    @TestFactory
    fun isMissing(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-11" // IS MISSING
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = allSupportedType.flatMap { t ->
                setOf(listOf(t))
            }.toSet()

            put(TestResult.Success(StaticType.BOOL), successArgs)
            put(TestResult.Failure, emptySet<List<StaticType>>())
        }

        return super.testGen("isMissing", tests, argsMap)
    }
}
