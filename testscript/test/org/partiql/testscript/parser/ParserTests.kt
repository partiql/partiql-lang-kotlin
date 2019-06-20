package org.partiql.testscript.parser

import com.amazon.ion.*
import org.junit.jupiter.api.*
import org.partiql.testscript.parser.ast.*
import java.nio.charset.Charset

class ParserTests : BaseParseTests() {

    @Test
    fun singleFullTest() = assertParse("""
            |test::{
            |  id: test_1,
            |  description: "a test",
            |  statement: "SELECT * FROM myTable",
            |  environment: {myTable: [{a: 1}]},
            |  expected: (success (bag {a: 1}))
            |}""".trimMargin(),
            expected = singleModulesList(TestNode(id = "test_1",
                    description = "a test",
                    statement = "SELECT * FROM myTable",
                    environment = ion.singleValue("{myTable: [{a: 1}]}") as IonStruct,
                    expected = ion.singleValue("(success (bag {a: 1}))") as IonSexp,
                    scriptLocation = ScriptLocation("input[0]",
                            1))))

    @Test
    fun singleTestWithRequiredOnly() = assertParse("""
            |test::{
            |  id: test_1,
            |  statement: "SELECT * FROM myTable",
            |  expected: (success (bag {a: 1}))
            |}""".trimMargin(),
            expected = singleModulesList(TestNode(id = "test_1",
                    description = null,
                    statement = "SELECT * FROM myTable",
                    environment = null,
                    expected = ion.singleValue("(success (bag {a: 1}))") as IonSexp,
                    scriptLocation = ScriptLocation(
                            "input[0]",
                            1))))

    @Test
    fun singleSetDefaultEnvironmentNode() = assertParse("set_default_environment::{ foo: [1,2,3,4,5] }",
            expected = singleModulesList(
                    SetDefaultEnvironmentNode(ion.singleValue("{ foo: [1,2,3,4,5] }") as IonStruct,
                            ScriptLocation("input[0]", 1))))

    @Test
    fun singleSkipList() = assertParse("""skip_list::[ "test_1", "test_2" ]""",
            expected = singleModulesList(SkipListNode(listOf("test_1", "test_2"),
                    ScriptLocation("input[0]", 1))))

    @Test
    fun singleAppendTest() = assertParse("""
            |append_test::{ 
            |  pattern: "test.*",
            |  additional_data: { foo: 1, bar: {} }
            |}""".trimMargin(),
            expected = singleModulesList(AppendTestNode(pattern = "test.*",
                    additionalData = ion.singleValue("{ foo: 1, bar: {} }") as IonStruct,
                    scriptLocation = ScriptLocation("input[0]",
                            1))))

    @Test
    fun allFunctions() = assertParse("""
        |set_default_environment::{ foo: [1,2,3,4,5] }
        |
        |test::{
        |  id: test_1,
        |  statement: "SELECT * FROM myTable",
        |  expected: (success (bag {a: 1}))
        |}
        |
        |skip_list::[ "test_1" ]
        |
        |append_test::{ 
        |  pattern: "test.*",
        |  additional_data: { foo: 1, bar: {} }
        |}
        |
        |for::{ 
        |  template: [
        |    test::{
        |      id: testTemplate,
        |      statement: "1 + #value",
        |      expected: #expected
        |    }
        |  ],
        |  
        |  variable_sets: [
        |    { value: 1, expected: (success 2) }
        |  ]
        |}
        """.trimMargin(),
            expected = singleModulesList(SetDefaultEnvironmentNode(ion.singleValue("{ foo: [1,2,3,4,5] }") as IonStruct,
                    ScriptLocation("input[0]",
                            1)),

                    TestNode(id = "test_1",
                            description = null,
                            statement = "SELECT * FROM myTable",
                            environment = null,
                            expected = ion.singleValue("(success (bag {a: 1}))") as IonSexp,
                            scriptLocation = ScriptLocation("input[0]",
                                    3)),

                    SkipListNode(listOf("test_1"),
                            ScriptLocation("input[0]", 9)),

                    AppendTestNode(pattern = "test.*",
                            additionalData = ion.singleValue("{ foo: 1, bar: {} }") as IonStruct,
                            scriptLocation = ScriptLocation("input[0]",
                                    11)),

                    TestNode(id = "testTemplate\$\${value:1,expected:(success 2)}",
                            description = null,
                            statement = "1 + 1",
                            environment = null,
                            expected = ion.singleValue("(success 2)") as IonSexp,
                            scriptLocation = ScriptLocation("input[0]",
                                    26))))

