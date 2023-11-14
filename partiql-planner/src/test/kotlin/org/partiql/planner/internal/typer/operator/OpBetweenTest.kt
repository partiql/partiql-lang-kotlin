package org.partiql.planner.typer.operator

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.typer.PartiQLTyperTestBase
import org.partiql.planner.util.CastType
import org.partiql.planner.util.allNumberType
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.cartesianProduct
import org.partiql.planner.util.castTable
import org.partiql.types.StaticType
import java.util.stream.Stream

// TODO: Between Semantic is messy on because of missing
//  We know that t1 Between t2 AND t3 === t1>=t2 AND t1<=t3
//  assuming t1, t2, t3 are all Single Type
//  t1 >= t2 can be Boolean or Missing or Null
//  t1 <= t3 can be Boolean or Missing or Null
//  t1 >= t2 AND T1 <= t3 =>
//    Boolean AND Boolean => Boolean
//    Boolean AND MISSING => UnionOf(NULL, BOOLEAN)
//    Boolean AND NULL => UnionOf(NULL, BOOLEAN)
//    NULL AND NULL => NULL
//    MISSING AND MISSING => NULL
class OpBetweenTest : PartiQLTyperTestBase() {
    @Disabled
    @TestFactory
    fun between(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-34",
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs =
                cartesianProduct(
                    allNumberType + listOf(StaticType.NULL, StaticType.MISSING),
                    allNumberType + listOf(StaticType.NULL, StaticType.MISSING),
                    allNumberType + listOf(StaticType.NULL, StaticType.MISSING),
                )

            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType,
                allSupportedType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<StaticType> ->
                val arg0 = args.first()
                val arg1 = args[1]
                val arg2 = args[2]
                if (args.contains(StaticType.MISSING)) {
                    (this[TestResult.Success(StaticType.MISSING)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.MISSING), it + setOf(args))
                    }
                } else if (args.contains(StaticType.NULL)) {
                    (this[TestResult.Success(StaticType.NULL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.NULL), it + setOf(args))
                    }
                } else if (arg0 == arg1 && arg0 == arg2) {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                } else if (castTable(arg1, arg0) == CastType.COERCION && castTable(arg2, arg0) == CastType.COERCION) {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                } else if (castTable(arg0, arg1) == CastType.COERCION && castTable(arg2, arg1) == CastType.COERCION) {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                } else {
                    (this[TestResult.Success(StaticType.BOOL)] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.BOOL), it + setOf(args))
                    }
                }
                Unit
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("between", tests, argsMap)
    }
}
