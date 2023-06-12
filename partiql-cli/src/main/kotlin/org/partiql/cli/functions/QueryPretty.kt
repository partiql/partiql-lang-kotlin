import com.amazon.ion.IonSystem
import com.amazon.ion.IonWriter
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

class QueryPretty(private val ion: IonSystem) : ExprFunction {

    override val signature = FunctionSignature(
        name = "pretty_write",
        requiredParameters = listOf(StaticType.ANY),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val str = required[0].toString()
        val pretty = prettyWrite(str)

        // Wrap the result in an ExprValue and return
        return ExprValue.newString(pretty)
    }

    fun rewrite(textIon: String, writer: IonWriter) {
        IonReaderBuilder.standard().build(textIon).use { reader ->
            writer.writeValues(reader)
        }
    }

    fun prettyWrite(unformatted: String): String {
        val stringBuilder = StringBuilder()
        IonTextWriterBuilder.pretty().build(stringBuilder).use { prettyWriter ->
            rewrite(unformatted, prettyWriter)
        }
        return stringBuilder.toString()
    }
}
