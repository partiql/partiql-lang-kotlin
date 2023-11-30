package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allSupportedType
import org.partiql.types.StaticType
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
            val successArgs = allSupportedType.flatMap { t ->
                setOf(listOf(t))
            }.toSet()

            put(TestResult.Success(StaticType.BOOL), successArgs)
            put(TestResult.Failure, emptySet<List<StaticType>>())
        }

        return super.testGen("isNull", tests, argsMap)
    }
}
