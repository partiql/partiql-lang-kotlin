package org.partiql.testscript.parser.ast.builders

import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonSystem
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import org.partiql.testscript.Failure
import org.partiql.testscript.Result
import org.partiql.testscript.Success
import org.partiql.testscript.TestScriptError
import org.partiql.testscript.extensions.UndefinedVariableInterpolationException
import org.partiql.testscript.extensions.crossProduct
import org.partiql.testscript.extensions.interpolate
import org.partiql.testscript.extensions.toIonText
import org.partiql.testscript.parser.EmptyError
import org.partiql.testscript.parser.InvalidTemplateValueError
import org.partiql.testscript.parser.IonInputReader
import org.partiql.testscript.parser.IonValueWithLocation
import org.partiql.testscript.parser.MissingRequiredError
import org.partiql.testscript.parser.MissingTemplateVariableError
import org.partiql.testscript.parser.ScriptLocation
import org.partiql.testscript.parser.UnexpectedFieldError
import org.partiql.testscript.parser.UnexpectedIonTypeError
import org.partiql.testscript.parser.ast.TestNode
import org.partiql.testscript.parser.ast.TestTemplate
import org.partiql.testscript.parser.ast.VariableSet

internal class ForBuilder(private val ion: IonSystem, private val location: ScriptLocation) {
    private var templates: MutableList<Result<TestTemplate>>? = null
    private var variableSets: MutableList<Result<VariableSet>>? = null

    private val errors = mutableListOf<TestScriptError>()

    fun setValue(name: String, reader: IonInputReader) {
        when (name) {
            "template" -> {
                templates = mutableListOf()

                setTemplate(reader)
            }
            "variable_sets" -> {
                variableSets = mutableListOf()

                setVariableSet(reader)
            }
            else -> errors.add(UnexpectedFieldError("for.$name", reader.currentScriptLocation()))
        }
    }

    private fun setTemplate(reader: IonInputReader) {
        if (reader.type != IonType.LIST) {
            val error = Failure<TestTemplate>(
                UnexpectedIonTypeError(
                    "for.template",
                    IonType.LIST,
                    reader.type,
                    reader.currentScriptLocation()
                )
            )
            templates!!.add(error)
        } else {
            reader.stepIn()
            var index = 0
            while (reader.next() != null) {
                val location = reader.currentScriptLocation()
                val valuePath = "for.template[$index]"

                if (reader.type != IonType.STRUCT) {
                    templates!!.add(Failure(UnexpectedIonTypeError(valuePath, IonType.STRUCT, reader.type, location)))
                } else {
                    val builder = TestTemplateBuilder(valuePath, location)
                    reader.stepIn()
                    while (reader.next() != null) {
                        builder.setValue(reader.fieldName, reader.ionValueWithLocation())
                    }
                    templates!!.add(builder.build())
                    reader.stepOut()
                }
                index += 1
            }

            reader.stepOut()
        }
    }

    private fun setVariableSet(reader: IonInputReader) {
        if (reader.type != IonType.LIST) {
            val error = Failure<VariableSet>(
                UnexpectedIonTypeError(
                    "for.variable_sets",
                    IonType.LIST,
                    reader.type,
                    reader.currentScriptLocation()
                )
            )

            variableSets!!.add(error)
        } else {
            reader.stepIn()
            var index = 0
            while (reader.next() != null) {
                val result: Result<VariableSet> = if (reader.type == IonType.STRUCT) {
                    val value = reader.ionValueWithLocation()
                    Success(VariableSet(value.ionValue as IonStruct, value.scriptLocation))
                } else {
                    Failure(
                        UnexpectedIonTypeError(
                            "variable_sets[$index]",
                            IonType.STRUCT,
                            reader.type,
                            reader.currentScriptLocation()
                        )
                    )
                }
                variableSets!!.add(result)

                index += 1
            }
            reader.stepOut()
        }
    }

    private fun validateRequired(label: String, field: List<Result<out Any>>?) {
        if (field == null) {
            errors.add(MissingRequiredError(label, location))
        }
    }

    private fun validateNotEmpty(label: String, field: List<Result<out Any>>?) {
        if (field != null && field.isEmpty()) {
            errors.add(EmptyError(label, location))
        }
    }

