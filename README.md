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
an Ion file with a single `struct` containing the initial *global environment*.

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
  ],
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

#### Working with Structure
Let's consider the following *initial environment*:

```
{
  stores:[
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
}
```

If we wanted to find all books *as their own rows* with a price greater than `7`
we can use paths on the `FROM` for this:

```
ionsql> SELECT * FROM stores.books.* WHERE price > 7
      | 
======'
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

OK!
```

If you wanted to also de-normalize the store ID and title into the above rows:

```
ionsql> SELECT b...id AS store, b.title AS title FROM stores.books.* AS b WHERE b.price > 7
      | 
======'
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

OK!
```

We can also use sub-queries with paths to predicate on sub-structure without changing the
cardinality.  So if we wanted to find all stores with books having prices greater than
`9.5`

```
ionsql> SELECT * FROM stores AS s
      | WHERE exists(
      |   SELECT * FROM stores.books.* AS b WHERE price > 9.5 AND b...id == s.id
      | )
      | 
======'
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

OK!
```

#### Reading/Writing Files
The REPL provides the `read_file` function to stream data from a file. For example:

```
ionsql> SELECT city FROM read_file("data.ion") AS c, ["HI", "NY"] AS s WHERE c.state == s.$value
      | 
======'
{
  city:"Honolulu"
}
{
  city:"Rochester"
}
```

The REPL also has the capability to write files with the `write_file` function:

```
ionsql> write_file("out.ion", SELECT * FROM _)
      | 
======'
true
------
```

Functions and expressions can be used in the *global configuration* as well.  Consider
the following `config.ion`:

```
{
  data: (read_file("data.ion"))
}
```

The `data` variable will now be bound to file containing Ion:

```
ionsql> SELECT * FROM data
      | 
======'
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

OK!
```

## TODO

* Implement a proper lexer, probably using the Ion one.
* Add support for aliasing wildcards within paths.
* Implement more the "standard" functions.
* Implement aggregation functions.
  
[ionjava]: https://code.amazon.com/packages/IonJava
[ionjava-sql-hack]: https://code.amazon.com/packages/IonJava/logs/heads/sqlhack