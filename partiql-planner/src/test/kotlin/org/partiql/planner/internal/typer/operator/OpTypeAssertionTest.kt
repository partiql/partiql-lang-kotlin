package org.partiql.planner.typer.operator

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.typer.PartiQLTyperTestBase
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
            val successArgs = allSupportedType.flatMap { t ->
                setOf(listOf(t))
            }.toSet()
            successArgs.forEach { args: List<StaticType> ->
                if (args.all { it is MissingType }) {
                    (this[TestResult.Success(StaticType.MISSING)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.MISSING), it + setOf(args))
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

        return super.testGen("type-assertion", tests, argsMap)
    }
}
