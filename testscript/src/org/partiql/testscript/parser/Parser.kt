package org.partiql.testscript.parser

import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonType
import org.partiql.testscript.Failure
import org.partiql.testscript.Result
import org.partiql.testscript.Success
import org.partiql.testscript.TestScriptError
import org.partiql.testscript.parser.ast.*
import org.partiql.testscript.parser.ast.builders.AppendTestBuilder
import org.partiql.testscript.parser.ast.builders.ForBuilder
import org.partiql.testscript.parser.ast.builders.StructBuilder
import org.partiql.testscript.parser.ast.builders.TestBuilder

//TODO replace the validation side of this class by ion-schema if/when https://github.com/amzn/ion-schema-kotlin/issues/120 
// is completed. Ion schema will provide much richer error messages but without the line numbers it's hard to figure 
// out where a correction must be made

/**
 * PTS parser. 
 */
class Parser(private val ion: IonSystem) {

    fun parse(ionDocuments: List<NamedInputStream>): List<ModuleNode> {
        val modules = mutableListOf<ModuleNode>()
        val errors = mutableListOf<TestScriptError>()

        for (input in ionDocuments) {
            val result = IonInputReader(input.name, ion, ion.newReader(input)).use { parseModule(it) }

            when (result) {
                is Failure -> errors.addAll(result.errors)
                is Success -> modules.add(result.value)
            }
        }

        if (errors.isNotEmpty()) {
            throw ParserException(errors.map { it.toPtsError() })
        }

        return modules
    }

    private fun parseModule(reader: IonInputReader): Result<ModuleNode> {
        val errors = mutableListOf<TestScriptError>()
        val astNodes = mutableListOf<AstNode>()

        while (reader.next() != null) {
            val annotations = reader.typeAnnotations
            if (annotations.size != 1) {
                errors.add(InvalidNumberOfAnnotationsError(annotations.size, reader.currentScriptLocation()))
            } else {
                val name = annotations[0]

                val result = parseFunction(name, reader)
                when (result) {
                    is Failure -> errors.addAll(result.errors)
                    is Success -> astNodes.addAll(result.value)
                }
            }
        }

        return if (errors.isNotEmpty()) {
            Failure(errors)
        } else {
            Success(ModuleNode(astNodes, ScriptLocation(reader.inputName, 0)))
        }
    }

    private fun parseFunction(name: String, reader: IonInputReader): Result<out List<AstNode>> {
        fun <T> wrap(result: Result<T>): Result<out List<T>> = when (result) {
            is Success -> Success(listOf(result.value))
            is Failure -> Failure(result.errors)
        }

        return when (name) {
            "set_default_environment" -> wrap(parseSetDefaultEnvironment(reader))
            "skip_list" -> wrap(parseSkipList(reader))
            "test" -> wrap(parseTest(reader))
            "append_test" -> wrap(parseAppendTest(reader))
            "for" -> parseFor(reader)
            else -> Failure(UnknownFunctionError(name, reader.currentScriptLocation()))
        }
    }

    private fun parseSetDefaultEnvironment(reader: IonInputReader): Result<SetDefaultEnvironmentNode> {
        return if (reader.type != IonType.STRUCT) {
            Failure(UnexpectedIonTypeError(
                    "set_default_environment",
                    IonType.STRUCT,
                    reader.type,
                    reader.currentScriptLocation()))
        } else {
            val value = reader.ionValueWithLocation()
            
            // remove the set_default_environment annotation
            value.ionValue.clearTypeAnnotations() 
            Success(SetDefaultEnvironmentNode(value.ionValue as IonStruct, value.scriptLocation))
        }
    }

    private fun parseSkipList(reader: IonInputReader): Result<SkipListNode> {
        val location = reader.currentScriptLocation()
        if (reader.type != IonType.LIST) {
            return Failure(UnexpectedIonTypeError("skip_list", IonType.LIST, reader.type, location))
        }

        val errors = mutableListOf<TestScriptError>()
        val patterns = mutableListOf<String>()

        reader.stepIn {
            it.forEachIndexed { index, _ ->
                if (reader.type == IonType.STRING) {
                    patterns.add(reader.stringValue())
                } else {
                    errors.add(UnexpectedIonTypeError(
                            "skip_list[$index]",
                            IonType.STRING,
                            reader.type,
                            reader.currentScriptLocation()))
                }
            }
        }

        return if (errors.isNotEmpty()) {
            Failure(errors)
        } else {
            Success(SkipListNode(patterns, location))
        }
    }

    private fun <T> parseStructFunction(reader: IonInputReader, builder: StructBuilder<T>): Result<T> {
        if (reader.type != IonType.STRUCT) {
            return Failure(UnexpectedIonTypeError(builder.path, IonType.STRUCT, reader.type, builder.location))
        }

        reader.stepIn {
            it.forEach { _ ->
                builder.setValue(reader.fieldName, reader.ionValueWithLocation())
            }
        }

        return builder.build()
    }

    private fun parseTest(reader: IonInputReader): Result<TestNode> =
            parseStructFunction(reader, TestBuilder("test", reader.currentScriptLocation()))

    private fun parseAppendTest(reader: IonInputReader): Result<AppendTestNode> =
            parseStructFunction(reader, AppendTestBuilder(reader.currentScriptLocation()))

    private fun parseFor(reader: IonInputReader): Result<List<TestNode>> {
        val location = reader.currentScriptLocation()
        if (reader.type != IonType.STRUCT) {
            return Failure(UnexpectedIonTypeError(
                    "for",
                    IonType.STRUCT,
                    reader.type,
                    location))
        }

        val builder = ForBuilder(ion, location)

        reader.stepIn { seq -> 
            seq.forEach { _ -> builder.setValue(reader.fieldName, reader) }
        }

        return builder.build()
    }
}