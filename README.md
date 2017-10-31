


# IonSQL++ Sandbox
This package is a basic implementation of Ion SQL++, a generalized extended SQL language
that uses the Ion type system natively, but is designed to be used over sparse, hierarchical
data in a relational way.

## Getting Started
Building the package requires a standard Brazil workspace and should be buildable against `live`.

```
$ brazil-build release
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
ionsql> SELECT id FROM `[{id: 5, name:"bill"}, {id: 6, name:"bob"}]` WHERE name = 'bob'
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
an IonSQL file with a single `struct` containing the initial *global environment*.

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
$ rlwrap java -jar build/lib/ionsql-sandbox-single.jar config.sql
```

And expressions can use the bound names.

```
ionsql> SELECT name, type, is_magic FROM animals, types WHERE type = id
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
  'stores':`[
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
  ]`
}
```

If we wanted to find all books *as their own rows* with a price greater than `7`
we can use paths on the `FROM` for this:

```
ionsql> SELECT * FROM stores[*].books[*] AS b WHERE b.price > 7
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
ionsql> SELECT s.id AS store, b.title AS title FROM stores AS s, @s.books AS b WHERE b.price > 7
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
      | WHERE EXISTS(
      |   SELECT * FROM @s.books AS b WHERE b.price > 9.5
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
ionsql> SELECT city FROM read_file('data.ion') AS c, `["HI", "NY"]` AS s WHERE c.state = s
      | 
======'
{
  city:"Honolulu"
}
{
  city:"Rochester"
}
------

OK!
```

The REPL also has the capability to write files with the `write_file` function:

```
ionsql> write_file('out.ion', SELECT * FROM _)
      | 
======'
true
------

OK!
```

Functions and expressions can be used in the *global configuration* as well.  Consider
the following `config.ion`:

```
{
  'data': read_file('data.ion')
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

##### TSV/CSV Data
The `read_file` function supports an optional struct argument to add additional parsing options.
Parsing delimited files can be specified with the `type` field with a string `tsv` or `csv`
to parse tab or comma separated values respectively.

```
ionsql> read_file('simple.tsv', `{type:"tsv"}`)
      | 
======' 
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
------- 

OK! 
```

The options `struct` can also define if the first row for delimited data should be the
column names with the `header` field.

```
ionsql> read_file('simple.tsv', `{type:"tsv", header:true}`)
      | 
======' 
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
------- 

OK!
```

Auto conversion can also be specified numeric and timestamps in delimited data.

```
ionsql> read_file('simple.tsv', `{type:"tsv", header:true, conversion:"auto"}`)
      | 
======' 
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
------- 

OK!
```

Writing TSV/CSV data can be done by specifying the optional `struct` argument to specify output
format to the `write_file` function.  Similar to the `read_file` function, the `type` field
can be used to specify `tsv`, `csv`, or `ion` output.

```
ionsql> write_file('out.csv', `{type:"csv"}`, SELECT name, type FROM animals)
      | 
======' 
true
-------
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
ionsql> write_file('out.csv', `{type:"csv", header:true}`, SELECT name, type FROM animals)
      | 
