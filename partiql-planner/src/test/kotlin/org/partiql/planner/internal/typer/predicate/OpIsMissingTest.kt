package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allSupportedPType
import org.partiql.types.PType
import java.util.stream.Stream

class OpIsMissingTest : PartiQLTyperTestBase() {
    @TestFactory
    fun isMissing(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-11" // IS MISSING
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = allSupportedPType.flatMap { t ->
                setOf(listOf(t))
            }.toSet()

            put(TestResult.Success(PType.bool()), successArgs)
            put(TestResult.Failure, emptySet<List<PType>>())
        }

        return super.testGenPType("isMissing", tests, argsMap)
    }
}
