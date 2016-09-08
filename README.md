# IonSQL Sandbox
This package is a basic implementation of Ion SQL, a generalized extended SQL language
that uses the Ion type system natively, but is designed to be used over sparse, hierarchical
data in a relational way.

## Getting Started
In order to build Ion SQL Sandbox, your workspace should have the [sql-hack][ionjava-sql-hack]
branch of [IonJava][ionjava] in your workspace or built into your version set.  This is
required because this implementation currently leverages the Ion parser as its tokenizer, 
and this branch specifically supports unquoted `,` in S-expressions.

Once this is done the package can be built as normal with:

```
$ brazil-build
```

Building a self-contained JAR with a very basic read-eval-print loop (REPL), can be done
using the `single-jar` target:

```
$ brazil-build single-jar
```

Once this is done the REPL can be used to experiment with Ion SQL.

## Using the REPL
Once built, the REPL can be run by invoking the `com.amazon.ionsql.tools.REPL` program.
If the `single-jar` was built, this can be run as follows:

```
$ java -jar build/lib/ionsql-sandbox-single.jar
```

It may be useful to use `rlwrap` to get a command history support.

You will get a prompt that looks as follows:

```
ionsql> 
```

At this point you can type in Ion SQL and press *double enter* to execute SQL:

```
ionsql> SELECT id FROM [{id: 5, name:"bill"}, {id: 6, name:"bob"}] WHERE name == "bob"
      | 
======'
{
  id:6
}
------

OK!
```

The previous expression is stored in the variable named `_`, so you can then run subsequent
expressions based on the last one.

```
ionsql> SELECT id + 4 AS name FROM _
      | 
======'
{
  name:10
}
------
```

#### Initial Environment
The initial environment for the REPL can be setup with a configuration file, which should be
an Ion file with a single `struct` containing the initial global environment.

For example a file named `config.ion`, containing the following:

```
{
  animals:[
    {name: "Kumo", type: "dog"},
    {name: "Mochi", type: "dog"},
    {name: "Lilikoi", type: "unicorn"},
  ],
  types:[
    {id: "dog", is_magic: false},
    {id: "cat", is_magic: false},
    {id: "unicorn", is_magic: true},
  ]
}
```

Could be loaded into the REPL with `animals` and `types` bound list of `struct` values.

The REPL could be started up with:

```
$ rlwrap java -jar build/lib/ionsql-sandbox-single.jar config.ion
```

And expressions can use the bound names.

```
ionsql> SELECT name, type, is_magic FROM animals, types WHERE type == id
      | 
======'
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

OK!
```
  
[ionjava]: https://code.amazon.com/packages/IonJava
[ionjava-sql-hack]: https://code.amazon.com/packages/IonJava/logs/heads/sqlhack