======' 
true
-------
```

Which would produce the following file:

```
$ cat out.csv 
name,type
Kumo,dog
Mochi,dog
Lilikoi,unicorn
```

## Builtin Functions

### CHAR_LENGTH, CHARACTER_LENGTH

Counts the number of characters in the specified string, where 'character' is defined as a single unicode codepoint.

Note: `CHAR_LENGTH` and `CHARACTER_LENGTH` have the same syntax and functionality.
  
    CHAR_LENGTH(<str>)
    CHARACTER_LENGTH(<str>)
 
 Returns:
  
 - `NULL` if `<str>` is `NULL` 
 - `MISSING` if `<str>` is `MISSING`.  
 - Otherwise, returns the number of characters in `<str>`.
  
#### Examples

    CHAR_LENGTH('') -- Returns 0 
    CHAR_LENGTH('abcdefg') -- Returns 7
    CHAR_LENGTH('😁😞😸😸') -- Returns 4 (non-BMP unicode characters)
    CHAR_LENGTH('eࠫ') -- Returns 2 because 'eࠫ' is two codepoints: the letter 'e' and combining character U+032B

### DATE_ADD

Increments date part by specified quantity for timestamp. Subtractions can be done by using a negative quantity
    
    DATE_ADD(<date part>, <quantity>, <timestamp>)
    
Where date part is one of the following keywords: `year, month, day, hour, minute, second` 

#### Examples

    DATE_ADD(year, 5, `2010-01-01T`) -> 2015-01-01T
    DATE_ADD(month, 1, `2010T`) -> 2010T
    DATE_ADD(month, 13, `2010T`) -> 2011T 
    DATE_ADD(day, -1, `2017-01-10T`) -> 2017-01-09T

### EXISTS

Indicates if the specified `IonSequence` is empty.  Always return `false` if the value specified is not a 
sequence.

 - `EXISTS(<sequence>)`
 
#### Examples

`<sequence>` is an empty sequence:

    EXISTS([]) -- Returns false

`<sequence>` is an `IonSequence` containing 3 values:

    EXISTS([1, 2, 3]) -- Returns true

`<sequence>` is not a sequence:

    EXISTS(1) -- Returns false

### LOWER 

Converts uppercase letters in the specified string to lowercase, leaving non-uppercase characters unchanged.
This operation currently relies on the default locale as defined by Java's official 
[String.toLowerCase()](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#toLowerCase()) documentation.
See [IONSQL-110](https://i.amazon.com/issues/IONSQL-110), which will allow IonSQL++ to have client-specifiable locales.

    LOWER(<str>)

Examples:

    LOWER('AbCdEfG!@#$') -- Returns 'abcdefg!@#$'
    
### SUBSTRING

Extracts part of a string.  

 - `SUBSTRING(<str> FROM <start pos> [FOR <length>])`
 - `SUBSTRING(<str>, <start pos> [, <length>])`
 
Where:

 - `<str>` is the string containing the part to be extracted.
 - `<start pos>` is the 1-based position of the first character (unicode codepoint) to be extracted.
 - `<length>` is the count of characters (unicode codepoints) of the part to be extracted. 

Returns `NULL` if any arugment is null, or `MISSING` if any argument is missing.

#### Examples

    SUBSTRING('abcdefghi' from 3 for 4) -- Returns 'cdef'
    SUBSTRING('abcdefghi', -1, 4)       -- Returns 'ab'
    
    
### TO_STRING

 - `TO_STRING(<timestamp>, <format pattern>)`

Formats an Ion timestamp as a pretty string.

Note:  There is an issue requiring this function name to be [specified in lowercase](https://issues.amazon.com/issues/IONSQL-120).

#### Examples

    TO_STRING(`1969-07-20T20:18Z`,  'MMMM d, y')                    --Returns "July 20, 1969"
    TO_STRING(`1969-07-20T20:18Z`, 'MMM d, yyyy')                   --Returns "Jul 20, 1969"
    TO_STRING(`1969-07-20T20:18Z`, 'M-d-yy')                        --Returns "7-20-69"
    TO_STRING(`1969-07-20T20:18Z`, 'MM-d-y')                        --Returns "07-20-1969"
    TO_STRING(`1969-07-20T20:18Z`, 'MMMM d, y h:m a')               --Returns "July 20, 1969 8:18 PM"
    TO_STRING(`1969-07-20T20:18Z`, 'y-MM-dd''T''H:m:ssX')           --Returns "1969-07-20T20:18:00Z"
    TO_STRING(`1969-07-20T20:18+08:00Z`, 'y-MM-dd''T''H:m:ssX')     --Returns "1969-07-20T20:18:00Z"
    TO_STRING(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssXXXX')   --Returns "1969-07-20T20:18:00+0800"
    TO_STRING(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssXXXXX')  --Returns "1969-07-20T20:18:00+08:00"

Format symbols:
       
    Symbol          Example         Description
    ------          -------         ----------------------------------------------------------------------------
    yy              69              2-digit year
    y               1969            4-digit year
    yyyy            1969            Zero padded 4-digit year
    
    M               1               Month of year             
    MM              01              Zero padded month of year
    MMM             Jan             Abbreviated month year name
    MMMM            January         Full month of year name
    MMMMM           J               Month of year letter
    
    d               2               Day of month (1-31)
    dd              02              Zero padded day of month (01-31)
    
    a               AM              AM or PM of day
    
    h               3               Hour of day (1-12)
    hh              03              Zero padded hour of day (01-12)
    
    H               3               Hour of day (0-23)
    HH              03              Zero padded hour of day (00-23)
    
    m               4               Minute of hour (0-59)               
    mm              04              Zero padded minute of hour (00-59)
    
    s               5               Second of minute (0-59)
    ss              05              Zero padded second of minute (00-59)
    
    S               0               Fraction of second (precision: 0.1, range: 0.0-0.9) 
    SS              06              Fraction of second (precision: 0.01, range: 0.0-0.99) 
    SSS             060             Fraction of second (precision: 0.001, range: 0.0-0.999) 
    ...             ...             ...
    SSSSSSSSS       060000000       Fraction of second (maximum precision: 1 nanosecond, range: 0.0-0.999999999)
    
    n               60000000        Nano of second
    
    X               +07 or Z        Offset in hours or "Z" if the offset is 0 
    XX or XXXX      +0700 or Z      Offset in hours and minutes or "Z" if the offset is 0  
    XXX or XXXXX    +07:00 or Z     Offset in hours and minutes or "Z" if the offset is 0
    
    x               +07             Offset in hours
    xx or xxxx      +0700           Offset in hours and minutes
    xxx or xxxxx    +07:00          Offset in hours and minutes

### TO_TIMESTAMP

Converts a string to a timestamp using an optional format pattern to specify non-standard date formats.

    TO_TIMESTAMP(<string> [, <format pattern>])
    
If the `<format pattern>` argument is omitted, `<string>` is assumed to be in the format of a 
[standard Ion timestamp](https://amzn.github.io/ion-docs/spec.html#timestamp).  This is the only recommended 
way to parse an Ion timestamp using this function.

The `<format pattern>` argument supports the same format symbols as the `to_string` argument of the same name.  

Zero padding is optional when using a single format symbol (e.g. `y`, `M`, `d`, `H`, `h`, `m`, `s`) but required
for their zero padded variants (e.g. `yyyy`, `MM`, `dd`, `HH`, `hh`, `mm`, `ss`).

Special treatment is given to 2-digit years (format symbol `yy`).  1900 is added to values greater than or equal to 70
and 2000 is added to values less than 70.

Month names and AM/PM specifiers are case-insensitive.  

#### Examples

Single argument parsing an Ion timestamp:

    TO_TIMESTAMP('2007T')
    TO_TIMESTAMP('2007-02-23T12:14:33.079-08:00')
    
Two arguments parsing a custom date format:

    TO_TIMESTAMP('2016', 'y')  --Returns `2016T`
    TO_TIMESTAMP('2016', 'yyyy')  --Returns `2016T`
    TO_TIMESTAMP('02-2016', 'MM-yyyy')  --Returns `2016-02T`
    TO_TIMESTAMP('Feb 2016', 'MMM yyyy')  --Returns `2016-02T`
    TO_TIMESTAMP('Febrary 2016', 'MMMM yyyy')  --Returns `2016-02T`

Notes:

[All SIM items for IonSQL++'s `TO_TIMESTAMP` function](https://i.amazon.com/issues/search?q=status%3A(Open)+(TO_TIMESTAMP)+containingFolder%3A(0efa7b8c-5170-4de7-a8e7-d0975778a686)&sort=lastUpdatedConversationDate+desc&selectedDocument=0b5e3cc3-40bc-40cf-854b-977f4ae4e08d).

Internally, this is implemented with Java 8's `java.time` package.  There are a few differences between Ion's 
timestamp and the `java.time` package that create a few hypothetically infrequently encountered caveats that do not 
really have good workarounds at this time.    

- The Ion specification allows for explicitly signifying an unknown timestamp with a negative zero offset 
(i.e. the `-00:00` at the end of `2007-02-23T20:14:33.079-00:00`) but Java 8's `DateTimeFormatter` doesn't recognize 
this. **Hence, unknown offsets specified in this manner will be parsed as if they had an offset of `+00:00`, i.e. UTC.** 
To avoid this issue when parsing Ion formatted timestamps, use the single argument variant of `TO_TIMESTAMP`.  There 
is no workaround for custom format patterns at this time.
- `DateTimeFormatter` is capable of parsing UTC offsets to the precision of seconds, but Ion Timestamp's precision for 
offsets is minutes. TimestampParser currently handles this by throwing an exception when an attempt is made to parse a 
timestamp with an offset that does does not land on a minute boundary.  For example, parsing this timestamp would 
throw an exception:  `May 5, 2017 8:52pm +08:00:01` while `May 5, 2017 8:52pm +08:00:00` would not. 
- Ion Java's Timestamp allows specification of offsets up to +/- 23:59, while an exception is thrown by 
`DateTimeFormatter` for any attempt to parse an offset greater than +/- 18:00.  For example, attempting to parse: 
`May 5, 2017 8:52pm +18:01` would cause and exception to be thrown.  (Note: the Ion specification does 
indicate minimum and maximum allowable values for offsets.) In practice this will not be an issue for systems that do 
not abuse the offset portion of Timestamp because real-life offsets do not exceed +/- 12h.  



### TRIM
 
Trims leading and/or trailing characters from a String. If the characters to be trimmed are not specified it defaults
to `' '`. 

    TRIM([[LEADING|TRAILING|BOTH <characters to remove>] FROM] <str>) 

#### Examples

    TRIM('       foobar         ') -- returns 'foobar'
    TRIM('      \tfoobar\t         ') -- returns '\tfoobar\t'
    TRIM(LEADING FROM '       foobar         ') -- returns 'foobar         '
    TRIM(TRAILING FROM '       foobar         ') -- returns '       foobar'
    TRIM(BOTH FROM '       foobar         ') -- returns 'foobar'
    TRIM(BOTH '😁' FROM '😁😁foobar😁😁') -- returns 'foobar'
    TRIM(BOTH '12' FROM '1112211foobar22211122') -- returns 'foobar' 

### UPPER 

Converts lowercase letters in the specified string to uppercase, leaving non-lowercase characters unchanged. This 
operation currently relies on the default locale as defined by Java's official 
[String.toUpperCase()](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#toUpperCase()) documentation.
See [IONSQL-110](https://i.amazon.com/issues/IONSQL-110), which will allow IonSQL++ to have client-specifiable locales.

    UPPER(<str>)

#### Examples

    UPPER('AbCdEfG!@#$') -- Returns 'ABCDEFG!@#$'
    
### UTCNOW 

returns the current time timestamp in UTC. Current time is defined by the `now` value in the `EvaluationSession` so it's 
consistent across an evaluation. The client can specify its own `now` value when creating the session    

    UTCNOW()

Examples:

    UTCNOW() -- Returns 2017-10-13T16:02:11.123Z
    
## Helpful Links

 - [Hyperlinked SQL-92 BNF](https://ronsavage.github.io/SQL/sql-92.bnf.html) - this is much easier to navigate than the official ISO standard!
 - [sqlfiddle.com](http://sqlfiddle.com/) - Use this to experiment with SQL as implemented by Postgres, MySQL, Oracle, MS SQL Server and SQLite. 

## TODO

* Implement more of the "standard" functions.
* Implement aggregation, sort, grouping.
  
[ionjava]: https://code.amazon.com/packages/IonJava

