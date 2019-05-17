
# Test Script Structure

## Terms

- `driver` A program which executes `commands` in a `test script` and sends `commands` to a `car` to execute the tests 
contained within.
- `car` A program with an embedded implementation of \SqlName that accepts `commands` from the `driver` for the purpose
of testing \SqlName.
- `test script` A file containing zero or more `commands`.
- `command` An element of a `test script` which specifies an action to be taken such as execute SQL and verify the 
result, or set defaults for `compile options` and `environment`.  `Commands` do not exist outside of `test scripts`.
- `command validation rule` A rule used by the `driver` to determine if a `command` is valid and can be executed.
    - If any `command` in a `test script` fails validation, the `driver` reports an error and no tests are executed.
- `test` A command that specifies:
    - \SqlName query to be executed
    - A snippet of Ion which is the expected result which may be:
        - Ion
        - An error code and expected property value (line, column, etc).
- `environment` A collection of global variables (Ion primitives, scalar, list, bag, etc) to be made available to the 
SQL of each test.
- `default environment` An element of each `test script`'s runtime state which is an environment to be used by tests 
that do not explicitly specify an environment.
- `compile options` A collection of key/value pairs specifying options that affect how \SqlName is compiled.
- `default compile options` An element of a test script's runtime state which specifies the `compile options` that are 
used by tests that do not explicitly define their compile options. 
- `session` A collection of key/value pairs used for the interpreter's evaluation session, specifying values such as 
the Ion `timestamp` to use for the `utcnow()` function.
- `default session` An element of each `test script`'s runtime state which species the `session` to be used by `test` 
commands that do not explicitly specify a `sesssion`.
 
## Driver

Is a program that:

- Reads test scripts
- Send requests to the car to execute the script commands, e.g.: set its environment, execute SQL, run benchmark
- The driver will execute commands in the order specified in the test script file.
- Retrieves results from each test execution and verifies that the results match the expected values.
- Retrieves results from each benchmark execution and outputs it's result
- The driver will only have to be implemented once and can be used with different car implementations.
- The driver must verify that each test script it loads has no duplicate test names so confusion can be avoided when 
failures are reported.

## Car

Is a program that:

- Is implementation specific, i.e. one will be implemented for each \SqlName implementation.
- Accepts commands from the driver and executes them for a given \SqlName implementation.
- Returns the commands results or error details to the driver for verification.

## Test Scripts

### Test Script Structure

At the outermost level, a test script contains a series of Ion structs.

```
    <command1> 
    <command2>
    ...
```

See the "Test Script Commands" section below for a list of available commands.

### Test Script Execution

The commands in a test script are executed in order, from top to bottom.

At runtime, test scripts do not have a notion of a "scope".  They have mutable global state consisting of:

| Global State Element | Command to Change | Defaults |
-----------------------|----------------|----------------|
| default environment | `set_default_environment` | An empty environment. |
| default compile options | `set_default_compile_options` | The \SqlName implementation's default compile options. |
| session | `set_default_session` | The \SqlName implementation's default session values, except `utcnow` must be `2000-01-01T00:00:00+00:00` |

Before beginning execution of a test script, the driver must set the global runtime state to the above 
defaults.  Once changed with any `set_*` command, new global state will be present during execution of any test 
commands in the same test script (or test script included with the include command) until another `set_*` command 
is encountered or test script execution has completed.

Note that the `include` command includes the commands of another test script in the current test script.  This is *not*
a nested scope.  Therefore, commands within an included test script *do* have access to the same global state as the
importing test script.  Additionally, changes made to the global state by an included test script will also be visible
to other test commands in the importing test script.  (The latter is in fact the primary use case of the `include`
command.)

All test scripts must reside on the file system and may be organized hierarchically under a common directory.
The driver must accept a directory as one of its command-line arguments.  The driver will recursively search
the specified directory for all test script files (with the `.sqlts` extension) and *execute each test script with its 
own isolated runtime global state such that the effects of the `set_*` commands affect only the currently executing 
test script.*

Important: test scripts intended solely to be included in other test scripts must be given the extension `.its` 
to prevent the driver from attempting to execute them directly.  The driver will only attempt to evaluate files with 
the `.isqlts` extensions. 

### Test Script Commands

#### `include`

Includes all the commands in another test script in the current test script at the point where the `include` command
is located. Makes it easier to specify and reuse commonly used state (default environment, default compile options
default session, etc). The path is relative to the location of the current test script on the file system.

##### Example

```
    include::"common_compile_options_and_environment.its"
```

##### Command Validation Rules

- The value following the `include::` annotation must be a string
- The string value must point to a file that exists.
- A test script may not include itself or another test script  that ultimately includes the same test script 
again, causing an an infinite loop.

#### `set_default_compile_options`

