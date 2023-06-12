import com.amazon.ion.IonSystem
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

class QueryStringfy(private val ion: IonSystem) : ExprFunction {

    override val signature = FunctionSignature(
        name = "cast_as_string",
        requiredParameters = listOf(StaticType.ANY),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val str = required[0].toString()

        // Wrap the result in an ExprValue and return
        return ExprValue.newString(str)
    }
}
