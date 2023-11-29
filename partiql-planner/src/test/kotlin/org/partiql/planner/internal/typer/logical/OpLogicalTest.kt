package org.partiql.planner.internal.typer.logical

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.isUnknown
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
        val supportedType = listOf<StaticType>(
            StaticType.BOOL,
            StaticType.NULL,
            StaticType.MISSING,
        )

        val unsupportedType = allSupportedType.filterNot {
            supportedType.contains(it)
        }

        val tests = listOf(
            "expr-02", // Not
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = supportedType.map { t -> listOf(t) }.toSet()
            successArgs.forEach { args: List<StaticType> ->
                val arg = args.first()
                if (arg.isUnknown()) {
                    (this[TestResult.Success(StaticType.NULL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.NULL), it + setOf(args))
                    }
                } else {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                }
                Unit
            }

            put(TestResult.Failure, unsupportedType.map { t -> listOf(t) }.toSet())
        }

        return super.testGen("not", tests, argsMap)
    }

    // TODO: There is no good way to have the inferencer to distinguish whether the logical operator returns
    //  NULL, OR BOOL, OR UnionOf(Bool, NULL), other than have a lookup table in the inferencer.
    @TestFactory
    fun booleanConnective(): Stream<DynamicContainer> {
        val supportedType = listOf<StaticType>(
            StaticType.BOOL,
            StaticType.NULL,
            StaticType.MISSING
        )

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

            put(TestResult.Success(StaticType.unionOf(StaticType.BOOL, StaticType.NULL)), successArgs)
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("booleanConnective", tests, argsMap)
    }
}
