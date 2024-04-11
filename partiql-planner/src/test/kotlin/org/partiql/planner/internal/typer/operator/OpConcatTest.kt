package org.partiql.planner.internal.typer.operator

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.util.CastType
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.allTextType
import org.partiql.planner.util.cartesianProduct
import org.partiql.planner.util.castTable
import org.partiql.types.NullType
import org.partiql.types.StaticType
import java.util.stream.Stream

class OpConcatTest : PartiQLTyperTestBase() {
    @TestFactory
    fun concat(): Stream<DynamicContainer> {
        val tests = listOf(
            "expr-35"
        ).map { inputs.get("basics", it)!! }

        val argsMap = buildMap {
            val successArgs = (allTextType + listOf(StaticType.NULL))
                .let { cartesianProduct(it, it) }
            val failureArgs = cartesianProduct(
                allSupportedType,
                allSupportedType
            ).filterNot {
                successArgs.contains(it)
            }.toSet()

            successArgs.forEach { args: List<StaticType> ->
                val arg0 = args.first()
                val arg1 = args[1]
                // TODO: Since nullable types are modeled as unions of types (where each has their own applicable casts),
                //  concatenating SYMBOL and NULL will resolve to CONCAT(CAST(<sym> AS STRING), CAST(NULL AS STRING))
                //  due to type precedence rules.
                if (args.contains(StaticType.NULL) && args.contains(StaticType.SYMBOL)) {
                    return@forEach
                }
                if (args.all { it is NullType }) {
                    val nullableHighestPrecedenceType = StaticType.unionOf(StaticType.STRING, StaticType.NULL)
                    (this[TestResult.Success(nullableHighestPrecedenceType)] ?: setOf(args)).let {
                        put(TestResult.Success(nullableHighestPrecedenceType), it + setOf(args))
                    }
                } else if (args.contains(StaticType.NULL)) {
                    (this[TestResult.Success(StaticType.unionOf(args.toSet()))] ?: setOf(args)).let {
                        put(TestResult.Success(StaticType.unionOf(args.toSet())), it + setOf(args))
                    }
                } else if (arg0 == arg1) {
                    (this[TestResult.Success(arg1)] ?: setOf(args)).let {
                        put(TestResult.Success(arg1), it + setOf(args))
                    }
                } else if (castTable(arg1, arg0) == CastType.COERCION) {
                    (this[TestResult.Success(arg0)] ?: setOf(args)).let {
                        put(TestResult.Success(arg0), it + setOf(args))
                    }
                } else {
                    (this[TestResult.Success(arg1)] ?: setOf(args)).let {
                        put(TestResult.Success(arg1), it + setOf(args))
                    }
                }
                Unit
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("concat", tests, argsMap)
    }
}
