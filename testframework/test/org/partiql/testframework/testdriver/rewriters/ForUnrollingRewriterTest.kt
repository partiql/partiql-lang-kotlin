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

package org.partiql.testframework.testdriver.rewriters

import org.partiql.testframework.testdriver.*
import org.partiql.testframework.testdriver.ast.*
import org.junit.*
import kotlin.test.*

class ForUnrollingRewriterTest : TestBase() {

    private fun parseFor(template: String): TestScript =
        parseScript("""
                for::{
                    template: [$template],
                    variableSets: [
                        {
                            aVariable: "this is aVariable",
                            anotherVariable: "this is anotherVariable",
                            aSuccessExpectation: result::"this is aSuccessExpectation",
                            anErrorExpectation: error:: { code: SOME_ERROR },
                            aMissingExpectation: missing::null,
                            aGeneralPurposeStruct: { general_purpose_field: "some value" },
                            aTable: [ { a: "b" }, { a: "c" } ],
                            count: 2
                        }
                    ]
                }
            """)

    private fun TestScript.firstExpandedScriptCommand(): ScriptCommand {
        val forReplacement = this.commands.first() as ScriptCommandList
        val expandedTemplate = forReplacement.commands.first() as ScriptCommandList
        return expandedTemplate.commands.first()
    }

    private fun parseForExtractFirstExpandedTestCommand(script: String): TestCommand =
        parseFor(script).firstExpandedScriptCommand() as TestCommand

