package org.partiql.planner.internal.typer.predicate

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccessNullCall
import org.partiql.planner.util.allNumberType
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.types.StaticType
import java.util.stream.Stream

// TODO: Finalize the semantics for Between operator when operands contain MISSING
//  For now, Between propagates MISSING.
class OpBetweenTest : PartiQLTyperTestBase() {
    @TestFactory
    fun between(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-34",
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs =
                cartesianProduct(
                    allNumberType + listOf(StaticType.NULL),
                    allNumberType + listOf(StaticType.NULL),
                    allNumberType + listOf(StaticType.NULL),
                ) + cartesianProduct(
                    StaticType.TEXT.allTypes + listOf(StaticType.CLOB, StaticType.NULL),
                    StaticType.TEXT.allTypes + listOf(StaticType.CLOB, StaticType.NULL),
                    StaticType.TEXT.allTypes + listOf(StaticType.CLOB, StaticType.NULL)
                ) + cartesianProduct(
                    listOf(StaticType.DATE, StaticType.NULL),
                    listOf(StaticType.DATE, StaticType.NULL),
                    listOf(StaticType.DATE, StaticType.NULL)
                ) + cartesianProduct(
                    listOf(StaticType.TIME, StaticType.NULL),
                    listOf(StaticType.TIME, StaticType.NULL),
                    listOf(StaticType.TIME, StaticType.NULL)
                ) + cartesianProduct(
                    listOf(StaticType.TIMESTAMP, StaticType.NULL),
                    listOf(StaticType.TIMESTAMP, StaticType.NULL),
                    listOf(StaticType.TIMESTAMP, StaticType.NULL)
                )

            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType,
                allSupportedType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<StaticType> ->
                accumulateSuccessNullCall(StaticType.BOOL, args)
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("between", tests, argsMap)
    }
}
