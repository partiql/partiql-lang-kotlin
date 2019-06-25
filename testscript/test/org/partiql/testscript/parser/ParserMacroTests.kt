package org.partiql.testscript.parser

import com.amazon.ion.IonSexp
import com.amazon.ion.IonStruct
import org.junit.jupiter.api.Test
import org.partiql.testscript.parser.ast.TestNode

class ParserMacroTests : BaseParseTests() {

    /*
     * We use `#` instead of `$` in test fixtures because escaping `$` in a kotlin
     * multiline string is messy, e.g. `"""${"$"}"""` results in `"$"`
     */
    
    @Test
    fun forWithSingleTestAndVariable() = assertParse("""
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
            |}""".trimMargin(),
            expected = singleModulesList(TestNode(id = "testTemplate\$\${value:1,expected:(success 2)}",
                    description = null,
                    statement = "1 + 1",
                    environment = null,
                    expected = BaseParseTests.ion.singleValue("(success 2)") as IonSexp,
                    scriptLocation = ScriptLocation(
                            "input[0]",
                            11))))

    @Test
    fun forWithMultipleTestsAndMultipleVariables() = assertParse("""
            |for::{ 
            |  template: [
            |    
            |    test::{
            |      id: testTemplate1,
            |      description: "test: #description",
            |      statement: "1 + #value",
            |      environment: {myTable:#table},
            |      expected: (success #result)
            |    },
            |    
            |    test::{
            |      id: testTemplate2,
            |      description: #description,
            |      statement: "#value",
            |      environment: #environment,
            |      expected: #expected
            |    }
            |  ],
            |  
            |  variable_sets: [
            |    { description: "description 1", value: 1, table: [1], result: 1, environment: {foo: 1}, expected: (success 10) },
            |    { description: "description 2", value: 2, table: [2], result: 2, environment: {foo: 2}, expected: (success 20) }
            |  ]
            |}""".trimMargin(),
            expected = singleModulesList(TestNode(id = "testTemplate1\$\${description:\"description 1\",value:1,table:[1],result:1,environment:{foo:1},expected:(success 10)}",
                    description = "test: description 1",
                    statement = "1 + 1",
                    environment = BaseParseTests.ion.singleValue(
                            "{myTable: [1]}") as IonStruct,
                    expected = BaseParseTests.ion.singleValue(
                            "(success 1)") as IonSexp,
                    scriptLocation = ScriptLocation(
                            "input[0]",
                            22)),

                    TestNode(id = "testTemplate1\$\${description:\"description 2\",value:2,table:[2],result:2,environment:{foo:2},expected:(success 20)}",
                            description = "test: description 2",
                            statement = "1 + 2",
                            environment = BaseParseTests.ion.singleValue(
                                    "{myTable: [2]}") as IonStruct,
                            expected = BaseParseTests.ion.singleValue(
                                    "(success 2)") as IonSexp,
                            scriptLocation = ScriptLocation(
                                    "input[0]",
                                    23)),

                    TestNode(id = "testTemplate2\$\${description:\"description 1\",value:1,table:[1],result:1,environment:{foo:1},expected:(success 10)}",
                            description = "description 1",
                            statement = "1",
                            environment = ion.singleValue(
                                    "{foo: 1}") as IonStruct,
                            expected = ion.singleValue(
                                    "(success 10)") as IonSexp,
                            scriptLocation = ScriptLocation(
                                    "input[0]",
                                    22)),

                    TestNode(id = "testTemplate2\$\${description:\"description 2\",value:2,table:[2],result:2,environment:{foo:2},expected:(success 20)}",
                            description = "description 2",
                            statement = "2",
                            environment = BaseParseTests.ion.singleValue(
                                    "{foo: 2}") as IonStruct,
                            expected = BaseParseTests.ion.singleValue(
                                    "(success 20)") as IonSexp,
                            scriptLocation = ScriptLocation(
                                    "input[0]",
                                    23))))

