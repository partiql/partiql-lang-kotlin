package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allSupportedType
import org.partiql.types.StaticType
import java.util.stream.Stream

class OpIsMissingTest : PartiQLTyperTestBase() {
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
