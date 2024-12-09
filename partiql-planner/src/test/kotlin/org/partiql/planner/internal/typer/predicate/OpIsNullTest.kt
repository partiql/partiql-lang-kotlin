package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allSupportedPType
import org.partiql.types.PType
import java.util.stream.Stream

// TODO: Finalize the semantics for IS NULL operator when operand is MISSING.
//  For now, the IS NULL function can take missing as a operand, and returns TRUE.
class OpIsNullTest : PartiQLTyperTestBase() {
    @TestFactory
    fun isNull(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-10", // IS NULL
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = allSupportedPType.flatMap { t ->
                setOf(listOf(t))
            }.toSet()

            put(TestResult.Success(PType.bool()), successArgs)
            put(TestResult.Failure, emptySet<List<PType>>())
        }

        return super.testGenPType("isNull", tests, argsMap)
    }
}
