package examples

import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.spi.types.StaticType
import kotlin.test.Test

class BreakingChanges {
    @Test
    fun `api change - replace Environment with EvaluationSession for ExprFunctions`() {
        class SomeExprFunction() : ExprFunction {
            override val signature = FunctionSignature(
                name = "some_expr_function",
                requiredParameters = listOf(StaticType.ANY),
                optionalParameter = StaticType.ANY,
                returnType = StaticType.ANY
            )

            // In v0.5.0, the `callWith*` functions' first argument was `Environment`
            override fun callWithRequired(env: Environment, required: List<ExprValue>): ExprValue {
                TODO("Implementation details without optional argument")
            }

            override fun callWithOptional(env: Environment, required: List<ExprValue>, opt: ExprValue): ExprValue {
                TODO("Implementation details with optional argument")
            }
        }
    }
}
