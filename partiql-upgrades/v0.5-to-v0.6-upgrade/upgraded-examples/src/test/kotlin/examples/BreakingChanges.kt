package examples

import org.partiql.lang.eval.EvaluationSession
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

            // Starting in v0.6.0, the `callWith*` functions' first argument is changed to `EvaluationSession`.
            // `Environment` has also been made private
            override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
                TODO("Implementation details without optional argument")
            }

            override fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
                TODO("Implementation details with optional argument")
            }
        }
    }
}
