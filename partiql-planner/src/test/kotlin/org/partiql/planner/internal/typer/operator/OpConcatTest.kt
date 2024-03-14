package org.partiql.planner.internal.typer.operator

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.accumulateSuccessNullCall
import org.partiql.planner.util.CastType
import org.partiql.planner.util.allSupportedType
import org.partiql.planner.util.allTextType
import org.partiql.planner.util.cartesianProduct
import org.partiql.planner.util.castTable
import org.partiql.types.NullType
import org.partiql.types.StaticType
import org.partiql.types.SymbolType
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
                val output = when {
                    arg0 is NullType && arg1 is NullType -> StaticType.STRING
                    arg0 == arg1 -> arg1
                    // This specifically needs to be added because STRING is higher on the precedence list. Therefore,
                    // since the NULL type is distinct from the value NULL, there is no exact match for (SYMBOL, NULL)
                    // and (NULL, SYMBOL). The implication is that we find the "best" match, in which case, this would
                    // be the (STRING, STRING) -> STRING function, since it is highest in precedence. Note that, this
                    // would be different if the input were (SYMBOL, SYMBOL?), but we don't support testing of this nature
                    // due to the limitations of StaticType.
                    args.any { it is SymbolType } && args.any { it is NullType } -> StaticType.STRING
                    castTable(arg1, arg0) == CastType.COERCION -> arg0
                    castTable(arg0, arg1) == CastType.COERCION -> arg1
                    else -> error("Arguments do not conform to parameters. Args: $args")
                }
                accumulateSuccessNullCall(output, args)
            }
            put(TestResult.Failure, failureArgs)
        }

        return super.testGen("concat", tests, argsMap)
    }
}