    @Test
    fun multipleDocuments() = assertParse("set_default_environment::{ foo: [1] }",
            "set_default_environment::{ foo: [2] }",
            expected = listOf(ModuleNode(listOf(SetDefaultEnvironmentNode(
                    ion.singleValue("{ foo: [1] }") as IonStruct,
                    ScriptLocation("input[0]", 1))),
                    ScriptLocation("input[0]", 0)),

                    ModuleNode(listOf(SetDefaultEnvironmentNode(ion.singleValue(
                            "{ foo: [2] }") as IonStruct,
                            ScriptLocation("input[1]", 1))),
                            ScriptLocation("input[1]", 0))))

    @Test
    fun testMissingId() =
            assertParseError(input = """
                |test::{
                |   description: "a test",
                |   statement: "SELECT * FROM myTable",
                |   environment: {myTable: [{a: 1}]},
                |   expected: (success (bag {a: 1}))
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Missing required field: test.id
            """.trimMargin())

    @Test
    fun testMissingStatement() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: "a test",
                |   environment: {myTable: [{a: 1}]},
                |   expected: (success (bag {a: 1}))
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Missing required field: test.statement
            """.trimMargin())

    @Test
    fun testMissingExpected() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: "a test",
                |   statement: "SELECT * FROM myTable",
                |   environment: {myTable: [{a: 1}]},
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Missing required field: test.expected
            """.trimMargin())

    @Test
    fun testMissingAllRequired() =
            assertParseError(input = """
                |test::{
                |   description: "a test",
                |   environment: {myTable: [{a: 1}]},
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Missing required field: test.expected
                |    input[0]:1 - Missing required field: test.id
                |    input[0]:1 - Missing required field: test.statement
            """.trimMargin())

    @Test
    fun testWrongType() =
            assertParseError(input = """test::"should be a struct" """,
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Wrong type for test. Expected STRUCT, got STRING
            """.trimMargin())

    @Test
    fun testWrongIdType() =
            assertParseError(input = """
                |test::{
                |   id: "should be symbol",
                |   description: "a test",
                |   statement: "SELECT * FROM myTable",
                |   environment: {myTable: [{a: 1}]},
                |   expected: (success (bag {a: 1}))
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:2 - Wrong type for test.id. Expected SYMBOL, got STRING
            """.trimMargin())

    @Test
    fun testWrongDescriptionType() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: should_be_text,
                |   statement: "SELECT * FROM myTable",
                |   environment: {myTable: [{a: 1}]},
                |   expected: (success (bag {a: 1}))
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:3 - Wrong type for test.description. Expected STRING, got SYMBOL
            """.trimMargin())

    @Test
    fun testWrongStatementType() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: "a test",
                |   statement: should_be_string,
                |   environment: {myTable: [{a: 1}]},
                |   expected: (success (bag {a: 1}))
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:4 - Wrong type for test.statement. Expected STRING, got SYMBOL
            """.trimMargin())

    @Test
    fun testWrongEnvironmentType() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: "a test",
                |   statement: "SELECT * FROM myTable",
                |   environment: "should be a struct",
                |   expected: (success (bag {a: 1}))
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:5 - Wrong type for test.environment. Expected STRUCT, got STRING
            """.trimMargin())

    @Test
    fun testExpectedWrongType() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: "a test",
                |   statement: "SELECT * FROM myTable",
                |   environment: {myTable: [{a: 1}]},
                |   expected: "should be a s-exp"
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:6 - Wrong type for test.expected. Expected SEXP, got STRING
            """.trimMargin())

    @Test
    fun testUnknownField() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: "a test",
                |   statement: "SELECT * FROM myTable",
                |   environment: {myTable: [{a: 1}]},
                |   expected: (success (bag {a: 1})),
                |   shouldNotBeHere: 1
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:7 - Unexpected field: test.shouldNotBeHere
            """.trimMargin())

    @Test
    fun testWrongExpectedValue() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: "a test",
                |   statement: "SELECT * FROM myTable",
                |   environment: {myTable: [{a: 1}]},
                |   expected: (invalid)
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:6 - Invalid test.expected tag, must be either 'success' or 'error' got 'invalid'
            """.trimMargin())

    @Test
    fun testExpectedErrorWithElements() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: "a test",
                |   statement: "SELECT * FROM myTable",
                |   environment: {myTable: [{a: 1}]},
                |   expected: (error a)
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:6 - test.expected error can only have a single element, e.g. (error)
            """.trimMargin())

    @Test
    fun testExpectedSuccessEmpty() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: "a test",
                |   statement: "SELECT * FROM myTable",
                |   environment: {myTable: [{a: 1}]},
                |   expected: (success)
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:6 - test.expected success must have two elements, e.g. (success (bag {a: 1}))
            """.trimMargin())

    @Test
    fun testExpectedSuccessWithTooManyElements() =
            assertParseError(input = """
                |test::{
                |   id: test_1,
                |   description: "a test",
                |   statement: "SELECT * FROM myTable",
                |   environment: {myTable: [{a: 1}]},
                |   expected: (success a b)
                |}
            """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:6 - test.expected success must have two elements, e.g. (success (bag {a: 1}))
            """.trimMargin())

    @Test
    fun setDefaultEnvironmentWrongType() =
            assertParseError(input = """ set_default_environment::"should be a struct" """,
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Wrong type for set_default_environment. Expected STRUCT, got STRING
            """.trimMargin())

    @Test
    fun skipListWrongType() =
            assertParseError(input = """ skip_list::"should be a list" """,
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Wrong type for skip_list. Expected LIST, got STRING
            """.trimMargin())

    @Test
    fun skipListWrongElementType() =
            assertParseError(input = """ skip_list::["test_1", 'should be a string'] """,
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Wrong type for skip_list[1]. Expected STRING, got SYMBOL
            """.trimMargin())

    @Test
    fun appendTestWrongType() =
            assertParseError(input = """ append_test::"should be a struct" """,
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Wrong type for append_test. Expected STRUCT, got STRING
            """.trimMargin())

    @Test
    fun appendTestMissingPattern() =
            assertParseError(input = """
                |append_test::{
                |  additional_data: {}
                |} """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Missing required field: append_test.pattern
            """.trimMargin())

    @Test
    fun appendTestMissingAdditionalData() =
            assertParseError(input = """
                |append_test::{
                |  pattern: "test",
                |} """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Missing required field: append_test.additional_data
            """.trimMargin())

    @Test
    fun appendTestWrongPatternType() =
            assertParseError(input = """
                |append_test::{
                |  pattern: 'should be a string',
                |  additional_data: {}
                |} """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:2 - Wrong type for append_test.pattern. Expected STRING, got SYMBOL
            """.trimMargin())

    @Test
    fun appendTestWrongAdditionalDataType() =
            assertParseError(input = """
                |append_test::{
                |  pattern: "test",
                |  additional_data: "should be a struct"
                |} """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:3 - Wrong type for append_test.additional_data. Expected STRUCT, got STRING
            """.trimMargin())

    @Test
    fun appendTestUnknownField() =
            assertParseError(input = """
                |append_test::{
                |  pattern: "test",
                |  additional_data: {},
                |  shouldNotBeHere: 1
                |} """.trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:4 - Unexpected field: append_test.shouldNotBeHere
            """.trimMargin())

}