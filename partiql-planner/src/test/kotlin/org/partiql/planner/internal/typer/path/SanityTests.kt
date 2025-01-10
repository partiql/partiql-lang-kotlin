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

        val start = 0
        val end = 14

        val tests = buildList {
            (start..end).forEach {
                this.add("paths-${it.toString().padStart(2, '0')}")
            }
        }.map { inputs.get("basics", it)!! }
        // -- t1 -> ANY
        // -- t2 -> ANY
        val argTypes = listOf(StaticType.ANY, StaticType.ANY)
        // -- All paths return ANY because t1 and t2 are both ANY
        val argsMap: Map<TestResult, Set<List<StaticType>>> = buildMap {
            put(TestResult.Success(StaticType.ANY), setOf(argTypes))
            put(TestResult.Failure, emptySet())
        }
        return super.testGen("path", tests, argsMap)
    }
}
