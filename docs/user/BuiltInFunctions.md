## Builtin Functions

This section provides documentation for all built-in functions available with the reference implementation. 
For each function the documentation provides 

1. A one sentence explanation of the functions intent.
1. The function's **signature** that specifies data types and names for each input argument, and, the expected data type for the function's return value. A function signature consists of the function's name followed by a colon `:` then a space separated list of data types--one for each formal argument of the function--followed by an arrow `->` followed by the function's return type, e.g., `add: Integer Integer -> Integer` is the signature for `add` which accepts 2 inputs both `Integer`'s and returns one value of type `Integer`. 
1. The function's **header** that specifies names for each of the function's formal arguments. Any documentation following the header can refer
   to the function's formal arguments by name. 
1. The function's **purpose statement** that further expands on how the function behaves and specifies any pre- and/or post-conditions. 
1. A list of examples calling the function and their expected results. 


### Unknown (`null` and `missing`) propagation

Unless otherwise stated all functions listed bellow propagate `null` and `missing` argument values. Propagating `null` 
and `missing` values is defined as: if any function argument is either `null` or `missing` the function will return 
`null`, e.g., 

```sql 
CHAR_LENGTH(null)    -- `null`
CHAR_LENGTH(missing) -- `null` (also returns `null`)
```

### CHAR_LENGTH, CHARACTER_LENGTH



Counts the number of characters in the specified string, where 'character' is defined as a single unicode code point.

*Note:* `CHAR_LENGTH` and `CHARACTER_LENGTH` are synonyms. 


Signature
: `CHAR_LENGTH: String -> Integer`

  `CHARACTER_LENGTH: String -> Integer`

Header
: `CHAR_LENGTH(str)`

  `CHARACTER_LENGTH(str)`

Purpose
: Given a `String` value `str` return the number of characters (code points) in `str`.
  
Examples
: 

```sql  
CHAR_LENGTH('')          -- 0
CHAR_LENGTH('abcdefg')   -- 7
CHAR_LENGTH('😁😞😸😸') -- 4 (non-BMP unicode characters)
CHAR_LENGTH('eࠫ')         -- 2 (because 'eࠫ' is two codepoints: the letter 'e' and combining character U+032B)
```

### COALESCE

Evaluates the arguments in order and returns the first non unknown, i.e. first non-`null` or non-`missing`. This function 
does **not** propagate `null` and `missing`.

Signature
: `COALESCE: Any Any ... -> Any`

Header
: `COALESCE(exp, [exp ...])`

Purpose
: Given a list of 1 or more arguments, evaluates the arguments left-to-right and returns the first value that is **not** an unknown (`missing` or
        `null`). 

Examples
: 

```sql  
COALESCE(1)                -- 1
COALESCE(null)             -- null
COALESCE(null, null)       -- null
COALESCE(missing)          -- null
COALESCE(missing, missing) -- null
COALESCE(1, null)          -- 1
COALESCE(null, null, 1)    -- 1
COALESCE(null, 'string')   -- 'string'
COALESCE(missing, 1)       -- 1
```

### DATE_ADD

Given a data part, a quantity and a timestamp, returns an updated timestamp by altering date part by quantity

Signature 
: `DATE_ADD: DatePart Integer Timestamp -> Timestamp`

Where `DatePart` is one of 

* `year`
* `month`
* `day`
* `hour`
* `minute`
* `second`

Header
: `DATE_ADD(dp, q, timestamp)`

Purpose
:  Given a data part `dp`, a quantity `q`, and, an Ion timestamp `timestamp` returns an updated timestamp by applying the value for `q` to the `dp`
component of `timestamp`. Positive values for `q` add to the `timestamp`'s `dp`, negative values subtract. 

