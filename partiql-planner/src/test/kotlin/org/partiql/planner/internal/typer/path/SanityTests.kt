package org.partiql.planner.internal.typer.path

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.types.StaticType
import java.util.stream.Stream

/**
 * This test makes sure that the planner can resolve various path expression
 */
class SanityTests : PartiQLTyperTestBase() {
    @TestFactory
    fun path(): Stream<DynamicContainer> {
        val tests = buildList {
            (0..14).forEach {
                this.add("paths-${it.toString().padStart(2,'0')}")
            }
        }.map { inputs.get("basics", it)!! }

        val argsMap: Map<TestResult, Set<List<StaticType>>> = buildMap {
            put(TestResult.Success(StaticType.ANY), setOf(listOf(StaticType.ANY, StaticType.ANY)))
            put(TestResult.Failure, emptySet<List<StaticType>>())
        }

        return super.testGen("path", tests, argsMap)
    }
}
