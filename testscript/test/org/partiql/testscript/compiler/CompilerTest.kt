package org.partiql.testscript.compiler

import com.amazon.ion.IonSexp
import com.amazon.ion.IonStruct
import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.testscript.parser.*

class CompilerTest {
    private val ion = IonSystemBuilder.standard().build()
    private val emptyStruct = ion.newEmptyStruct().apply { makeReadOnly() }

    private val parser = Parser(ion)
    private val compiler = Compiler(ion)

    private fun String.toSexp() = ion.singleValue(this) as IonSexp
    private fun String.toStruct() = ion.singleValue(this) as IonStruct

    private fun assertCompile(vararg scripts: String, expected: List<TestScriptExpression>) {
        val inputs = createInput(*scripts)
        val ast = parser.parse(inputs)

        // sort both by id to ignore order and make error messages easier to read 
        val sortedActual = compiler.compile(ast).sortedBy { it.id }
        val sortedExpected = expected.sortedBy { it.id }

        assertEquals(sortedExpected, sortedActual) {
            "Expected size: ${sortedExpected.size}, Actual size: ${sortedActual.size}"
        }
    }

    private fun assertCompileError(input: String, expectedErrorMessage: String) {
        val inputs = createInput(input)
        val ast = parser.parse(inputs)

        val exception = assertThrows<CompilerException> { compiler.compile(ast) }
        assertEquals(expectedErrorMessage, exception.message)
    }