Sets the default compile options used by tests which do not specify compile options of their own.  Any unspecified
compile options will be set to their default values.  The compile options will be used when compiling the SQL of any
test that does not specify its own compilation options. The default values are determined by the \SqlName
implementation.  If no `set_compile_options` command is present, all tests not specifying their own compilation options
will execute with the default compile options.

##### Example

```
    set_default_compile_options::{
        variableResoltion: "STATIC",
        fictitiousOptimizationLevel: "MAXIMUM",
        strictness: "WARNINGS_AS_ERRORS",
        ...
    }
```

This has the effect of setting the default compile options to their default values because no
options are specified:

```
    set_default_compile_options::{}
```

##### Command Validation Rules

- The field names of the struct must be the names of valid compile options.  
- A field name may not be used twice.


#### `set_default_environment`

Sets the default environment used by test commands until another `set_default_environment` command is encountered.

If no `set_default_environment` command is present in a test script (or included test scripts) then the default environment 
will remain empty.

##### Examples

Add a global variable named `people` which is a list containing two structs:

```
    set_default_environment::{
        people: [
            { first_name: "Bob", last_name:"Jones" },
            { first_name: "Jane", last_name:"Jackson" }
        ],
        ...
    }
```

Load a `.ion` file to use as the default environment.  The file should contain a single Ion struct. The fields the 
struct will become global variables in the default environment:

```
    set_default_environment::"path_to.ion"
```

Load a set of `.ion` files.  As when loading a single `.ion` file, each file should contain a single Ion struct, the
fields of which will become a global variables, so field names must be unique.

```
    set_default_environment::["path_to.ion", "path_to_more.ion"]
```


Clear the environment entirely (because no variables are specified):

```
    set_default_environment::{}
```

##### Command Validation Rules

- The value following the `set_default_environment::` annotation must be one of:
    - A struct
    - A string comprising valid path to a file.
    - A list composed entirely of strings, all of which comprise valid paths to files.

#### `set_default_session`

Sets the session available during `test` command execution when a session is not explicitly specified.  Values that
are left unspecified will be set to their default values.

##### Example

```
    set_default_session::{
        utcnow = 2001-01-01T00:00:00.000+00:00
    }

```

##### Command Validation Rules

- The value specified for `utcnow` must be a timestamp at the maximum precision and may not be null.
- Other validation rules must be added here as more values are added to the session.
 
#### `test`

Executes the specified \SqlName query against the specified environment with the specified compile options and verifies the
expected result or expected error.
 
If no environment is specified, the default environment (set by the `set_default_environment` command) is used.
 
If no compile options are specified, the default compile options are used (set by the `set_default_compile_options` 
 command) are used.
 
##### Examples
 
```
    test::{
        name: simple_test_1,
        sql: "SELECT p.last_name FROM people as p", 
        environment: { people: [...], ... },
        session: { key: value, ... },
        compile_options: { option1: "<value1>", ... },
        expected: result::(bag { last_name: "Jones" }) 
    }
```

When the Ion value paired with the `expected` field has a `result::` annotation, the test must be considered failed  
if the result of evaluating `sql` doesn't match `expected` exactly, as defined by \SqlName's data equality rules 
(section 6.0.2 of the \SqlName specification).  

There is a 1:1 correlation between types of `\SqlName` and Ion with a single exception:  \SqlName 
additionally defines a bag type--which differs from a list only in that the order of the items in the bag are undefined.
The implications of this is that when two bags are compared, the order of the elements do not matter.  More 
specifically: two bags are the equivalent if every element that appears `n` times in the first bag also appears `n` 
times in the second bag.

Given that Ion does not have `<<` and `>>` notation to indicate a literal bag the way SQL does, we specify a bag in the
`expected` field of a `test::{}` using an s-expression with the `bag` tag:

```
    test::{
        ..., 
        expected: result::(bag 1 2 3),
        ...
    }
```

| `expected` Field Example | \SqlName Equivalent | Description 
---|---|---
| `[(bag 1 2 3)]` | `[<<1, 2, 3>]` | A list containing a bag of three integers. |
| `(bag { foo: (bag 1 2 3) })` | `<<{ 'foo': <<1, 2, 3>> >>` | A bag containing a struct with field `foo` that is also a bag. |
| `(sexp 1 2 3)` | ``` `(1 2 3)` ``` | An s-expression containing three integers. |
| `(sexp outer (sexp inner))` | ``` `(outer (inner))` ``` | An s-expression containing another s-expression. | 

Note that all of the above syntax is only usable within the value of the `expected` field.

If the expected result of the SQL evaluation is `MISSING`, that may be expressed as follows:

```
    test::{
        ..., 
        expected: result::(missing),
        ...
    }
```

As with `bag` and `sexp`, `missing` may exist anywhere within the `expected` field. 

