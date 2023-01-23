# PartiQL CLI

## Build and Run the CLI 

The following command will build and run the CLI:

```shell
# To build and run
./partiql-app/partiql-cli/shell.sh

# To build (only)
./gradlew :partiql-app:partiql-cli:install

# To Run (only)
./partiql-app/partiql-cli/build/install/partiql-cli/bin/partiql
```

After building the entire project, distributable jars are located in the `cli/build/distributions` directory (relative to the 
project root).

Be sure to include the correct relative path to `gradlew` if you are not in the project root.

## CLI Options

To view all available options, run the CLI with the `--help` option.

## Non-Interactive (Single Query Execution)

### Using the Script

To execute a single query, run:

```shell
./partiql-app/partiql-cli/shell.sh query.partiql
```

where `query.partiql` contains the PartiQL query to execute.

### Using the `partiql` Command Directly

Alternatively, you may pipe input into the native command:

```shell
# Via `echo`
echo "SELECT * FROM [0, 1, 2]" | ./partiql-app/partiql-cli/build/install/partiql-cli/bin/partiql

# Via `cat`
echo ~/Desktop/query.partiql | ./partiql-app/partiql-cli/build/install/partiql-cli/bin/partiql
```

### Running a PartiQL Executable File (Unix)

Users can also create and run executable files containing PartiQL queries. To use this feature,
please add `partiql` (the built executable) to your path:

```shell
# file: ~/.bashrc or ~/.zshrc
# desc: Example configuration update of BASH or ZSH shells

# Example adding gradle-built partiql command. Need to build the executable (see directions above).
PATH_TO_PARTIQL_LANG_KOTLIN="${HOME}/partiql-lang-kotlin"
PATH="$PATH_TO_PARTIQL_LANG_KOTLIN/partiql-app/partiql-cli/build/install/partiql-cli/bin:$PATH"
export PATH
```

Once you have saved your configurations, remember to source your configuration file.
```shell
# For ZSH
source ~/.zshrc

# For Bash
source ~/.bashrc
```

Now, with the `partiql` executable on your path, you can write PartiQL files such as the below file (`example.partiql`):
```partiql
#!/usr/bin/env partiql

-- file: example.partiql
-- desc: A simple PartiQL query

SELECT t.a AS result
FROM <<
  { 'a': 1 },
  { 'a': 9 },
  { 'a': 4 },
  { 'a': 6 }
>> AS t
WHERE a > 2
ORDER BY a DESC
```

Now, you can convert this file into an executable and run it directly!
```shell
$ chmod +x ./example.partiql
$ ./example.partiql
[{'result': 9}, {'result': 6}, {'result': 4}]
```

## Interactive (Shell)

To start an interactive shell, execute:

> Note that running directly with Gradle will eat arrow keys and control sequences due to the Gradle daemon.

```shell
./partiql-app/partiql-cli/shell.sh
```

You will see a prompt that looks as follows:

```shell
Welcome to the PartiQL shell!
PartiQL> 
```

At this point, you can type in valid SQL/PartiQL and press enter *twice* to execute it:

```shell
PartiQL> SELECT id FROM `[{id: 5, name:"bill"}, {id: 6, name:"bob"}]` WHERE name = 'bob';
```
```partiql
<<
  {
    'id': 6
  }
>>
```

Alternatively, you can denote the end of a query using a semi-colon:
```shell
PartiQL> SELECT id FROM `[{id: 5, name:"bill"}, {id: 6, name:"bob"}]` WHERE name = 'bob';
```
```partiql
<<
  {
    'id': 6
  }
>>
```

The result of the previous expression is stored in the variable named `_`, so you can then run subsequent
expressions based on the last one.

```shell
PartiQL> SELECT id + 4 AS name FROM _;
```
```partiql
<<
  {
    'name': 10
  }
>>
```

Press control-D to exit the REPL.

### Advanced Shell Features

To view the AST of a PartiQL statement, type the statement and press enter only *once*, then type `!!` and press enter:

```shell
PartiQL> 1 + 1
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
OK!
```

### Initial Environment

The initial environment for the Shell can be setup with a configuration file, which should be a PartiQL file with a 
single `struct` containing the initial *global environment*.

For example, a file named `config.env` contains the following:

```partiql
{
  'animals':[
    {'name': 'Kumo', 'type': 'dog'},
    {'name': 'Mochi', 'type': 'dog'},
    {'name': 'Lilikoi', 'type': 'unicorn'}
  ],
  'types':[
    {'id': 'dog', 'is_magic': false},
    {'id': 'cat', 'is_magic': false},
    {'id': 'unicorn', 'is_magic': true}
  ]
}
```

The variables `animals` and `types` can both be bound to the execution environment for later access.
To bind the environment file to the execution environment, start the Shell with the following command:

