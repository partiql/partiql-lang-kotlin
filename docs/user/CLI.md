# \SqlName CLI

```
PartiQL CLI
Command line interface for executing PartiQL queries. Can be run in an interactive (REPL) mode or non-interactive.

Examples:
To run in REPL mode simply execute the executable without any arguments:
     sqlcli

In non-interactive mode input data is bound to a global variable named "input_data", in the example below
/logs/log.ion is bound to "input_data":
     sqlcli --query="SELECT * FROM input_data" --input=/logs/log.ion
In non-interactive mode input data is bound to "input_data", in the example below the /logs/log.ion is bound to "input_data":
     sqlcli --query="SELECT * FROM input_data" --input=/logs/log.ion

To output binary ion:
     sqlcli --query="SELECT * FROM input_data" --output-format=BINARY --input=/logs/log.ion

To pipe input data in via stdin:
     cat /logs/log.10n | sqlcli --query="SELECT * FROM input_data" --format=BINARY > output.10n

Option                                               Description
------                                               -----------
-e, --environment <File>                             initial global environment (optional)
-h, --help                                           prints this help
-i, --input <File>                                   input file, requires the query option (default: stdin)
-o, --output <File>                                  output file, requires the query option (default: stdout)
--of, --output-format <OutputFormat: (TEXT|BINARY)>  output format, requires the query option (default: TEXT)
-q, --query <String>                                 PartiQL query, triggers non interactive mode
```

# Building the CLI 

The CLI is built during the main Gradle build.  To build it separately execute:

```
./gradlew :cli:build
```

After building, distributable jars are located in the `cli/build/distributions` directory (realtive to the 
project root).

Be sure to include the correct relative path to `gradlew` if you are not in the project root.

# Using the CLI

The following command will build any dependencies before starting the CLI.

```
./gradlew :cli:run -q --args="<command line arguments>"
```

# REPL

To start an interactive read, eval, print loop (REPL) execute:

```
rlwrap ./gradlew :cli:run --console=plain
```

