package org.partiql.testscript.parser.ast.builders

import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonType
import org.partiql.testscript.Failure
import org.partiql.testscript.Result
import org.partiql.testscript.Success
import org.partiql.testscript.parser.EmptyError
import org.partiql.testscript.parser.InvalidExpectedErrorSizeError
import org.partiql.testscript.parser.InvalidExpectedSuccessSizeError
import org.partiql.testscript.parser.InvalidExpectedTagError
import org.partiql.testscript.parser.IonValueWithLocation
import org.partiql.testscript.parser.ScriptLocation
import org.partiql.testscript.parser.UnexpectedIonTypeError
import org.partiql.testscript.parser.ast.TestNode

internal class TestBuilder(path: String, location: ScriptLocation) : StructBuilder<TestNode>(path, location) {
    private fun validateExpected(expected: IonValueWithLocation?) {
        val initialErrorCount = errors.size
        val fieldPath = "$path.expected"

        validateRequired(fieldPath, expected)
        validateType(fieldPath, expected, IonType.SEXP)

        if (initialErrorCount == errors.size) {
            val sexp = expected!!.ionValue as IonSexp
            val location = expected.scriptLocation

            val error = when {
                sexp.isEmpty -> EmptyError(fieldPath, location)
                sexp[0].type != IonType.SYMBOL -> UnexpectedIonTypeError("$fieldPath[0]", IonType.SYMBOL, sexp[0].type, location)
                else -> {
                    val tag = (sexp[0] as IonSymbol).stringValue()
                    when {
                        !listOf("error", "success").contains(tag) -> InvalidExpectedTagError(fieldPath, tag, location)
                        tag == "error" && sexp.size != 1 -> InvalidExpectedErrorSizeError(fieldPath, location)
                        tag == "success" && sexp.size != 2 -> InvalidExpectedSuccessSizeError(fieldPath, location)
                        else -> null
                    }
                }
            }

            error?.let { errors.add(it) }
        }
    }

    override fun build(): Result<TestNode> {
        val id = fieldMap.remove("id")
        val description = fieldMap.remove("description")
        val statement = fieldMap.remove("statement")
        val environment = fieldMap.remove("environment")
        val expected = fieldMap.remove("expected")

        validateUnexpectedFields()

        validateRequired("$path.id", id)
        validateType("$path.id", id, IonType.SYMBOL)

        validateType("$path.description", description, IonType.STRING)

        validateRequired("$path.statement", statement)
        validateType("$path.statement", statement, IonType.STRING)

        validateType("$path.environment", environment, IonType.STRUCT)

        validateExpected(expected)

        return if (errors.isEmpty()) {
            Success(
                TestNode(
                    id = (id!!.ionValue as IonSymbol).stringValue(),
                    description = (description?.ionValue as IonString?)?.stringValue(),
                    statement = (statement!!.ionValue as IonString).stringValue(),
                    environment = environment?.ionValue as IonStruct?,
                    expected = expected!!.ionValue as IonSexp,
                    scriptLocation = location
                )
            )
        } else {
            Failure(errors)
        }
    }
}