```shell
$ ./partiql-app/partiql-cli/shell.sh -e config.env
```

Or, if you have extracted one of the compressed archives:

```shell
$ ./bin/partiql -e config.env
```

Expressions can then use the environment defined by `config.env`:

```shell
PartiQL> SELECT name, type, is_magic FROM animals, types WHERE type = id
```
```partiql
<<
  {
    'name': 'Kumo',
    'type': 'dog',
    'is_magic': false
  },
  {
    'name': 'Mochi',
    'type': 'dog',
    'is_magic': false
  },
  {
    'name': 'Lilikoi',
    'type': 'unicorn',
    'is_magic': true
  }
>>
```

To see the current Shell environment you can use `!global_env`, for example for the file above: 

```shell
PartiQL> !global_env;
```
```partiql
{
  'types': [
    {
      'id': 'dog',
      'is_magic': false
    },
    {
      'id': 'cat',
      'is_magic': false
    },
    {
      'id': 'unicorn',
      'is_magic': true
    }
  ],
  'animals': [
    {
      'name': 'Kumo',
      'type': 'dog'
    },
    {
      'name': 'Mochi',
      'type': 'dog'
    },
    {
      'name': 'Lilikoi',
      'type': 'unicorn'
    }
  ]
}
``` 

You can also add new values to the global environment or replace existing values using `!add_to_global_env`. The 
example below replaces the value bound to `types`

```shell
PartiQL> !add_to_global_env {'types': []};
```
```partiql
{
  'types': []
}
```
Let's look at what has changed:
```shell
PartiQL> !global_env
```
```partiql
{
  'types': [],
  'animals': [
    {
      'name': 'Kumo',
      'type': 'dog'
    },
    {
      'name': 'Mochi',
      'type': 'dog'
    },
    {
      'name': 'Lilikoi',
      'type': 'unicorn'
    }
  ]
}
``` 

### Working with Structure

Let's consider the following initial environment:

```partiql
{
  'stores':[
    {
     'id': 5,
     'books': [
       {'title':'A', 'price': 5.0, 'categories':['sci-fi', 'action']},
       {'title':'B', 'price': 2.0, 'categories':['sci-fi', 'comedy']},
       {'title':'C', 'price': 7.0, 'categories':['action', 'suspense']},
       {'title':'D', 'price': 9.0, 'categories':['suspense']}
     ]
    },
    {
     'id': 6,
     'books': [
       {'title':'A', 'price': 5.0, 'categories':['sci-fi', 'action']},
       {'title':'E', 'price': 9.5, 'categories':['fantasy', 'comedy']},
       {'title':'F', 'price': 10.0, 'categories':['history']}
     ]
    }
  ]
}
```
Set the environment as below
```shell
PartiQL> !add_to_global_env { 'stores':[ { 'id': 5, 'books': [ {'title':'A', 'price': 5.0, 'categories':['sci-fi', 'action']}, {'title':'B', 'price': 2.0, 'categories':['sci-fi', 'comedy']}, {'title':'C', 'price': 7.0, 'categories':['action', 'suspense']}, {'title':'D', 'price': 9.0, 'categories':['suspense']} ] }, { 'id': 6, 'books': [ {'title':'A', 'price': 5.0, 'categories':['sci-fi', 'action']}, {'title':'E', 'price': 9.5, 'categories':['fantasy', 'comedy']}, {'title':'F', 'price': 10.0, 'categories':['history']} ] } ] }
```
If we wanted to find all books *as their own rows* with a price greater than `7` we can use paths on the `FROM` for this:

```shell
PartiQL> SELECT * FROM stores[*].books[*] AS b WHERE b.price > 7;
```
```partiql
<<
  {
    'title': 'D',
    'price': 9.0,
    'categories': [
      'suspense'
    ]
  },
  {
    'title': 'E',
    'price': 9.5,
    'categories': [
      'fantasy',
      'comedy'
    ]
  },
  {
    'title': 'F',
    'price': 10.0,
    'categories': [
      'history'
    ]
  }
>>
```

If you wanted to also de-normalize the store ID and title into the above rows:

```shell
PartiQL> SELECT s.id AS store, b.title AS title FROM stores AS s, @s.books AS b WHERE b.price > 7;
```
```partiql
<<
  {
    'store': 5,
    'title': 'D'
  },
  {
    'store': 6,
    'title': 'E'
  },
  {
    'store': 6,
    'title': 'F'
  }
>>
```

We can also use sub-queries with paths to predicate on sub-structure without changing the
cardinality. So if we wanted to find all stores with books having prices greater than
`9.5`

