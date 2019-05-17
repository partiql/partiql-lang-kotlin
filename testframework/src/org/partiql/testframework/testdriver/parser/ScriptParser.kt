/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.testframework.testdriver.parser

import com.amazon.ion.*
import com.amazon.ion.IonType.*
import org.partiql.testframework.testdriver.ast.*
import org.partiql.testframework.testdriver.error.*
import java.io.*
import java.nio.file.*
import java.util.*

private const val MAX_INCLUDE_DEPTH = 10

class ScriptParser(
    private val reader: IonReader,
    private val scriptPath: String,
    private val ions: IonSystem,
    private val resultStream: ResultStream,
    private val nestedIncludeDepth: Int = 0) {

    private val parselets: Map<String, () -> ScriptCommand> = mapOf(
        "include"                       to { parseIncludeCommand() },
        "test"                          to { parseTestCommand() },
        "benchmark"                     to { parseBenchmarkCommand() },
        "set_default_environment"       to { parseSetDefaultEnvironmentCommand() },
        "set_default_compile_options"   to { parseSetDefaultCompileOptionsCommand() },
        "set_default_session"           to { parseDefaultSessionCommands() },
        "for"                           to { parseFor() })

    fun parse(): TestScript {
        return TestScript(
            scriptPath,
            try {
                val cmds = ArrayList<ScriptCommand>()
                var cmd = parseCommand()
                while (cmd != null) {
                    cmds.add(cmd)
                    cmd = parseCommand()
                }

                cmds
            } catch(iex: IonException) {
                resultStream.error(currentLocation(), "IonException encountered while parsing this line, message: ${iex.message}")
                throw TestScriptParseException("IonException encountered.  See resultStream and cause.", iex)
            })
    }

    private fun parseCommand(): ScriptCommand? {
        if(reader.next() == null) return null

        expectOneAnnotation()
        val commandName = reader.typeAnnotations.first()

        val parselet = parselets[commandName] ?: parseError("Invalid command $commandName")
        val cmd: ScriptCommand = parselet()

        return cmd
    }

    private fun parseIncludeCommand(): Include {
        expectIonType(STRING)
        val scriptPath = computeAbsolutePath(reader.stringValue())

        if(nestedIncludeDepth >= MAX_INCLUDE_DEPTH) {
            parseError("Maximum nested include depth ($MAX_INCLUDE_DEPTH) exceeded.  Do your scripts have a circular include reference?")
        }

        try {
            FileInputStream(scriptPath)
        } catch(ex: FileNotFoundException) {
            parseError("could not open $scriptPath (cause: ${ex.message})")
        }.use {

            resultStream.pushIncludeContext(currentLocation())
            val parser = ScriptParser(ions.newReader(it), scriptPath, ions, resultStream, nestedIncludeDepth + 1)
            val script = parser.parse()

            resultStream.popContext()
            return Include(currentLocation(), script)
        }
    }

    private fun parseTestCommand(): TestCommand {
        expectIonType(STRUCT)
        val startingLocation = currentLocation()
        reader.stepIn()

        var name: String? = null
        var description: String? = null
        var sql: IonText = ions.newString("")
        var alternateEnvironmentSpec: EnvironmentSpec? = null
        var alternateCompileOptions: IonValue? = null
        var alternateSession: IonValue? = null

        var expected: Expectation? = null

        while (reader.next() != null) {

            when (reader.fieldName) {
                "name"              -> {
                    expectIonType(STRING, SYMBOL)
                    name = reader.stringValue()
                }
                "description"       -> {
                    expectIonType(STRING)
                    description = reader.stringValue()
                }
                "sql"               -> {
                    expectIonType(STRING, SYMBOL)
                    sql = ions.newValue(reader) as IonText
                }
                "environment"       -> {
                    alternateEnvironmentSpec = parseEnvironmentSpec()
                }
                "compile_options"   -> {
                    expectIonType(STRUCT, SYMBOL)
                    alternateCompileOptions = ions.newValue(reader).apply { clearTypeAnnotations() }
                }
                "session"           -> {
                    expectIonType(STRUCT)
                    alternateSession = ions.newValue(reader).apply { clearTypeAnnotations() }
                }
                "expected"          -> expected = parseExpectation()
                else -> parseError("Unexpected field '${reader.fieldName}'")
            }
        }
        reader.stepOut()
        return TestCommand(
            startingLocation,
            name,
            description,
            sql,
            expected, alternateEnvironmentSpec,
            alternateCompileOptions,
            alternateSession)
    }

    private fun parseBenchmarkCommand(): BenchmarkCommand {
        expectIonType(STRUCT)
        val startingLocation = currentLocation()
        reader.stepIn()

        var name: String? = null
        var description: String? = null
        var sql: IonText = ions.newString("")
        var alternateEnvironmentSpec: EnvironmentSpec? = null
        var alternateCompileOptions: IonValue? = null
        var alternateSession: IonValue? = null

        var expected: Expectation? = null

        fun checkExpectedNotSet() {
            if (expected != null) {
                parseError("Expectation defined more than once in ${currentLocation()}")
            }
        }

        while (reader.next() != null) {
            when (reader.fieldName) {
                "name"              -> {
                    expectIonType(STRING, SYMBOL)
                    name = reader.stringValue()
                }
                "description"       -> {
                    expectIonType(STRING)
                    description = reader.stringValue()
                }
                "sql"               -> {
                    expectIonType(STRING, SYMBOL)
                    sql = ions.newValue(reader) as IonText
                }
                "environment"       -> {
                    alternateEnvironmentSpec = parseEnvironmentSpec()
                }
                "compile_options"   -> {
                    expectIonType(STRUCT, SYMBOL)
                    alternateCompileOptions = ions.newValue(reader).apply { clearTypeAnnotations() }
                }
                "session"           -> {
                    expectIonType(STRUCT)
                    alternateSession = ions.newValue(reader).apply { clearTypeAnnotations() }
                }
                "expected"          -> {
                    checkExpectedNotSet()
                    expected = parseExpectation()
                }
                "expected_count"    -> {
                    checkExpectedNotSet()
                    expected = parseCountExpectation()
                }
                else -> parseError("Unexpected field '${reader.fieldName}'")
            }
        }

        reader.stepOut()

        return BenchmarkCommand(
            startingLocation,
            name,
            description,
            sql,
            expected,
            alternateEnvironmentSpec,
            alternateCompileOptions,
            alternateSession)
    }


    private fun parseFor(): For {
        expectIonType(STRUCT)
        val startingLocation = currentLocation()
        reader.stepIn()
        var scriptCommandList: ScriptCommandList? = null
        var variableSets: List<VariableSet>? = null

        while(reader.next() != null) {
            when (reader.fieldName) {
                "template"     -> {
                    scriptCommandList = parseListOfCommands()
                }
                "variableSets" -> {
                    expectIonType(LIST)
                    variableSets = parseListOfVariableSets()
                }
                else           -> parseError("Unexpected field ${reader.fieldName}")
            }
        }
        reader.stepOut()
        return For(startingLocation, variableSets ?: ArrayList(), scriptCommandList)
    }

    fun parseEnvironmentSpec(useLocation: ScriptLocation = currentLocation()): EnvironmentSpec {
        expectIonType(STRUCT, STRING, LIST, SYMBOL)
        return when (reader.type) {
            STRUCT -> EnvironmentSpecStruct(useLocation,
                                            ions.newValue(reader).apply { clearTypeAnnotations() } as IonStruct)
            STRING -> EnvironmentSpecFiles(useLocation, listOf(reader.stringValue()))
            LIST   -> EnvironmentSpecFiles(useLocation, parseListOfStrings())
            SYMBOL -> EnvironmentSpecVariableRef(useLocation, reader.stringValue())
            else   -> parseError("Unhandled ion type (this code should be unreachable)")
        }
    }

    fun parseCountExpectation(useLocation: ScriptLocation = currentLocation()): Expectation {
        if(reader.type == SYMBOL && !reader.isNullValue) {
            val value = reader.stringValue()
            if(value.startsWith('$')) {
                return VariableRefExpectation(useLocation, reader.stringValue(), VariableRefExpectation.Type.EXPECTED_COUNT)
            }
        }

        expectIonType(INT)
        val countValue = reader.intValue()
        return CountExpectation(useLocation, countValue)
    }

    fun parseExpectation(useLocation: ScriptLocation = currentLocation()): Expectation {
        if(reader.type == SYMBOL && !reader.isNullValue) {
            val value = reader.stringValue()
            if(value.startsWith('$')) {
                return VariableRefExpectation(useLocation, reader.stringValue(), VariableRefExpectation.Type.EXPECTED)
            }
        }

        expectOneAnnotation()

        val expectationType = reader.typeAnnotations[0]
        return when(expectationType) {
            "missing" -> parseMissingExpectation()
            "result" -> parseSuccessExpectation()
            "error"  -> parseErrorExpectation()
            else     -> parseError("Unknown expectation type: $expectationType")
        }
    }

    private fun parseMissingExpectation(): SuccessExpectation {
        val startingLocation = currentLocation()
        val hopefullyIonNull = ions.newValue(reader)
        if(!hopefullyIonNull.isNullValue || hopefullyIonNull.type != NULL) {
            parseError("Only 'null' can be missing")
        }
        return SuccessExpectation(startingLocation, hopefullyIonNull)
    }

    private fun parseSuccessExpectation(): Expectation {
        val startingLocation = currentLocation()
        val expectedIon = ions.newValue(reader).apply { clearTypeAnnotations() }
        return SuccessExpectation(startingLocation, expectedIon)
    }

    private fun parseErrorExpectation(): ErrorExpectation {
        expectIonType(STRUCT)
        val startingLocation = currentLocation()
        reader.stepIn()

        var errorCode: IonText? = null
        var properties: IonStruct? = null

        while(reader.next() != null) {
            when(reader.fieldName) {
                "code" -> {
                    expectIonType(STRING, SYMBOL)
                    errorCode = ions.newValue(reader) as IonText
                }
                "properties" -> {
                    expectIonType(STRUCT)
                    properties = ions.newValue(reader).apply { clearTypeAnnotations() } as IonStruct
                }
            }
        }
        reader.stepOut()
        return ErrorExpectation(startingLocation, errorCode, properties)
    }

    private fun parseSetDefaultEnvironmentCommand(): ScriptCommand {
        return SetDefaultEnvironment(currentLocation(), parseEnvironmentSpec())
    }

    private fun parseSetDefaultCompileOptionsCommand(): SetDefaultCompileOptions {
        expectIonType(STRUCT, SYMBOL)
        val startLocation = currentLocation()
        val sessionStruct = ions.newValue(reader).apply { clearTypeAnnotations() }
        return SetDefaultCompileOptions(startLocation, sessionStruct)
    }

    private fun parseDefaultSessionCommands(): SetDefaultSession {
        expectIonType(STRUCT)
        val startLocation = currentLocation()
        val sessionStruct = ions.newValue(reader).apply { clearTypeAnnotations() } as IonStruct
        return SetDefaultSession(startLocation, sessionStruct)
    }


    private fun parseListOfCommands(): ScriptCommandList? {
        expectIonType(LIST)
        reader.stepIn()

        val templateCommands = mutableListOf<ScriptCommand>()
        val templateStartingLocation = currentLocation()

        var cmd = parseCommand()
        while (cmd != null) {
            templateCommands.add(cmd)
            cmd = parseCommand()
        }
        reader.stepOut()
        return ScriptCommandList(templateStartingLocation, templateCommands)
    }

    private fun parseListOfStrings(): List<String> {
        expectIonType(LIST)
        reader.stepIn()
        val strings = mutableListOf<String>()
        while(reader.next() != null) {
            expectIonType(STRING)
            strings.add(reader.stringValue())
        }
        reader.stepOut()
        return strings
    }

    private fun parseListOfStructs() : List<IonStruct> {
        expectIonType(LIST)
        reader.stepIn()
        val structs = mutableListOf<IonStruct>()
        while(reader.next() != null) {
            expectIonType(STRUCT)
            structs.add(ions.newValue(reader) as IonStruct)
        }
        reader.stepOut()
        return structs
    }

    private fun parseListOfVariableSets() : List<VariableSet> {
        expectIonType(LIST)

        reader.stepIn()
        val variableSets = mutableListOf<VariableSet>()
        while(reader.next() != null) {
            expectIonType(STRUCT)
            variableSets.add(VariableSet(currentLocation(), ions.newValue(reader) as IonStruct))
        }
        reader.stepOut()
        return variableSets
    }

    private fun expectIonType(expectedIonType: IonType) {
        if(reader.type != expectedIonType) {
            parseError("Expected a $expectedIonType but ${reader.type} was encountered")
        }
    }

    private fun expectIonType(vararg t : IonType) {
        if (!t.contains(reader.type)) {
            parseError("Expected one of [${t.joinToString(", ")}] but ${reader.type} was encountered")
        }
    }

    private fun expectOneAnnotation() {
        if(reader.typeAnnotations.size != 1){
            parseError("Expected 1 annotation instead of ${reader.typeAnnotations.size}")
        }
    }

    private fun currentLocation(): ScriptLocation {
        return ScriptLocation(scriptPath, dirtyHackToGetTheLineNum())
    }

    private fun parseError(message :String) : Nothing {
        resultStream.error(currentLocation(), message)
        throw TestScriptParseException("Error encountered while parsing test script -- see error stream.")
    }

    private fun computeAbsolutePath(relativePath: String): String =
        // TODO:  examine exceptions that could be thrown by this...
        Paths.get(File(scriptPath).parent, relativePath).toAbsolutePath().toString()

    // TODO: https://github.com/amzn/ion-java/issues/226
    private fun dirtyHackToGetTheLineNum(): Long {
        //Attempt to get the line number using the dirty hack first (this way provides accurate line numbers)
        try {
            val scannerField = reader.javaClass.superclass.superclass.getDeclaredField("_scanner")
            //Ow
            scannerField.isAccessible = true
            //Gah
            val scanner = scannerField.get(reader)
            //My
            val lineCountField = scanner.javaClass.getDeclaredField("_line_count")
            //Eyes!
            lineCountField.isAccessible = true
            //Put me out of...
            val lineNum = lineCountField.get(scanner) as Long
            //my misery!
            return lineNum
        } catch(_: NoSuchFieldException) {
            return -1
            //TODO:  figure out how to get around this problem because even the inaccurate SpanProvider doesn't work
            //the ion tree reader
            //            val facet = reader.asFacet(SpanProvider::class.java)
            //            return (facet.currentSpan() as TextSpan).startLine
        }
    }
}
