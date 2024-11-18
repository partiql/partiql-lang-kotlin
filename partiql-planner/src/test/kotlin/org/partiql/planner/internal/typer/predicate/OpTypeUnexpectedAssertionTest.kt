package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccesses
import org.partiql.planner.util.allSupportedPType
import org.partiql.types.PType
import java.util.stream.Stream

class OpTypeUnexpectedAssertionTest : PartiQLTyperTestBase() {
    @TestFactory
    fun typeAssertion(): Stream<DynamicContainer> {
        val tests = buildList {
            (12..27).forEach {
                this.add("expr-$it")
            }
        }.map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = allSupportedPType.flatMap { t ->
                setOf(listOf(t))
            }.toSet()
            accumulateSuccesses(PType.bool(), successArgs)
            put(TestResult.Failure, emptySet<List<PType>>())
        }

        return super.testGenPType("type-assertion", tests, argsMap)
    }
}
