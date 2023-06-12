import com.amazon.ion.IonSystem
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.numberValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

class QueryPower(private val ion: IonSystem) : ExprFunction {

    override val signature = FunctionSignature(
        name = "query_power",
        requiredParameters = listOf(StaticType.INT, StaticType.INT),
//            optionalParameter = StaticType.INT,
        returnType = StaticType.INT
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val base = required[0].numberValue()
        val exponent = required[1].numberValue()
        val result = Math.pow(base.toDouble(), exponent.toDouble()).toInt()
        // Wrap the result in an ExprValue and return
        return ExprValue.newInt(result)
    }
}
