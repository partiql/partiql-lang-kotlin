# PartiQL Test Script (PTS) 

PartiQL Test Script (PTS) is a DSL to describe the expected behavior of a PartiQL implementation. It defines a `test` function that 
encapsulates: 
* a PartiQL statement
* an environment used to evaluate the statement
* the evaluation's expected result. 
 
All other functions defined in PTS are used to either reduce repetition, ignore a test, or add information 
necessary to execute a test..

A PTS file is an Ion document in which each top level value represents a PTS function or macro.

## Functions

### test

Represents a single PartiQL test, syntax:

```ion
test::{
    id: <symbol, required - test unique identifier>
    description: <string, optional - test description>,
    statement: <string, required - PartiQL statement>,
    environment: <struct, optional - PartiQL environment to evaluate the statement>,
    expected: <S-exp, required - expected result of the statement execution expressed as Ion>
}
```

`expected` is an S-expression where the first symbol indicates whether the statement is expected to complete 
successfully or produce an error:
* `(success <result>)`: statement is successful and <result> is the Ion representation of evaluating the statement
* `(error)`: statement results in an error

### set_default_environment

Changes the default environment for the current file. The initial default environment is the empty environment.

```ion
set_default_environment::<struct|string, required - new default environment>
```

When `set_default_environment` is a `string` it denotes the relative file path to an ion file that contains 
the environment.  

### for

A `for` function contains a `template` and `variable_set`. Each `test` in the template is performed once for 
each `variable_set` by substituting a `variable_set`'s name/value pairs into corresponding placeholders in 
the template.

```ion
for::{
    template: <list of tests, required - list of templated PTS tests to be interpolated>,
    variable_sets: <list of structs, required - list of variables used to interpolate the template>,
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
    variable_sets: [
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

id field is not expanded but auto generated based on the following rule: `<id>$$<variable_set_value>`. 

### skip_list

A skip list is used to skip tests. It's recommended that this list be defined per PartiQL implementation.

```ion
skip_list::[
    <string, required - zero or more strings, each containing a regular expression that matches one or more test ids>
]
```

### append_test

Appends a test with additional information. This should be used to provide additional metadata that a PartiQL 
implementation may need to run a test. For example, PartiQL implementation that requires DDL can use `append_test` 
to add a DDL statement that must be run before executing the test statement.

```ion
append_test::{
    pattern: <string, required - a pattern that matches one or more test ids>
    additional_data: <struct, required - struct containing all extra information>
}
```

Each test can only be appended **once** with the exception of skipped tests. Similar to `skip_list`, pattern is a 
regular expression. 

**Examples**:

For the example below we have two PartiQL implementations: the reference implementation and an implementation that 
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

## Expected Data model

We avoid using PartiQL syntax to specify the expected results and environment as it would create a dependency between 
PTS and a PartiQL implementation. 

Instead we use Ion since the PartiQL type system is based on the Ion type system. Most PartiQL types have 1:1 mapping 
into Ion types except for `MISSING` and `BAG`. To represent those we use the following convention:   

| PartiQL Value | Ion Value     |
|---------------|---------------|
| <<1,2,3>>     | (bag 1 2 3)   |
| missing       | missing::null |

Comparison is mostly deferred to Ion definition of equality. The exceptions, again, are `BAG`s and `MISSING` types: 
1. `BAG`s are unordered lists so two `BAG`s are equal if they have the same size and all elements on `BAG` A are contained 
in `BAG` B.
1. `MISSING` values are only considered equal if they both have the `MISSING` annotation.

## Interpreter

The PTS interpreter has three stages: parsing, compilation and evaluation.

### Parsing

The Parser parses PTS files using an Ion parsing library and generates an AST representing all scripts. 
Macros are expanded during the parsing phase and generate the corresponding expanded nodes.

TODO: Link code and API when they are merged

### Compilation

The Compiler compiles the AST into `TestExpression`s using the current compilation environment which is scoped to a 
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
official equality function that must be used by the evaluator when onboarding with PTS consensus testing framework.

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