    @Test
    fun forWrongType() =
            assertParseError(input = """ for::"should be a struct" """,
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Wrong type for for. Expected STRUCT, got STRING
            """.trimMargin())

    @Test
    fun forWrongTemplateType() =
            assertParseError(input = """
                |for::{ 
                |  template: "should be a list",
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ] 
                |}
                |""".trimMargin(),
                    expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:2 - Wrong type for for.template. Expected LIST, got STRING
                """.trimMargin())

    @Test
    fun forEmptyTemplate() = assertParseError(input = """
                |for::{ 
                |  template: [],
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ] 
                |}  """.trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Field must have at least one element: for.template
                """.trimMargin())

    @Test
    fun forTestTemplateWrongType() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::"should be a struct"
                |  ],
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:3 - Wrong type for for.template[0]. Expected STRUCT, got STRING
                """.trimMargin())

    @Test
    fun forTestTemplateWrongIdType() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: "should be a symbol",
                |      statement: "1 + #value",
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:4 - Wrong type for for.template[0].id. Expected SYMBOL, got STRING
                """.trimMargin())

    @Test
    fun forTestTemplateMissingId() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      statement: "1 + #value",
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:3 - Missing required field: for.template[0].id
                """.trimMargin())

    @Test
    fun forTestTemplateWrongDescriptionType() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      description: 'should be a string',
                |      statement: "1 + #value",
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:5 - Invalid template value for field: for.template[0].description. Must start with '${'$'}' when it's a SYMBOL
                """.trimMargin())

    @Test
    fun forTestTemplateWrongStatementType() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: 'should be a string',
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:5 - Invalid template value for field: for.template[0].statement. Must start with '${'$'}' when it's a SYMBOL
                """.trimMargin())

    @Test
    fun forTestTemplateMissingStatement() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:3 - Missing required field: for.template[0].statement
                """.trimMargin())

    @Test
    fun forTestTemplateWrongEnvironmentType() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: "1 + #value",
                |      environment: "should be a struct",
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:6 - Wrong type for for.template[0].environment. Expected STRUCT, got STRING
                """.trimMargin())

    @Test
    fun forTestTemplateWrongExpectedType() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: "1 + #value",
                |      expected: 'should be a SEXP'
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:7 - Invalid template value for field: for.template[0].expected. Must start with '${'$'}' when it's a SYMBOL
                """.trimMargin())

    @Test
    fun forTestTemplateMissingExpected() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: "1 + #value"
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:3 - Missing required field: for.template[0].expected
                """.trimMargin())

    @Test
    fun forMissingTemplate() = assertParseError(input = """
                |for::{
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Missing required field: for.template
                """.trimMargin())

    @Test
    fun forWrongVariableSetType() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: "1 + #value",
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: "should be a list" 
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:10 - Wrong type for for.variable_sets. Expected LIST, got STRING
                """.trimMargin())

    @Test
    fun forMissingVariableSet() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: "1 + #value",
                |      expected: #expected
                |    }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Missing required field: for.variable_sets
                """.trimMargin())

    @Test
    fun forWrongVariableSetElementType() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: "1 + #value",
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    "should be a struct"
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:11 - Wrong type for variable_sets[0]. Expected STRUCT, got STRING
                """.trimMargin())

    @Test
    fun forEmptyVariableSet() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: "1 + #value",
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: []
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:1 - Field must have at least one element: for.variable_sets
                """.trimMargin())

    @Test
    fun forUnknownField() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: "1 + #value",
                |      expected: #expected
                |    }
                |  ],
                |  
                |  shouldNotBeHere: 1,
                |  
                |  variable_sets: [
                |    { value: 1, expected: (success 2) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:10 - Unexpected field: for.shouldNotBeHere
                """.trimMargin())

    @Test
    fun forUnknownVariable() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: "1 + #unknown",
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    { expected: (success 2) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:11 - Missing template variable: unknown
                """.trimMargin())

    @Test
    fun forInvalidExpectedVariable() = assertParseError(input = """
                |for::{ 
                |  template: [
                |    test::{
                |      id: test,
                |      statement: "1 + 1",
                |      expected: #expected
                |    }
                |  ],
                |  
                |  variable_sets: [
                |    { expected: (success) }
                |  ]
                |}
                |""".trimMargin(),
            expectedErrorMessage = """ 
                |Errors found when parsing test scripts:
                |    input[0]:11 - for.template.expected success must have two elements, e.g. (success (bag {a: 1}))
                """.trimMargin())


}