    private fun buildTest(testTemplate: TestTemplate, variableSet: VariableSet): Result<TestNode> {
        val location = variableSet.scriptLocation

        val testBuilder = TestBuilder("for.template", location)

        val id = IonValueWithLocation(
            ion.newSymbol("${testTemplate.id}\$\$${variableSet.variables.toIonText()}"),
            location
        )

        testBuilder.setValue("id", id)

        val errors = mutableListOf<TestScriptError>()

        testTemplate.description?.let {
            val result = interpolate(it, variableSet)
            when (result) {
                is Success -> {
                    testBuilder.setValue("description", IonValueWithLocation(result.value, location))
                }
                is Failure -> errors.addAll(result.errors)
            }
        }

        testTemplate.statement.let {
            val result = interpolate(it, variableSet)
            when (result) {
                is Success -> {
                    testBuilder.setValue("statement", IonValueWithLocation(result.value, location))
                }
                is Failure -> errors.addAll(result.errors)
            }
        }

        testTemplate.environment?.let {
            val result = interpolate(it, variableSet)
            when (result) {
                is Success -> {
                    testBuilder.setValue("environment", IonValueWithLocation(result.value, location))
                }
                is Failure -> errors.addAll(result.errors)
            }
        }

        testTemplate.expected.let {
            val result = interpolate(it, variableSet)
            when (result) {
                is Success -> {
                    testBuilder.setValue("expected", IonValueWithLocation(result.value, location))
                }
                is Failure -> errors.addAll(result.errors)
            }
        }

        return if (errors.isEmpty()) {
            testBuilder.build()
        } else {
            Failure(errors)
        }
    }

    fun build(): Result<List<TestNode>> {
        validateRequired("for.template", templates)
        validateNotEmpty("for.template", templates)

        validateRequired("for.variable_sets", variableSets)
        validateNotEmpty("for.variable_sets", variableSets)

        if (errors.isNotEmpty()) {
            return Failure(errors)
        }

        val (validTemplates, templatesWithErrors) = templates!!.partition { it is Success }
        val (validVariableSets, variableSetsWithErrors) = variableSets!!.partition { it is Success }

        templatesWithErrors.map { it as Failure }
            .union(variableSetsWithErrors.map { it as Failure })
            .forEach { errors.addAll(it.errors) }

        val testNodes = mutableListOf<TestNode>()
        validTemplates.crossProduct(validVariableSets).forEach {
            val template = (it.first as Success).value
            val variableSet = (it.second as Success).value

            val result = buildTest(template, variableSet)
            when (result) {
                is Success -> testNodes.add(result.value)
                is Failure -> errors.addAll(result.errors)
            }
        }

        return if (errors.isEmpty()) {
            Success(testNodes)
        } else {
            Failure(errors)
        }
    }

    private fun interpolate(target: IonValue, variableSet: VariableSet): Result<IonValue> = try {
        Success(target.interpolate(variableSet.variables))
    } catch (e: UndefinedVariableInterpolationException) {
        Failure(MissingTemplateVariableError(e.variableName, variableSet.scriptLocation))
    }
}

private class TestTemplateBuilder(path: String, location: ScriptLocation) :
    StructBuilder<TestTemplate>(path, location) {

    private fun validateTemplate(label: String, field: IonValueWithLocation?, expected: IonType) {
        if (field == null) {
            return
        }

        if (field.ionValue.type == IonType.SYMBOL) {
            if (!(field.ionValue as IonSymbol).stringValue().startsWith("$")) {
                errors.add(InvalidTemplateValueError(label, field.scriptLocation))
            }
        } else if (field.ionValue.type != expected) {
            errors.add(UnexpectedIonTypeError(label, expected, field.ionValue.type, field.scriptLocation))
        }
    }

    override fun build(): Result<TestTemplate> {
        val id = fieldMap.remove("id")
        val statement = fieldMap.remove("statement")
        val expected = fieldMap.remove("expected")
        val description = fieldMap.remove("description")
        val environment = fieldMap.remove("environment")

        validateUnexpectedFields()

        validateRequired("$path.id", id)
        validateType("$path.id", id, IonType.SYMBOL)

        validateTemplate("$path.description", description, IonType.STRING)

        validateRequired("$path.statement", statement)
        validateTemplate("$path.statement", statement, IonType.STRING)

        validateTemplate("$path.environment", environment, IonType.STRUCT)

        validateRequired("$path.expected", expected)
        validateTemplate("$path.expected", expected, IonType.SEXP)

        return if (errors.isEmpty()) {
            Success(
                TestTemplate(
                    id = (id!!.ionValue as IonSymbol).stringValue(),
                    description = description?.ionValue,
                    statement = statement!!.ionValue,
                    environment = environment?.ionValue,
                    expected = expected!!.ionValue,
                    scriptLocation = location
                )
            )
        } else {
            Failure(errors)
        }
    }
}