When the Ion value paired with the `expected` field has an `error::` annotation an error must result from the attempt
to evaluate the SQL with the current environment.  For example:
  
```
    test::{
        name: error_test_1,
        sql: "SELECT undefined_variable FROM people",
        expected: error::{
            code: SEMANTIC_VARIABLE_UNDEFINED, 
            properties: {
                line: 1,
                column: 7,
                binding_name: "undefined_variable"
            }
    }
```

Note that the value of the `code` property shown above can be of type `string` or `symbol`.

When specified in this manner, the test is considered failed if:

- No error happens.
- If the error's `code` does not match.
- If any properties exist in the error properties reported by the car that are not specified in `properties`.
- If any properties exist in `properties` that do not exist in the error properties reported by the `car`.
- If the value of any property in the error does not match the value of the corresponding field in `properties`.

Otherwise, the test will pass.

##### Command Validation Rules

| Field           | Required                              | Type             | Other Validation                      |
|-----------------|---------------------------------------|------------------|---------------------------------------|
| name            | Yes                                   | String or Symbol | unique per test script, length > 1    | 
| compile_options | No                                    | Struct           | same as `set_default_compile_options` |
| environment     | No                                    | Struct           | same as `set_default_environment`     |
| sql             | Yes                                   | String           |                                       |
| expected_count  | Yes if `expected` is not preset       | Int              | less than 0x7fffffff                  |  
| expected        | Yes if `expected_count` is not preset | Depends          | See below                             |

The `expected` field is either annotated with `result` or `error`:
- When annotated with `result`: 
    - Can be any Ion value.  If an s-expression is encountered it must have one of the following symbols as its first
    element:
      - `bag`, and any number arguments may follow.
      - `sexp`, and any number of arguments may follow.
      - `missing`, and no arguments can be present.  
- If annotated with `error`:
    - Is a struct
    - Contains a valid error code with field name `code` 
    - Contains a struct with field name `properties` 

#### `benchmark`

Executes the set of benchmarks for the specified \SqlName query against the specified environment with the specified compile 
options and aggregates the benchmark results or error 

`benchmark` command specification is based on the `test` command specification. The differences are: 
* `expected_count` can be used in the place of `expected`. It verifies that the query result is a collection with the 
given size  
* Restricts `expected`, forbidding the `error::` variation

##### Examples
 
```
    benchmark::{
        name: simple_test_1,
        sql: "SELECT p.last_name FROM people as p", 
        environment: { people: [...], ... },
        session: { key: value, ... },
        compile_options: { option1: "<value1>", ... },
        expected_count: 100 
    }
    
    benchmark::{
        name: addition,
        sql: "1 + 1", 
        expected: result::1 
    }
```

Although benchmarks are not intended for correctness validation some result validation is still important to detect 
cases when high volume may result in a non deterministic result. Since using `expected` may be cumbersome in some 
situations, e.g. when the expected result is hundreds of lines, `expected_count` can be used instead as a soft 
validation    

##### Command Validation Rules

| Field           | Required                              | Type             | Other Validation                      |
|-----------------|---------------------------------------|------------------|---------------------------------------|
| name            | Yes                                   | String or Symbol | unique per test script, length > 1    | 
| compile_options | No                                    | Struct           | same as `set_default_compile_options` |
| environment     | No                                    | Struct           | same as `set_default_environment`     |
| sql             | Yes                                   | String           |                                       |
| expected_count  | Yes if `expected` is not preset       | Int              | less than 0x7fffffff                  |  
| expected        | Yes if `expected_count` is not preset | Any              | annotated with `result::`             |

        
#### `for`

Repeats the commands in its `template` for each of variable set listed in its `variableSets`. 

##### Examples

`for` with simple string interpolation:  

```
for::{
    template: [     
        test::{ 
            name: "simple addition test",
            sql: "1 + ${aVariable}",
            expected: $expected
        }
    ],
    variableSets: [ 
        { aVariableValue: 1, expected: result::2 }, 
        { aVariableValue: 2, expected: result::3 } 
    ],
}
 
```

Note the presence of `${aVariable}` in the `sql` field of the command.  The above command is the semantic equivalent of:

```
test::{ 
     name: "simple addition test",
     sql: "1 + 1",
     expected: expected: 2
},
test::{ 
    name: "simple addition test",
    sql: "1 + 2",
    expected: expected::3
}
```

##### More Details

- The `template` property is a list of commands. 
- The `variableSets` property is a list of structs.
- For each struct in `variableSets` the items in `template` are executed with variable references replaced with values
from the struct.  This variable replacement happens in the `driver`.
- There are two types of variable references:
    - A portion of an Ion string in the format of `${<variable>}` (shown above).  Whitespace may be present on either 
    side of `<variable>`.  String references must match the regular expression: `\$\{\s*(\S*?)\s*\}`.  The capturing 
    group is the variable name.
    - An Ion symbol prefixed with `$`