[rlwrap](https://github.com/hanslub42/rlwrap) provides command history support.  It allows 
the use of the up and down arrow keys to cycle through recently executed commands and remebers commands entered into 
previous sessions. `rlwrap` is available as an optional package in all major Linux distributions and in 
[Homebrew](https://brew.sh/) on MacOS.  `rlwrap` is not required but is highly recommended. 

You will see a prompt that looks as follows:

```
sql> 
```

At this point you can type in SQL and press enter *twice* to execute it:

```
sql> SELECT id FROM `[{id: 5, name:"bill"}, {id: 6, name:"bob"}]` WHERE name = 'bob'
   | 
==='
{
  id:6
}
---
Result type was BAG and contained 1 items
OK! (130 ms)
```

The result of previous expression is stored in the variable named `_`, so you can then run subsequent
expressions based on the last one.

```
sql> SELECT id + 4 AS name FROM _
   | 
==='
{
  name:10
}
---
Result type was BAG and contained 1 items
OK! (20 ms)
```

Press control-D to exit the REPL.

## Advanced REPL Features

To view the AST of an SQL statement, type one and press enter only *once*, then type `!!` and press enter:

```
sql> 1 + 1
   | !!
==='
(
  ast
  (
    version
    1
  )
  (
    root
    (
      +
      (
        lit
        1
      )
      (
        lit
        1
      )
    )
  )
)
---
Result type was SEXP
OK! (32 ms)
```

To view the AST with metadata information of an SQL statement, type one and press enter only *once*, 
then type `!?` and press enter:

```
sql> 1 + 1
   | !?
==='
(
  ast
  (
    version
    1
  )
  (
    root
    (
      term
      (
        exp
        (
          +
          (
            term
            (
              exp
              (
                lit
                1
              )
            )
            (
              meta
              (
                $source_location
                (
                  {
                    line_num:1,
                    char_offset:1
                  }
                )
              )
            )
          )
          (
            term
            (
              exp
              (
                lit
                1
              )
            )
            (
              meta
              (
                $source_location
                (
                  {
                    line_num:1,
                    char_offset:5
                  }
                )
              )
            )
          )
        )
      )
      (
        meta
        (
          $source_location
          (
            {
              line_num:1,
              char_offset:3
            }
          )
        )
      )
    )
  )
)
---
Result type was SEXP
OK! (5 ms)
```

## Initial Environment

The initial environment for the REPL can be setup with a configuration file, which should be an \SqlName file with a 
single `struct` containing the initial *global environment*.

For example a file named `config.sql`, containing the following:

```
{
  'animals':`[
    {name: "Kumo", type: "dog"},
    {name: "Mochi", type: "dog"},
    {name: "Lilikoi", type: "unicorn"},
  ]`,
  'types':`[
    {id: "dog", is_magic: false},
    {id: "cat", is_magic: false},
    {id: "unicorn", is_magic: true},
  ]`,
}
```

Could be loaded into the REPL with `animals` and `types` bound list of `struct` values.

The REPL could be started up with:

```
$ ./gradlew :cli:run -q --console=plain --args='-e config.sql'
```

(Note that shell expansions such as `~` do not work within the value of the `args` argument.)

Or if you have extracted one of the compressed archives:

```
$ ./bin/sqlcli -e config.sql
```

Expressions can then use the environment defined by `config.sql`:

```
sql> SELECT name, type, is_magic FROM animals, types WHERE type = id
   | 
==='
{
  name:"Kumo",
  type:"dog",
  is_magic:false
}
{
  name:"Mochi",
  type:"dog",
  is_magic:false
}
{
  name:"Lilikoi",
  type:"unicorn",
  is_magic:true
}
------
Result type was BAG and contained 3 items
OK! (64 ms)
```

# Working with Structure

Let's consider the following initial environment:

```
`{
  'stores':[
    {
     id: "5",
     books: [
       {title:"A", price: 5.0, categories:["sci-fi", "action"]},
       {title:"B", price: 2.0, categories:["sci-fi", "comedy"]},
       {title:"C", price: 7.0, categories:["action", "suspense"]},
       {title:"D", price: 9.0, categories:["suspense"]},
     ]
    },
    {
     id: "6",
     books: [
       {title:"A", price: 5.0, categories:["sci-fi", "action"]},
       {title:"E", price: 9.5, categories:["fantasy", "comedy"]},
       {title:"F", price: 10.0, categories:["history"]},
     ]
    }
  ]
}`
```

If we wanted to find all books *as their own rows* with a price greater than `7` we can use paths on the `FROM` for this:

```
sql> SELECT * FROM stores[*].books[*] AS b WHERE b.price > 7
   | 
==='
{
  title:"D",
  price:9.0,
  categories:[
    "suspense"
  ]
}
{
  title:"E",
  price:9.5,
  categories:[
    "fantasy",
    "comedy"
  ]
}
{
  title:"F",
  price:10.0,
  categories:[
    "history"
  ]
}
------
Result type was BAG and contained 3 items
OK! (70 ms)
```

If you wanted to also de-normalize the store ID and title into the above rows:

```
sql> SELECT s.id AS store, b.title AS title FROM stores AS s, @s.books AS b WHERE b.price > 7
   | 
==='
{
  store:"5",
  title:"D"
}
{
  store:"6",
  title:"E"
}
{
  store:"6",
  title:"F"
}
------
Result type was BAG and contained 3 items
OK! (32 ms)
```

We can also use sub-queries with paths to predicate on sub-structure without changing the
cardinality. So if we wanted to find all stores with books having prices greater than
`9.5`

```
sql> SELECT * FROM stores AS s
   | WHERE EXISTS(
   |   SELECT * FROM @s.books AS b WHERE b.price > 9.5
   | )
   | 
==='
{
  id:"6",
  books:[
    {
      title:"A",
      price:5.0,
      categories:[
        "sci-fi",
        "action"
      ]
    },
    {
      title:"E",
      price:9.5,
      categories:[
        "fantasy",
        "comedy"
      ]
    },
    {
      title:"F",
      price:10.0,
      categories:[
        "history"
      ]
    }
  ]
}
------
Result type was BAG and contained 1 items
OK! (52 ms)
```

# Reading/Writing Files
The REPL provides the `read_file` function to stream data from a file. The files needs to be placed in the folder `cli`. 
For example:

```
sql> SELECT city FROM read_file('data.ion') AS c, `["HI", "NY"]` AS s WHERE c.state = s
   | 
==='
{
  city:"Honolulu"
}
{
  city:"Rochester"
}
------
Result type was BAG and contained 3 items
OK! (45 ms)
```

The REPL also has the capability to write files with the `write_file` function:

```
sql> write_file('out.ion', SELECT * FROM _)
   | 
==='
true
------
Result type was BOOL
OK! (20 ms)
```

Functions and expressions can be used in the *global configuration* as well.  Consider
the following `config.ion`:

```
`{
  'data': read_file('data.ion')
}`
```

The `data` variable will now be bound to file containing Ion:

```
sql> SELECT * FROM data
   | 
==='
{
  city:"Seattle",
  state:"WA"
}
{
  city:"Bellevue",
  state:"WA"
}
{
  city:"Honolulu",
  state:"HI"
}
{
  city:"Rochester",
  state:"NY"
}
------
Result type was BAG and contained 3 items
OK! (75 ms)
```

# TSV/CSV Data

The `read_file` function supports an optional struct argument to add additional parsing options.
Parsing delimited files can be specified with the `type` field with a string `tsv` or `csv`
to parse tab or comma separated values respectively.

```
sql> read_file('simple.tsv', `{type:"tsv"}`)
   | 
===' 
{
  _0:"title",
  _1:"category",
  _2:"price"
}
{
  _0:"harry potter",
  _1:"book",
  _2:"7.99"
}
{
  _0:"dot",
  _1:"electronics",
  _2:"49.99"
}
{
  _0:"echo",
  _1:"electronics",
  _2:"99.99"
}
---- 
Result type was BAG and contained 3 items
OK! (83 ms)
```

The options `struct` can also define if the first row for delimited data should be the
column names with the `header` field.

```
sql> read_file('simple.tsv', `{type:"tsv", header:true}`)
   | 
===' 
{
  title:"harry potter",
  category:"book",
  price:"7.99"
}
{
  title:"dot",
  category:"electronics",
  price:"49.99"
}
{
  title:"echo",
  category:"electronics",
  price:"99.99"
}
---- 
Result type was BAG and contained 3 items
OK! (87 ms)
```

Auto conversion can also be specified numeric and timestamps in delimited data.

```
sql> read_file('simple.tsv', `{type:"tsv", header:true, conversion:"auto"}`)
   | 
===' 
{
  title:"harry potter",
  category:"book",
  price:7.99
}
{
  title:"dot",
  category:"electronics",
  price:49.99
}
{
  title:"echo",
  category:"electronics",
  price:99.99
}
---- 
Result type was BAG and contained 3 items
OK! (96 ms)
```

Writing TSV/CSV data can be done by specifying the optional `struct` argument to specify output
format to the `write_file` function.  Similar to the `read_file` function, the `type` field
can be used to specify `tsv`, `csv`, or `ion` output.

```
sql> write_file('out.csv', `{type:"csv"}`, SELECT name, type FROM animals)
   | 
===' 
true
----
Result type was BOOL
OK! (41 ms)
```

This would produce the following file:

```
$ cat out.csv
Kumo,dog
Mochi,dog
Lilikoi,unicorn
```

The options `struct` can also specify a `header` Boolean field to indicate whether the output
TSV/CSV should have a header row.

```
sql> write_file('out.csv', `{type:"csv", header:true}`, SELECT name, type FROM animals)
   | 
===' 
true
----
Result type was BOOL
OK! (39 ms)
```

Which would produce the following file:

```
$ cat out.csv 
name,type
Kumo,dog
Mochi,dog
Lilikoi,unicorn
```