```shell
PartiQL> SELECT * FROM stores AS s
   | WHERE EXISTS(
   |    SELECT * FROM @s.books AS b WHERE b.price > 9.5
   | );
```
```partiql
<<
  {
    'id': 6,
    'books': [
      {
        'title': 'A',
        'price': 5.0,
        'categories': [
          'sci-fi',
          'action'
        ]
      },
      {
        'title': 'E',
        'price': 9.5,
        'categories': [
          'fantasy',
          'comedy'
        ]
      },
      {
        'title': 'F',
        'price': 10.0,
        'categories': [
          'history'
        ]
      }
    ]
  }
>>
```

## Reading/Writing Files
The CLI provides the `read_file` function to stream data from a file. The files need to be placed in the folder `cli`, 
and, if using the default file type (Ion), they must contain only a single Ion value (typically a list).

**Note**: Later on, we will introduce reading different file types, but we will first focus on the default (Ion).

For example, create a file called `data.ion` in the `cli` folder with the following contents
```ion
[
    { 'city': 'Seattle', 'state': 'WA' },
    { 'city': 'Bellevue', 'state': 'WA' },
    { 'city': 'Honolulu', 'state': 'HI' },
    { 'city': 'Rochester', 'state': 'NY' }
]
```

To select the cities that are in `HI` and `NY` states:

```shell
PartiQL> SELECT city FROM read_file('data.ion') AS c, `["HI", "NY"]` AS s WHERE c.state = s;
```
```partiql
<<
  {
    'city': 'Honolulu'
  },
  {
    'city': 'Rochester'
  }
>>
```

The CLI also has the capability to write files with the `write_file` function:

```shell
PartiQL> write_file('out.ion', SELECT * FROM _);
```

A file called `out.ion` will be created in the `cli` directory with the following contents:
```ion
$bag::[
    {
        city: Honolulu
    },
    {
        city: Rochester
    }
]
```

Notice that PartiQL added the annotation of `$bag` to the Ion list. When outputting to Ion, we use type 
annotations to represent some PartiQL values/types not in Ion.

Functions and expressions can be used in the *global configuration* as well.  Consider
the following `config.ion`:

```ion
{
  'data': read_file('data.ion')
}
```

The `data` variable will now be bound to file containing Ion:

```shell
PartiQL> SELECT * FROM data;
```
```partiql
<<
    {
      'city': 'Seattle',
      'state': 'WA'
    },
    {
      'city': 'Bellevue',
      'state': 'WA'
    },
    {
      'city': 'Honolulu',
      'state': 'HI'
    },
    {
      'city': 'Rochester',
      'state': 'NY'
    }
>>
```

# TSV/CSV Data

The `read_file` function supports an optional struct argument to add additional parsing options.
Parsing delimited files can be specified with the `type` field with a string `tsv` or `csv`
to parse tab or comma separated values respectively.

**Note**: One might ask how this differs from reading in Ion files. With Ion files, PartiQL expects a *single* value -- 
typically a bag/list, but it can also be literals such as strings or integers. CSV & TSV rows, on the other hand, are 
*always* interpreted as being contained within a bag.

Create a file called `simple.csv` in the `cli` directory with the following contents:
```text
title,category,price
harry potter,book,7.99
dot,electronics,49.99
echo,electronics,99.99
```

You can read the file with the following CLI command:
```shell
PartiQL> read_file('simple.csv', {'type':'csv'});
```
```partiql
<<
    {
      _0:'title',
      _1:'category',
      _2:'price'
    },
    {
      _0:'harry potter',
      _1:'book',
      _2:'7.99'
    },
    {
      _0:'dot',
      _1:'electronics',
      _2:'49.99'
    },
    {
      _0:'echo',
      _1:'electronics',
      _2:'99.99'
    }
>>
```

The options `struct` can also define if the first row for delimited data should be the
column names with the `header` field.

```shell
PartiQL> read_file('simple.csv', {'type': 'csv', 'header': true});
```
```partiql
<<
    {
      'title': 'harry potter',
      'category': 'book',
      'price': '7.99'
    },
    {
      'title': 'dot',
      'category': 'electronics',
      'price': '49.99'
    },
    {
      'title': 'echo',
      'category': 'electronics',
      'price': '99.99'
    }
>>
```

Auto-conversion for numeric and timestamp values can also be specified as follows:

```shell
PartiQL> read_file('simple.csv', {'type':'csv', 'header':true, 'conversion':'auto'});
```
```partiql
<<
    {
      'title':' harry potter',
      'category': 'book',
      'price': 7.99
    },
    {
      'title': 'dot',
      'category': 'electronics',
      'price': 49.99
    },
    {
      'title': 'echo',
      'category': 'electronics',
      'price': 99.99
    }
>>
```

