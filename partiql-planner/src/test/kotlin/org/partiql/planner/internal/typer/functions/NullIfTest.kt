package org.partiql.planner.internal.typer.functions

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.types.StaticType
import java.util.stream.Stream

// TODO: Model handling of Truth Value in typer better.
class NullIfTest : PartiQLTyperTestBase() {

    @TestFactory
    fun nullIf(): Stream<DynamicContainer> {
        val tests = listOf(
            "func-00",
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = cartesianProduct(allSupportedType, allSupportedType)

            successArgs.forEach { args: List<StaticType> ->
                val returnType = StaticType.unionOf(args.first(), StaticType.NULL).flatten()
                (this[TestResult.Success(returnType)] ?: setOf(args)).let {
                    put(TestResult.Success(returnType), it + setOf(args))
                }
                Unit
            }
            put(TestResult.Failure, emptySet<List<StaticType>>())
        }

        return super.testGen("nullIf", tests, argsMap)
    }
}