    @Test
    fun singleTest() = assertCompile("""
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expected = listOf(
                    TestExpression(
                            id = "test1",
                            description = null,
                            statement = "SELECT * FROM <<1,2,3>>",
                            environment = ion.newEmptyStruct(),
                            expected = "(success 1)".toSexp(),
                            scriptLocation = ScriptLocation("input[0]", 1)
                    )))

    @Test
    fun multipleTests() = assertCompile("""
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
                |
                |test::{
                |   id: test2,
                |   description: "second test",
                |   statement: "SELECT * FROM {}",
                |   expected: (success 2)
                |}
            """.trimMargin(),
            expected = listOf(
                    TestExpression(
                            id = "test1",
                            description = null,
                            statement = "SELECT * FROM <<1,2,3>>",
                            environment = ion.newEmptyStruct(),
                            expected = "(success 1)".toSexp(),
                            scriptLocation = ScriptLocation("input[0]", 1)),
                    TestExpression(
                            id = "test2",
                            description = "second test",
                            statement = "SELECT * FROM {}",
                            environment = ion.newEmptyStruct(),
                            expected = "(success 2)".toSexp(),
                            scriptLocation = ScriptLocation("input[0]", 7)
                    )))

    @Test
    fun multipleTestsWithSameId() = assertCompileError("""
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM {}",
                |   expected: (success 1)
                |}
                |
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM {}",
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expectedErrorMessage = """
                |Errors found when compiling test scripts:
                |    input[0]:7 - testId: test1 not unique also found in: input[0]:1
            """.trimMargin())

    @Test
    fun singleTestWithEnvironment() = assertCompile("""
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   environment: {a: 12},
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expected = listOf(
                    TestExpression(
                            id = "test1",
                            description = null,
                            statement = "SELECT * FROM <<1,2,3>>",
                            environment = "{a: 12}".toStruct(),
                            expected = "(success 1)".toSexp(),
                            scriptLocation = ScriptLocation("input[0]", 1)
                    )))

    @Test
    fun setDefaultThenTest() = assertCompile("""
                |set_default_environment::{a: 12}
                |       
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expected = listOf(
                    TestExpression(
                            id = "test1",
                            description = null,
                            statement = "SELECT * FROM <<1,2,3>>",
                            environment = "{a: 12}".toStruct(),
                            expected = "(success 1)".toSexp(),
                            scriptLocation = ScriptLocation("input[0]", 3)
                    )))

    @Test
    fun setDefaultOnlyAffectsSubsequentTests() = assertCompile("""
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
                |
                |set_default_environment::{a: 12}
                |       
                |test::{
                |   id: test2,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expected = listOf(
                    TestExpression(
                            id = "test1",
                            description = null,
                            statement = "SELECT * FROM <<1,2,3>>",
                            environment = emptyStruct,
                            expected = "(success 1)".toSexp(),
                            scriptLocation = ScriptLocation("input[0]", 1)
                    ),
                    TestExpression(
                            id = "test2",
                            description = null,
                            statement = "SELECT * FROM <<1,2,3>>",
                            environment = "{a: 12}".toStruct(),
                            expected = "(success 1)".toSexp(),
                            scriptLocation = ScriptLocation("input[0]", 9)
                    )))

    @Test
    fun setDefaultResetsForNextModule() = assertCompile("""
                |set_default_environment::{a: 12}
                |       
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
            """.trimMargin(),

            """
                |test::{
                |   id: test2,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
            """.trimMargin(),

            expected = listOf(
                    TestExpression(
                            id = "test1",
                            description = null,
                            statement = "SELECT * FROM <<1,2,3>>",
                            environment = "{a: 12}".toStruct(),
                            expected = "(success 1)".toSexp(),
                            scriptLocation = ScriptLocation("input[0]", 3)
                    ),
                    TestExpression(
                            id = "test2",
                            description = null,
                            statement = "SELECT * FROM <<1,2,3>>",
                            environment = emptyStruct,
                            expected = "(success 1)".toSexp(),
                            scriptLocation = ScriptLocation("input[1]", 1)
                    )))

    @Test
    fun testEnvironmentWinsOverDefault() = assertCompile("""
                |set_default_environment::{a: 12}
                |       
                |test::{
                |   id: test1,
                |   environment: {foo: 20},
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expected = listOf(
                    TestExpression(
                            id = "test1",
                            description = null,
                            statement = "SELECT * FROM <<1,2,3>>",
                            environment = "{foo: 20}".toStruct(),
                            expected = "(success 1)".toSexp(),
                            scriptLocation = ScriptLocation("input[0]", 3)
                    )))

    @Test
    fun skipList() = assertCompile("""
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
                |
                |skip_list::["test1"]
            """.trimMargin(),
            expected = listOf(
                    SkippedTestExpression(
                            id = "test1",
                            original = TestExpression(
                                    id = "test1",
                                    description = null,
                                    statement = "SELECT * FROM <<1,2,3>>",
                                    environment = emptyStruct,
                                    expected = "(success 1)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 1)
                            ),
                            scriptLocation = ScriptLocation("input[0]", 7))))

    @Test
    fun skipListMultipleTestsSkipSingle() = assertCompile("""
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
                |
                |test::{
                |   id: test2,
                |   statement: "SELECT * FROM <<1,2,3>>",
                |   expected: (success 1)
                |}
                |
                |skip_list::["test1"]
            """.trimMargin(),
            expected = listOf(
                    TestExpression(
                            id = "test2",
                            description = null,
                            statement = "SELECT * FROM <<1,2,3>>",
                            environment = emptyStruct,
                            expected = "(success 1)".toSexp(),
                            scriptLocation = ScriptLocation("input[0]", 7)),

                    SkippedTestExpression(
                            id = "test1",
                            original = TestExpression(
                                    id = "test1",
                                    description = null,
                                    statement = "SELECT * FROM <<1,2,3>>",
                                    environment = emptyStruct,
                                    expected = "(success 1)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 1)
                            ),
                            scriptLocation = ScriptLocation("input[0]", 13))))

    @Test
    fun skipListMultipleTestsSkipAll() = assertCompile("""
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM 1",
                |   expected: (success 1)
                |}
                |
                |test::{
                |   id: test2,
                |   statement: "SELECT * FROM 2",
                |   expected: (success 2)
                |}
                |
                |skip_list::[".*"]
            """.trimMargin(),
            expected = listOf(
                    SkippedTestExpression(
                            id = "test1",
                            original = TestExpression(
                                    id = "test1",
                                    description = null,
                                    statement = "SELECT * FROM 1",
                                    environment = emptyStruct,
                                    expected = "(success 1)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 1)),
                            scriptLocation = ScriptLocation("input[0]", 13)),

                    SkippedTestExpression(
                            id = "test2",
                            original = TestExpression(
                                    id = "test2",
                                    description = null,
                                    statement = "SELECT * FROM 2",
                                    environment = emptyStruct,
                                    expected = "(success 2)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 7)
                            ),
                            scriptLocation = ScriptLocation("input[0]", 13))))

    @Test
    fun skipListBeforeTest() = assertCompile("""
                |skip_list::[".*"]
                |
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM 1",
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expected = listOf(
                    SkippedTestExpression(
                            id = "test1",
                            original = TestExpression(
                                    id = "test1",
                                    description = null,
                                    statement = "SELECT * FROM 1",
                                    environment = emptyStruct,
                                    expected = "(success 1)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 3)),
                            scriptLocation = ScriptLocation("input[0]", 1))))

    @Test
    fun skipListSameTestMultipleTimes() = assertCompile("""
                |skip_list::[".*", "test1", "test.*"]
                |
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM 1",
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expected = listOf(
                    SkippedTestExpression(
                            id = "test1",
                            original = TestExpression(
                                    id = "test1",
                                    description = null,
                                    statement = "SELECT * FROM 1",
                                    environment = emptyStruct,
                                    expected = "(success 1)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 3)),
                            scriptLocation = ScriptLocation("input[0]", 1))))


    @Test
    fun appendTest() = assertCompile("""
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM 1",
                |   expected: (success 1)
                |}
                |
                |append_test::{
                |   pattern: "test1",
                |   additional_data: { foo: bar }
                |}
            """.trimMargin(),
            expected = listOf(
                    AppendedTestExpression(
                            id = "test1",
                            additionalData = "{ foo: bar }".toStruct(),
                            original = TestExpression(
                                    id = "test1",
                                    description = null,
                                    statement = "SELECT * FROM 1",
                                    environment = emptyStruct,
                                    expected = "(success 1)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 1)),
                            scriptLocation = ScriptLocation("input[0]", 7))))

    @Test
    fun appendTestBeforeTest() = assertCompile("""
                |append_test::{
                |   pattern: "test1",
                |   additional_data: { foo: bar }
                |}
                |
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM 1",
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expected = listOf(
                    AppendedTestExpression(
                            id = "test1",
                            additionalData = "{ foo: bar }".toStruct(),
                            original = TestExpression(
                                    id = "test1",
                                    description = null,
                                    statement = "SELECT * FROM 1",
                                    environment = emptyStruct,
                                    expected = "(success 1)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 6)),
                            scriptLocation = ScriptLocation("input[0]", 1))))

    @Test
    fun appendAndSkippedTest() = assertCompile("""
                |skip_list::["test1"]
                |
                |append_test::{
                |   pattern: "test1",
                |   additional_data: { foo: bar }
                |}
                |
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM 1",
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expected = listOf(
                    SkippedTestExpression(
                            id = "test1",
                            original = TestExpression(
                                    id = "test1",
                                    description = null,
                                    statement = "SELECT * FROM 1",
                                    environment = emptyStruct,
                                    expected = "(success 1)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 8)),
                            scriptLocation = ScriptLocation("input[0]", 1))))

    @Test
    fun appendSkippedTestMultipleTimes() = assertCompile("""
                |skip_list::["test1"]
                |
                |append_test::{
                |   pattern: "test1",
                |   additional_data: { foo: bar }
                |}
                |
                |append_test::{
                |   pattern: "test1",
                |   additional_data: { foo: bar }
                |}
                |
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM 1",
                |   expected: (success 1)
                |}
            """.trimMargin(),
            expected = listOf(
                    SkippedTestExpression(
                            id = "test1",
                            original = TestExpression(
                                    id = "test1",
                                    description = null,
                                    statement = "SELECT * FROM 1",
                                    environment = emptyStruct,
                                    expected = "(success 1)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 13)),
                            scriptLocation = ScriptLocation("input[0]", 1))))

    @Test
    fun appendTestNoTestMatches() = assertCompileError("""
                |append_test::{
                |   pattern: "test1",
                |   additional_data: { foo: bar }
                |}
            """.trimMargin(),
            expectedErrorMessage = """
                |Errors found when compiling test scripts:
                |    input[0]:1 - No testId matched the pattern: test1
            """.trimMargin())

    @Test
    fun appendTestMoreThanOneTestMatches() = assertCompile("""
                |append_test::{
                |   pattern: ".*",
                |   additional_data: { foo: bar }
                |}
                |
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM 1",
                |   expected: (success 1)
                |}
                |
                |test::{
                |   id: test2,
                |   statement: "SELECT * FROM 2",
                |   expected: (success 2)
                |}
            """.trimMargin(),
            expected = listOf(
                    AppendedTestExpression(
                            id = "test1",
                            additionalData = "{ foo: bar }".toStruct(),
                            original = TestExpression(
                                    id = "test1",
                                    description = null,
                                    statement = "SELECT * FROM 1",
                                    environment = emptyStruct,
                                    expected = "(success 1)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 6)),
                            scriptLocation = ScriptLocation("input[0]", 1)),
                    AppendedTestExpression(
                            id = "test2",
                            additionalData = "{ foo: bar }".toStruct(),
                            original = TestExpression(
                                    id = "test2",
                                    description = null,
                                    statement = "SELECT * FROM 2",
                                    environment = emptyStruct,
                                    expected = "(success 2)".toSexp(),
                                    scriptLocation = ScriptLocation("input[0]", 12)),
                            scriptLocation = ScriptLocation("input[0]", 1))))

    @Test
    fun appendAppendedTest() = assertCompileError("""
                |test::{
                |   id: test1,
                |   statement: "SELECT * FROM 1",
                |   expected: (success 1)
                |}
                |
                |append_test::{
                |   pattern: "test1",
                |   additional_data: { foo: bar }
                |}
                |
                |append_test::{
                |   pattern: "test1",
                |   additional_data: { foo: baz }
                |}
            """.trimMargin(),
            expectedErrorMessage = """
                |Errors found when compiling test scripts:
                |    input[0]:12 - testId: test1 was already appended on input[0]:7
            """.trimMargin())
}