package org.partiql.planner.internal.typer.logical

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.types.StaticType
import java.util.stream.Stream

// TODO: Finalize the semantics for logical operators when operand(s) contain MISSING
//  For now Logical Operator (NOT, AND, OR) can take missing as operand(s)
//  and never returns MISSING
class OpLogicalTest : PartiQLTyperTestBase() {
    @TestFactory
    fun not(): Stream<DynamicContainer> {
        val supportedType = listOf<StaticType>(StaticType.BOOL)

        val unsupportedType = allSupportedType.filterNot {
            supportedType.contains(it)
        }

        val tests = listOf(
            "expr-02", // Not
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = supportedType.map { t -> listOf(t) }.toSet()
            successArgs.forEach { args: List<StaticType> ->
                (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                    put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                }
                Unit
            }

            put(TestResult.Failure, unsupportedType.map { t -> listOf(t) }.toSet())
        }

        return super.testGen("not", tests, argsMap)
    }

    @TestFactory
    fun booleanConnective(): Stream<DynamicContainer> {
        val supportedType = listOf<StaticType>(StaticType.BOOL)

        val tests = listOf(
            "expr-00", // OR
            "expr-01", // AND
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = cartesianProduct(supportedType, supportedType)
            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            put(TestResult.Success(StaticType.BOOL), successArgs)
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("booleanConnective", tests, argsMap)
    }
}
