# PartiQL Test Script (PTS) 

PTS is a DSL to describe the expected behavior of a PartiQL implementation. It defines a core `test` function that 
encapsulates: 
* a PartiQL statement
* an environment used to evaluate the statement
* the evaluation's expected result. 
 
All other functions defined in PTS are used to either reduce repetition or modify a test by ignoring it or appending 
additional metadata necessary to execute the test

A PTS file is an Ion document where each top level value represents a PTS function or macro.

## Functions

### test

Represents a single PartiQL example, syntax:

```ion
test::{
    id: (Symbol, required - test unique identifier)
    description: (String, optional - test description),
    statement: (String, required - PartiQL statement),
    environment: (Struct, optional - PartiQL environment to evaluate the statement.),
    expected: (S-exp, required - expected result of the statement execution express as Ion)
}
```

Expected is an s-expression where the first symbol determines what type of result is expected:

* `(success <result>)`: statement is successful and <result> is the ion representation of evaluating the PartiQL 
statement
* `(error)`: statement results in an error

### set_default_environment

Changes the default environment for the current file. The initial default environment is the empty environment.

```ion
set_default_environment::(Struct, required - new default environment)
```

### for

A macro that expands into other functions by interpolating the template with variables in the variable set.

```ion
for::{
    template: (List of tests, required - list of templated PTS functions to be interpolated),
    variableSets: (List of structs, required - list of variables used to interpolate the template),
}
```

Example:

```ion
// the macro
for::{
    template: [
        test::{
            id: '1+value',
            statement: "1 + $value",
            expected: (success $expected)
        }
    ],
    variableSets: [
        { value: 1, expected: 2 },
        { value: 10, expected: 11 },
    ]
}

// Expands to
test::{
    id: '1+value$${value:1,expected:2}',
    statement: "1 + 1",
    expected: (success 2)
}

test::{
    id: '1+value$${value:10,expected:11}',
    statement: "1 + 10",
    expected: (success 11)
}
```

id field is not expanded but auto generated based on the following rule: `<base_id>$$<variable_set_value>`. 

### skip_list

A skip list is used to skip tests. It's recommended that this list be defined per PartiQL implementation.

```ion
skip_list::[
    (String, required - a pattern that matches one or more test ids)
]
```

Pattern is a regular expression.

### append_test

Appends a test with additional information. This should be used to provide additional metadata that a PartiQL 
implementation may need to run a test. For example a DDL dependent PartiQL implementation can use append_test to add a 
DDL statement that must be ran before running the test statement.

```ion
append_test::{
    pattern: (String, required - a pattern that matches one or more test ids)
    additional_data: (Struct, required - struct containing all extra information)
}
```

Each test can only be appended **once** with the exception of skipped tests. Similar to `skip_list`, pattern is a 
regular expression. 

**Examples**:

For the example bellow we have two PartiQL implementations: the reference implementation and an implementation that 
requires a DDL. 

Core PTS

```ion
test::{
    id: select_star,
    environment: { myTable: [{v: 1}, {v: 2}] },
    statement: "SELECT * FROM myTable t",
    expected: (success (bag {v: 1} {v: 2}))
}

test::{
    id: select_path,
    environment: { myTable: [{a: {b: {c: 1}}}] },
    statement: "SELECT t.a.b.c as c FROM myTable t",
    expected: (success (bag {c: 1}))
}

test::{
    id: order_by,
    environment: { myTable: [{v: 10}, {v: 2}] },
    statement: "SELECT * FROM myTable t ORDER BY v",
    expected: (success [{v: 2}, {v: 10}])
}
```

Reference implementation PTS

```ion
skip_list::[
    "order_by" // not implemented yet 
]
```

DDL dependent implementation PTS

```ion
skip_list::[
    "order_by",     // not implemented yet 
    "select_path"   // not supported 
]

append_test::{
    pattern: "select_star"
    additional_data: {
        ddl: "CREATE TABLE myTable (v INT)",
        ...
    }
}
```

## Interpreter

The PTS interpreter has three stages: parsing, compilation and evaluation.

### Parsing

The Parser parses PTS files using an ion parsing library and generates an AST representing all scripts. 
Macros are expanded during the parsing phase and generate the corresponding expanded nodes.

TODO: Link code and API when they are merged

### Compilation

The Compiler is to compile the AST into `TestExpression`s using the current compilation environment which is scoped to a 
PTS file script, i.e. the compilation environment resets at the beginning of each file.

A `TestExpression` represents the execution of a single test.

AST nodes fall into one of three categories:

1. Compilation Environment mutators: change the current compilation environment, e.g. `set_default_environment`
2. Tests: define a test that can be compiled into a `TestExpression` with the current compilation environment, e.g.
`test` and `for` macros 
3. `TestExpression` decorators. Decorates a `TestExpression` changing how it should be evaluated, e.g. `skip_list` and 
`append_test`

TODO: Link code and API when they are merged

### Evaluation

Evaluator evaluates `TestExpressions` into `TestResults` which represent the test success or failure. Each PartiQL 
implementation must provide it's own Evaluator implementation.

The evaluator interface allows an equality function to be provided to the evaluator implementation. PTS provides an 
official equality function that must be used by the evaluator when onboarding with PTS consensus testing framework

Proposed Interfaces: TODO Link code and API when they are merged https://github.com/partiql/partiql-lang-kotlin/issues/87 

```kotlin
sealed class ExpectedResult 

class ExpectedSuccess : ExpectedResult {
    val expected: IonValue;
}

object ExpectedError : ExpectedResult 

interface Equality<T> {
    fun isEqual(expected: Expectation, actual: T): Boolean;
}

abstract class Evaluator<T>(val equality: Equality<T>) {
    abstract fun evaluate(testExpressions: List<TestExpression>): 
        List<TestResult>;
}
```

## Expected Data model

We avoid using PartiQL syntax to specify the expected results and environment as it would create a dependency between 
PTS and a PartiQL implementation. 

Instead we use Ion since PartiQL type system is based on the its type system. Most PartiQL types have 1:1 mapping into
and Ion type apart from `MISSING` and `BAG`. To represent those we use the following convention:   

| PartiQL Value | Ion Value     |
|---------------|---------------|
| <<1,2,3>>     | (bag 1 2 3)   |
| missing       | missing::null |

Comparison is mostly deferred to Ion definition of equality. The exceptions, again, are bags and missing types: 
1. Bags are unordered lists so two bags are equal if they have the same size and all elements on bag A are contained 
in bag B.
1. For missing values it is only considered equal if the both have the missing annotation.

