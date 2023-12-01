package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allSupportedType
import org.partiql.types.MissingType
import org.partiql.types.StaticType
import java.util.stream.Stream

class OpTypeAssertionTest : PartiQLTyperTestBase() {
    @TestFactory
    fun typeAssertion(): Stream<DynamicContainer> {
        val tests = buildList {
            (12..29).forEach {
                this.add("expr-$it")
            }
        }.map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = allSupportedType.filterNot { it is MissingType }.flatMap { t ->
                setOf(listOf(t))
            }.toSet()
            val failureArgs = setOf(listOf(MissingType))
            put(TestResult.Success(StaticType.BOOL), successArgs)
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("type-assertion", tests, argsMap)
    }
}
