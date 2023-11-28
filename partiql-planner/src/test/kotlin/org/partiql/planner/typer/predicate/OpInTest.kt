package org.partiql.planner.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allCollectionType
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.types.MissingType
import org.partiql.types.StaticType
import java.util.stream.Stream

class OpInTest : PartiQLTyperTestBase() {

    @TestFactory
    fun inSingleArg(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-30", // IN ( true )
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs =
                allSupportedType
                    .filterNot { it is MissingType }
                    .map { t -> listOf(t) }
                    .toSet()

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
            put(TestResult.Failure, emptySet<List<StaticType>>())
        }

        return super.testGen("in", tests, argsMap)
    }

    @TestFactory
    fun inDoubleArg(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-31", // t1 IN t2
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = cartesianProduct(
                allSupportedType.filterNot { it is MissingType },
                (allCollectionType + listOf(StaticType.NULL))
            )
            val failureArgs = cartesianProduct(
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

        return super.testGen("in", tests, argsMap)
    }
}
