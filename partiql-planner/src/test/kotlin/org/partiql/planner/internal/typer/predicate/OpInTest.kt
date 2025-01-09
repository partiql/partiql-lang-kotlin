package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccess
import org.partiql.planner.util.allCollectionPType
import org.partiql.planner.util.allSupportedPType
import org.partiql.planner.util.cartesianProduct
import org.partiql.spi.types.PType
import java.util.stream.Stream

class OpInTest : PartiQLTyperTestBase() {

    @TestFactory
    fun inSingleArg(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-30", // IN ( true )
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs =
                allSupportedPType
                    .map { t -> listOf(t) }
                    .toSet()

            successArgs.forEach { args: List<PType> ->
                accumulateSuccess(PType.bool(), args)
            }
            put(TestResult.Failure, emptySet<List<PType>>())
        }

        return super.testGenPType("in", tests, argsMap)
    }

    @TestFactory
    fun inDoubleArg(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-31", // t1 IN t2
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = cartesianProduct(
                allSupportedPType,
                allCollectionPType
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

        return super.testGenPType("in", tests, argsMap)
    }
}