- Variable references of either type work anywhere within the template.
- Variable references may only refer to the top level fields within the structs of `variableSets`.

This illustrates everywhere variable references may be used:

```
for::{
    template: [
        set_default_compile_options::$compileOptions,  // may also be an interpolated struct
        set_default_environment::$theEnvironment,      // may also be an interpolated struct
        test::{ 
            name: "demo",
            sql: $sqlToExecute,                         // may also be an interpolated string, e.g.: "1 + ${aVariable}"
            compile_options: $theCompileOptions,        // may also be an interpolated struct, e.g.: { foo: $bar }
            environment: {                              // may also be a variable reference, e.g.: $currentEnvironment
                anEnvironmentVariable: $aVariableValue, 
                aStringInterpolatedVariable: "here is ${aVariable}",
            },
            expected: $expectedResult                   // may also be an interpolated struct
        },
    ],
    variableSets: [ 
        { 
            aVariable: "the value of aVariable", 
            expectedResult: result::42,
            compileOptions: { theCompileOptions: someValue },
            theEnvironment: { aList: [ { foo: 1 } ] } 
        }
    ]
}
```

##### Variable Sets vs Environment

The `car` is not aware of the variable sets of the `for` command.  All processing of `for` command variables must occur
within the `driver`.  `for` command variables may be used to specify environment variables.

##### Command Validation Rules

- The `template` field value must be a list containing at least one command. 
- The `variableSets` field. 
    - Must be a list containing only structs. 
    - Must have at least one struct.
    - Every struct must have at least one field.
    - A `include` command cannot be present.

##### Known issues:

- [\SqlName Test suite: support annotated variable references for test expectation](https://github.com/partiql/partiql-lang-kotlin/issues/24)

## A Complete Example

```
//No environment has yet been specified.
test::{
    name: "addition_test_1",
    sql: "1 + 1", 
    expected: result::2
}

test::{
    name:"upper_test_1",
    sql:"upper('hi jane!')",
    expected: result::'HI JANE!'
}

// All SQL will now be executed with this environment
set_env::{
    global_variable: 234,
    crew: [
        { first_name: "Jean Luc", last_name: "Picard", rank: 9 },
        { first_name: "William", last_name: "Riker", rank: 8 },
        { first_name: "Beverly", last_name: "Crusher", rank: 7 },
        { first_name: "Geordi", last_name: "LaForge", rank: 7 },
        { first_name: "Deanna", last_name: "Troi", rank: 7 },
        { first_name: "Wesley", last_name: "Crusher", rank: 1 }
    ]
}

test::{
    sql: "SELECT global_variable, f.a FROM `{a:456}`",
    expected: result::(bag { global_variable: 123, a: 456 })
}

test::{
    name: "where_clause_test_1",
    sql: "SELECT global_variable, c.first_name FROM crew AS c WHERE c.rank = 7", 
    expected: result::(bag 
        { global_variable: 234, first_name: "Beverly" }
        { global_variable: 234, first_name: "Geordi" }
        { global_variable: 234, first_name: "Deanna" }
    )
}

load_env::"someReallyBigEnvironment.ion"
 
test::{
    name: "count_star_test_1",
    sql: "SELECT COUNT(*) FROM listWith1000Items",
    expected: result::(bag { _1: 1000 })        
}

//Clear out the current environment
set_env::{}

//Reset compile options to defeault, except for `variableResolution` which is set to "STATIC"
set_compile_options::{
    variabledResolution: "STATIC"
}

test::{ 
    name: "static_scope_undefined_variable_test",
    sql: "SELECT undefined_variable FROM people",
    expected: error::{
       code: SEMANTIC_VARIABLE_UNDEFINED, 
       properties: {
           line: 1, 
           column: 6, 
           binding_name: "undefined_variable"
       }
    }
}

test::{
    name: "lotsa_fields_test_1",
    sql: '''
        SELECT
            s.field1,
            s.field2,
            /*...,*/
            s.field30
        FROM someTableWithLotsOfColumns AS s
    ''',
    expected: result::(bag 
        { field1: 1, field2: 2, /* ... , */ field30: 30 }
        /* ..., */         
    )
}

for::{
    template: [
        test::{
            name: "to_string",
            sql: "to_string(ts, '${pattern}')",
            environment: {
                ts: 2001-01-02T03:04:05.6+08:00
            },
            expected: $result
        }
    ],
    variableSets: [
        {  pattern: "yyyy",     result: result::"2001" },
        {  pattern: "M",        result: result::"1" },
        {  pattern: "dd",       result: result::"02" },
    ]
}
```