The value for `timestamp` as well as the return value from `DATE_ADD` must be a valid [Ion Timestamp](https://amzn.github.io/ion-docs/spec.html#timestamp)

Examples
: 

```sql  
DATE_ADD(year, 5, `2010-01-01T`)                -- 2015-01-01 (equivalent to 2015-01-01T)
DATE_ADD(month, 1, `2010T`)                     -- 2010-02T (result will add precision as necessary)
DATE_ADD(month, 13, `2010T`)                    -- 2011-02T
DATE_ADD(day, -1, `2017-01-10T`)                -- 2017-01-09 (equivalent to 2017-01-09T)
DATE_ADD(hour, 1, `2017T`)                      -- 2017-01-01T01:00-00:00
DATE_ADD(hour, 1, `2017-01-02T03:04Z`)          -- 2017-01-02T04:04Z
DATE_ADD(minute, 1, `2017-01-02T03:04:05.006Z`) -- 2017-01-02T03:05:05.006Z
DATE_ADD(second, 1, `2017-01-02T03:04:05.006Z`) -- 2017-01-02T03:04:06.006Z
```

### DATE_DIFF

Given a date part and two valid timestamps returns the difference in date parts.

Signature
: `DATE_DIFF: DatePart Timestamp Timestamp -> Integer`

See [DATE_ADD](#date_add) for the definition of `DatePart`

Header
: `DATE_DIFF(dp, t1, t2)`


Purpose
: Given a date part `dp` and two timestamps `t1` and `t2` returns the difference in value for `dp` part of `t1` with `t2`. 
The return value is a negative integer when the `dp` value of `t1` is greater than the `dp` value of `t2`, and, a positive 
integer when the `dp` value of `t1` is less than the `dp` value of `t2`. 


Examples
:  

```sql  
DATE_DIFF(year, `2010-01-01T`, `2011-01-01T`)            -- 1
DATE_DIFF(year, `2010T`, `2010-05T`)                     -- 4 (2010T is equivalent to 2010-01-01T00:00:00.000Z)
DATE_DIFF(month, `2010T`, `2011T`)                       -- 12
DATE_DIFF(month, `2011T`, `2010T`)                       -- -12
DATE_DIFF(day, `2010-01-01T23:00T`, `2010-01-02T01:00T`) -- 0 (need to be at least 24h apart to be 1 day apart)
```


### EXISTS

Given an IonSQL++ value returns `true` if and only if the value is a non-empty sequence, returns `false` otherwise. 

Signature
: `EXISTS: Any -> Boolean`

Header
: `EXISTS(val)`

Purpose 
: Given an IonSQL++ value, `val`, returns `true` if and only if `val` is a non-empty sequence, returns `false` otherwise. 
This function does **not** propagate `null` and `missing`.  


Examples
:  

```sql
EXISTS(`[]`)        -- false (empty list)
EXISTS(`[1, 2, 3]`) -- true (non-empty list)
EXISTS(`[missing]`) -- true (non-empty list)
EXISTS(`{}`)        -- false (empty struct)
EXISTS(`{ a: 1 }`)  -- true (non-empty struct)
EXISTS(`()`)        -- false (empty s-expression)
EXISTS(`(+ 1 2)`)   -- true (non-empty s-expression)
EXISTS(`<<>>`)      -- false (empty bag)
EXISTS(`<<null>>`)  -- true (non-empty bag)
EXISTS(1)           -- false
EXISTS(`2017T`)     -- false
EXISTS(null)        -- false
EXISTS(missing)     -- false
```

### EXTRACT

Extracts a date part from a timestamp. 
    
    EXTRACT(<date part> FROM <timestamp>)
    
Where date part is one of the following keywords: `year, month, day, hour, minute, second, timestamp_hour, timestamp_minute`. 
Note that the allowed date parts for `EXTRACT` is not the same as `DATE_ADD`

**Note**: Extract does not propagate null for it's first parameter, the date part. From the SQL92 spec only the date 
part keywords are allowed as first argument   

#### Examples
    
```sql
EXTRACT(YEAR FROM `2010-01-01T`) -- 2010
EXTRACT(MONTH FROM `2010T`) -- null as `2010T` doesn't have a `month` part 
EXTRACT(MONTH FROM `2010-10T`) -- 10 
EXTRACT(TIMESTAMP_HOUR FROM `2010-01-01T15:20:30+10:20`) -- 10
EXTRACT(TIMESTAMP_HOUR FROM `2010-01-01T15:20:30-10:20`) -- -10
EXTRACT(TIMESTAMP_MINUTE FROM `2010-01-01T15:20:30+10:20`) -- 20
EXTRACT(TIMESTAMP_MINUTE FROM `2010-01-01T15:20:30-10:20`) -- -20
```

### LOWER 

Converts uppercase letters in the specified string to lowercase, leaving non-uppercase characters unchanged.
This operation currently relies on the default locale as defined by Java's official 
[String.toLowerCase()](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#toLowerCase()) documentation.
See [IONSQL-110](https://i.amazon.com/issues/IONSQL-110), which will allow IonSQL++ to have client-specifiable locales.

    LOWER(<str>)

#### Examples

```sql
LOWER('AbCdEfG!@#$') -- Returns 'abcdefg!@#$'
```

### SIZE   
Returns the quantity of elements in a container, i.e a `BAG`, `STRUCT` or `LIST`, as an `INT`. Throws an exception for 
any other argument type

    SIZE(<container>)
      
#### Examples

```sql
SIZE(`[1,2,3]`) -- returns 3
SIZE(<<'foo', 'bar'>>) -- returns 2
SIZE(`{foo: bar}`) -- returns 1
SIZE(`[{foo: 1}, {foo: 2}]`) -- returns 2
SIZE(12) -- throws an exception
```

### NULLIF

Returns null if both specified expressions are equal, otherwise returns the first argument. Equality here is defined as 
the `=` operator in IonSQL, i.e. `a` and `b` are considered equal by `NULLIF` if `a = b` is true 

This function does **not** propagate `null` and `missing`.

    NULLIF(expression1, expression2) 

#### Examples

```sql  
NULLIF(1, 1) -- null
NULLIF(1, 2) -- 1
NULLIF(1.0, 1) -- null
NULLIF(1, '1') -- 1
NULLIF([1], [1]) -- null
NULLIF(1, NULL) -- 1
NULLIF(null, null) -- null
NULLIF(missing, null) -- null
```
    
### SUBSTRING

Extracts part of a string.  

 - `SUBSTRING(<str> FROM <start pos> [FOR <length>])`
 - `SUBSTRING(<str>, <start pos> [, <length>])`
 
Where:

 - `<str>` is the string containing the part to be extracted.
 - `<start pos>` is the 1-based position of the first character (unicode codepoint) to be extracted.
 - `<length>` is the count of characters (unicode codepoints) of the part to be extracted. 

#### Examples
```sql
SUBSTRING('abcdefghi' from 3 for 4) -- Returns 'cdef'
SUBSTRING('abcdefghi', -1, 4)       -- Returns 'ab'
```
    
### TO_STRING

    `TO_STRING(<timestamp>, <format pattern>)`

Formats an Ion timestamp as a pretty string.

#### Examples
```sql
TO_STRING(`1969-07-20T20:18Z`,  'MMMM d, y')                    --Returns "July 20, 1969"
TO_STRING(`1969-07-20T20:18Z`, 'MMM d, yyyy')                   --Returns "Jul 20, 1969"
TO_STRING(`1969-07-20T20:18Z`, 'M-d-yy')                        --Returns "7-20-69"
TO_STRING(`1969-07-20T20:18Z`, 'MM-d-y')                        --Returns "07-20-1969"
TO_STRING(`1969-07-20T20:18Z`, 'MMMM d, y h:m a')               --Returns "July 20, 1969 8:18 PM"
TO_STRING(`1969-07-20T20:18Z`, 'y-MM-dd''T''H:m:ssX')           --Returns "1969-07-20T20:18:00Z"
TO_STRING(`1969-07-20T20:18+08:00Z`, 'y-MM-dd''T''H:m:ssX')     --Returns "1969-07-20T20:18:00Z"
TO_STRING(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssXXXX')   --Returns "1969-07-20T20:18:00+0800"
TO_STRING(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssXXXXX')  --Returns "1969-07-20T20:18:00+08:00"
```
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
    MMMMM           J               Month of year first letter (NOTE: not valid for use with to_timestamp function)

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
```sql
TO_TIMESTAMP('2007T')
TO_TIMESTAMP('2007-02-23T12:14:33.079-08:00')
```
Two arguments parsing a custom date format:
```sql
TO_TIMESTAMP('2016', 'y')  --Returns `2016T`
TO_TIMESTAMP('2016', 'yyyy')  --Returns `2016T`
TO_TIMESTAMP('02-2016', 'MM-yyyy')  --Returns `2016-02T`
TO_TIMESTAMP('Feb 2016', 'MMM yyyy')  --Returns `2016-02T`
TO_TIMESTAMP('Febrary 2016', 'MMMM yyyy')  --Returns `2016-02T`
```
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
```sql
TRIM('       foobar         ') -- returns 'foobar'
TRIM('      \tfoobar\t         ') -- returns '\tfoobar\t'
TRIM(LEADING FROM '       foobar         ') -- returns 'foobar         '
TRIM(TRAILING FROM '       foobar         ') -- returns '       foobar'
TRIM(BOTH FROM '       foobar         ') -- returns 'foobar'
TRIM(BOTH '😁' FROM '😁😁foobar😁😁') -- returns 'foobar'
TRIM(BOTH '12' FROM '1112211foobar22211122') -- returns 'foobar' 
```
### UPPER 

Converts lowercase letters in the specified string to uppercase, leaving non-lowercase characters unchanged. This 
operation currently relies on the default locale as defined by Java's official 
[String.toUpperCase()](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#toUpperCase()) documentation.
See [IONSQL-110](https://i.amazon.com/issues/IONSQL-110), which will allow IonSQL++ to have client-specifiable locales.

    UPPER(<str>)

#### Examples
```sql
UPPER('AbCdEfG!@#$') -- Returns 'ABCDEFG!@#$'
```    

### UTCNOW 

returns the current time timestamp in UTC. Current time is defined by the `now` value in the `EvaluationSession` so it's 
consistent across an evaluation. The client can specify its own `now` value when creating the session    

    UTCNOW()

#### Examples
```sql
UTCNOW() -- Returns the current time, e.g. 2017-10-13T16:02:11.123Z
```