    /////////// TestCommand.sql
    @Test
    fun testCommandSqlFieldStringInterpolation() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', sql: "before #{aVariable} after" }""")
        assertEquals("before this is aVariable after", expanded.sql.stringValue())
    }

    @Test
    fun testCommandSqlFieldVariableReference() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', sql: #aVariable }""")
        assertEquals("this is aVariable", expanded.sql.stringValue())
    }

    /////////// TestCommand.expected is success::
    @Test
    fun testCommandExpectedFieldVariableReferenceResult() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: "test", expected: #aSuccessExpectation }""")
        val successExpectation = expanded.expectation as SuccessExpectation
        assertEquals(ion.singleValue("\"this is aSuccessExpectation\""), successExpectation.expectedResult)
    }

    @Test
    fun testCommandExpectedFieldIonInterpolationResult() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', expected: result::{ value: #aVariable } }""")
        val successExpectation = expanded.expectation as SuccessExpectation
        assertEquals(ion.singleValue("{value: \"this is aVariable\"}"), successExpectation.expectedResult)
    }

    /////////// TestCommand.expected is error::
    @Test
    fun testCommandExpectedFieldVariableReferenceError() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', expected: #anErrorExpectation }""")
        val successExpectation = expanded.expectation as ErrorExpectation
        assertEquals(ion.singleValue("SOME_ERROR"), successExpectation.expectedErrorCode)
    }

    @Test
    fun testCommandExpectedFieldIonInterpolationError() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', expected: error::{ code: #aVariable } }""")
        val failureExpectation = expanded.expectation as ErrorExpectation
        assertEquals("this is aVariable", failureExpectation.expectedErrorCode!!.stringValue())
    }

    /////////// TestCommand.expected is missing::
    @Test
    fun testCommandExpectedFieldVariableReferenceMissing() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', expected: #aMissingExpectation }""")
        val expectation = expanded.expectation as SuccessExpectation
        assertTrue(expectation.expectedResult.isNullValue, "expectaion.expectedResult should be an ion null value")
        assertTrue(expectation.expectedResult.hasTypeAnnotation("missing"), "expected result should have 'missing' annotation")
    }

    /////////// TestCommand.alternateEnvironment
    @Test
    fun testCommandEnvironementFieldVariableReference() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', environment: { a_field: #aVariable } } """)
        val structSpec = expanded.environmentSpec as EnvironmentSpecStruct
        assertEquals(ion.singleValue("{ a_field: \"this is aVariable\" }"), structSpec.struct)
    }

    @Test
    fun testCommandEnvironementFileListStringInterpolation() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', environment:["some file here #{aVariable}"] } """)
        val structSpec = expanded.environmentSpec as EnvironmentSpecFiles
        assertEquals("some file here this is aVariable", structSpec.paths[0])
    }

    @Test
    fun testCommandEnvironementFieldIonInterpolation() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', environment: #aGeneralPurposeStruct } """)
        val structSpec = expanded.environmentSpec as EnvironmentSpecStruct
        assertEquals(ion.singleValue("{general_purpose_field: \"some value\"}"), structSpec.struct)
    }

    /////////// TestCommand.alternateCompileOptions
    @Test
    fun testCommandCompileOptionsFieldVariableReference() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', compile_options: { a_field: #aVariable } } """)
        assertEquals(ion.singleValue("{ a_field: \"this is aVariable\" }"), expanded.alternateCompileOptions)
    }

    @Test
    fun testCommandCompileOptionsFieldIonInterpolation() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', compile_options: #aGeneralPurposeStruct } """)
        assertEquals(ion.singleValue("{general_purpose_field: \"some value\"}"), expanded.alternateCompileOptions)
    }

    /////////// Miscellaneous
    @Test
    fun stringInterpolationUsingIonList() {
        val expanded = parseForExtractFirstExpandedTestCommand("""test::{ name: 'test', sql: "SELECT * FROM `#{aTable}`" }""")
        assertEquals("""SELECT * FROM `[{a:"b"},{a:"c"}]`""", expanded.sql.stringValue())
    }

    @Test
    fun multipleCommandsInTemplate() {
        val testScript = parseFor("""test::{ name: "test1", sql: #aVariable }, test::{ name: "test2", sql: #anotherVariable }""")
        val forReplacement = testScript.commands.first() as ScriptCommandList
        val expandedTemplate = forReplacement.commands.first() as ScriptCommandList
        val firstCommand = expandedTemplate.commands[0] as TestCommand
        assertEquals("this is aVariable", firstCommand.sql.stringValue())

        val secondCommand = expandedTemplate.commands[1] as TestCommand
        assertEquals("this is anotherVariable", secondCommand.sql.stringValue())

    }

    @Test
    fun multipleVariableSets() {
        val testScript = parseScript(
            """
                for::{
                    template: [
                        test::{ name: 'test', sql: #sql }
                    ],
                    variableSets: [ { sql: "first" }, { sql: "second" } ]
                }
            """
        )

        val forReplacement = testScript.commands.first() as ScriptCommandList
        val firstExpandedTemplate = forReplacement.commands[0] as ScriptCommandList
        val commandFromFirstExpandedTemplate = firstExpandedTemplate.commands[0] as TestCommand
        assertEquals("first", commandFromFirstExpandedTemplate.sql.stringValue())

        val secondExpandedTemplate = forReplacement.commands[1] as ScriptCommandList
        val commandFromSecondExpandedTemplate = secondExpandedTemplate.commands[0] as TestCommand
        assertEquals("second", commandFromSecondExpandedTemplate.sql.stringValue())
    }

    /////////// Commands other than test::{}
    @Test
    fun setDefaultEnvironmentCommandStringInterpoloation() {
        val expanded = parseFor(""" set_default_environment::"some file here #{aVariable}" """)
            .firstExpandedScriptCommand() as SetDefaultEnvironment

        val filesEnvSpec = expanded.envSpec as EnvironmentSpecFiles
        assertEquals("some file here this is aVariable", filesEnvSpec.paths[0])
    }

    @Test
    fun setDefaultEnvironmentCommandIonInterpoloation() {
        val expanded = parseFor(""" set_default_environment::{ some_field: #aVariable } """)
            .firstExpandedScriptCommand() as SetDefaultEnvironment
        val structSpec = expanded.envSpec as EnvironmentSpecStruct
        assertEquals(ion.singleValue("{ some_field: \"this is aVariable\" }"), structSpec.struct)
    }

    @Test
    fun setDefaultCompileOptionsIonInterpolation() {
        val expanded = parseFor(""" set_default_compile_options::{ some_field: #aVariable } """)
            .firstExpandedScriptCommand() as SetDefaultCompileOptions
        assertEquals(ion.singleValue("{ some_field: \"this is aVariable\" }"), expanded.options)
    }


    /////////// Undefined variables
    @Test
    fun undefinedVariableWithinStringInterpolation() {
        parseScript("""test::{ name: 'test', sql: "#{someUndefinedVariable}" }""")
    }

    @Test
    fun undefinedVariableWithIonInterpolation() {
        parseScript("""test::{ name: 'test', environment: { some_field: #someUndefinedVariable } }""")
    }

    @Test
    fun undefinedVariableRefOnTestEnvironmentProperty() {
        parseScript("""test::{ name: 'test', environment: #someUndefinedVariable }""")
    }

    @Test
    fun undefinedVariableRefOnTestCompileOptionsProperty() {
        parseScript("""test::{ name: 'test', compile_options: #someUndefinedVariable }""")
    }

    @Test
    fun undefinedVariableRefOnSetEnvironment() {
        parseScript("""set_default_environment:: #someUndefinedVariable """)
    }

    @Test
    fun undefinedVariableRefOnSetEnvironmentField() {
        parseScript("""set_default_environment:: { some_field: #someUndefinedVariable }""")
    }

    @Test
    fun undefinedVariableRefOnSetCompileOptionsField() {
        parseScript("""set_default_compile_options:: { someOption: #someUndefinedVariable }""")
    }

    @Test
    fun undefinedVariableRefOnSetCompileOptions() {
        parseScript("""set_default_compile_options:: #someUndefinedVariable """)
    }

    /////////// BenchmarkCommand
    @Test
    fun benchmarkSqlFieldStringInterpolation() {
        val expanded = parseFor("""benchmark::{ name: 'test', sql: "before #{aVariable} after" }""")
            .firstExpandedScriptCommand() as BenchmarkCommand
        assertEquals("before this is aVariable after", expanded.sql.stringValue())
    }

    @Test
    fun benchmarkSqlFieldVariableReference() {
        val expanded = parseFor("""benchmark::{ name: 'test', sql: #aVariable }""")
            .firstExpandedScriptCommand() as BenchmarkCommand
        assertEquals("this is aVariable", expanded.sql.stringValue())
    }

    @Test
    fun benchmarkExpectedCountVariableReference() {
        val expanded = parseFor("""benchmark::{ name: 'test', sql: 'something', expected_count: #count }""")
            .firstExpandedScriptCommand() as BenchmarkCommand
        val expectation = expanded.expectation as CountExpectation
        assertEquals(2, expectation.expectedResult)
    }

}