Writing TSV/CSV data can be done by specifying the optional `struct` argument to specify output
format to the `write_file` function.  Similar to the `read_file` function, the `type` field
can be used to specify `tsv`, `csv`, or `ion` output.

```shell
PartiQL> write_file('out.tsv', SELECT name, type FROM animals, {'type':'tsv'});
```

This would produce the following file:

```shell
$ cat out.tsv
Kumo	dog
Mochi	dog
Lilikoi	unicorn
```

The options `struct` can also specify a `header` Boolean field to indicate whether the output
TSV/CSV should have a header row.

```shell
PartiQL> write_file('out.tsv', SELECT name, type FROM animals, {'type':'tsv', 'header':true});
```

Which would produce the following file:

```shell
$ cat out.tsv
name	type
Kumo	dog
Mochi	dog
Lilikoi	unicorn
```

## Predefined CSV Data

The `read_file` function provides options to read other predefined CSV data formats. 
For example, if a CSV file is exported from PostgreSQL, we can use the following command 
to read the file: 
```shell
PartiQL> read_file('simple_postgresql.csv', {'type':'postgresql_csv'})
```
Other available options for the argument `type` besides `postgresql_csv` are `excel_csv`, `mysql_csv`, and `postgresql_text`. 

## Customized CSV Data 
The `read_file` function also provides options to read customized CSV data formats. 
For example, we have a data file where the whitespace is the separator as shown below: 
```text
title category price
harry_potter book 7.99
dot electronics 49.99
echo electronics 99.99
```
We can use the following command to read the file:
```shell
PartiQL> read_file('customized.csv', {'type':'customized', 'delimiter':' ', 'header':true})
```
All the available options for customized CSV files are as follows: 
1. Ignore empty lines: `'ignore_empty_line': true`
2. Ignore spaces surrounding comma:  `'ignore_surrounding_space': true` 
3. Trim leading and trailing blanks: `'trim': true` 
4. Set line breaker (only working with '\\r', '\\n' and '\\r\\n'): `'line_breaker: \n'`
5. Set escape sign (single character only): `'escape': '\'`
6. Set quote sign (single character only): `'quote': '"'`
7. Set delimiter sign (single character only): `'delimiter': ','`

## Querying Amazon DynamoDB

We also provide a CLI function, `query_ddb`, that allows you to query AWS DynamoDB tables and perform additional computations on
the response.

**Note**: This implementation uses the [Default Credentials Provider](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html). 
Please see the link to determine how you can specify which account/profile to use.

For example, let's say you have a DDB table called `users` with primary-key of `id`. If your AWS credentials and 
configurations are set correctly, you can perform the following:
```shell
PartiQL> query_ddb('SELECT * FROM users WHERE id = 0');
```
Which, depending on the content of your table, will return something like:
```shell
[
  {
    'id': 0,
    'name': 'John Doe',
    'age': 22
  }
]
```
Now, while we don't recommend performing scans on your DDB tables (due to the cost), you can execute queries similar to:
```shell
PartiQL> !add_to_global_env {
             'fullNames': <<
               'John Doe',
               'Sarah Jane',
               'Boe Jackson'
             >>
         };
PartiQL> SELECT VALUE id 
         FROM query_ddb('SELECT id, name, age FROM users')
         WHERE name IN fullNames;
```
The above query will get the `id`'s of all the users in your local environment, something like:
```partiql
<<
    0,
    17,
    1004
>>
```

Also, if you'd like to perform insertions into DDB tables, you can perform them as follows:
```shell
PartiQL> query_ddb('INSERT INTO users VALUE {''id'': 96, ''name'': ''Kim Lu'', ''age'': 26}');
```

**Note**: You can escape the PartiQL single-quote by prepending another single-quote. See above.

For in-depth documentation on valid DDB PartiQL queries, please reference the official 
[AWS DynamoDB PartiQL Docs](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.html).

## Permissive Typing Mode
By default, the CLI runs in [LEGACY](https://github.com/partiql/partiql-lang-kotlin/blob/main/lang/src/main/kotlin/org/partiql/lang/eval/CompileOptions.kt#L62)
typing mode, which will give an evaluation time error in the case of data type mismatches.

```shell
# Running in the default LEGACY typing mode
PartiQL> 1 + 'foo';
org.partiql.lang.eval.EvaluationException: ...
    ...
```

Specifying the `-p` or `-permissive` flag will allow you to run PartiQL queries in [PERMISSIVE](https://github.com/partiql/partiql-lang-kotlin/blob/main/lang/src/org/partiql/lang/eval/CompileOptions.kt#L64-L73)
typing mode, which will return `MISSING` in the case of data type mismatches.

```shell
# Running in PERMISSIVE typing mode
PartiQL> 1 + 'foo';
==='
MISSING
---
OK!
```
