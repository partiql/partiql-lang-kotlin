package org.partiql.testscript.parser

import com.amazon.ion.IonType
import org.partiql.testscript.PtsError
import org.partiql.testscript.TestScriptError

internal sealed class ParserError(
        override val errorMessage: String,
        override val scriptLocation: ScriptLocation) : TestScriptError()

internal class EmptyError(valuePath: String, scriptLocation: ScriptLocation) :
        ParserError("Field must have at least one element: $valuePath", scriptLocation)


internal class InvalidNumberOfAnnotationsError(numberOfAnnotations: Int, scriptLocation: ScriptLocation) :
        ParserError("Wrong number of annotations. Expected 1 got: $numberOfAnnotations", scriptLocation)

internal class UnknownFunctionError(functionName: String, scriptLocation: ScriptLocation) :
        ParserError("Unknown PTS function: $functionName", scriptLocation)

internal class DuplicatedFieldError(valuePath: String, scriptLocation: ScriptLocation) :
        ParserError("DuplicatedField: $valuePath", scriptLocation)


internal class MissingRequiredError(valuePath: String, scriptLocation: ScriptLocation) :
        ParserError("Missing required field: $valuePath", scriptLocation)

internal class UnexpectedIonTypeError(
        valuePath: String,
        expected: IonType,
        actual: IonType,
        scriptLocation: ScriptLocation) :
        ParserError("Wrong type for $valuePath. Expected $expected, got $actual", scriptLocation)

internal class InvalidTemplateValueError(valuePath: String, scriptLocation: ScriptLocation) :
        ParserError(
                "Invalid template value for field: $valuePath. Must start with '$' when it's a SYMBOL",
                scriptLocation)

internal class UnexpectedFieldError(valuePath: String, scriptLocation: ScriptLocation) :
        ParserError("Unexpected field: $valuePath", scriptLocation)

internal class MissingTemplateVariableError(variable: String, scriptLocation: ScriptLocation) :
        ParserError("Missing template variable: $variable", scriptLocation)

internal class InvalidExpectedTagError(valuePath: String, tag: String, scriptLocation: ScriptLocation) :
        ParserError("Invalid $valuePath tag, must be either 'success' or 'error' got '$tag'", scriptLocation)

internal class InvalidExpectedErrorSizeError(valuePath: String, scriptLocation: ScriptLocation) :
        ParserError("$valuePath error can only have a single element, e.g. (error)", scriptLocation)

internal class InvalidExpectedSuccessSizeError(valuePath: String, scriptLocation: ScriptLocation) :
        ParserError("$valuePath success must have two elements, e.g. (success (bag {a: 1}))", scriptLocation)

