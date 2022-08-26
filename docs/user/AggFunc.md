# Aggregate Functions

Aggregate functions output a single result value from a collection of input values. Unless otherwise specified, aggregate functions will skip `NULL` and `MISSING`.

## AVG

Returns the average (arithmetic mean) of the input expression values.

Signature
: `AVG: COLLECTION OF NUMERIC TYPE -> DECIMAL OR NULL`

Header
: `AVG(expr)`

Purpose
: Given a collection of numeric type values, evaluates the arithmetic mean of the input expression values. 

Example
: 

```SQL
AVG(<< 1, 2, 3 >>)          -- 2 (`decimal(1, 0)`
AVG(<< 1, 2, 3, '1' >>)     -- !! ERROR !!
AVG(<< MISSING >>)          -- NULL
AVG(<< 1, 2, 3, MISSING >>) -- 2
```

## COUNT

Returns the number of “rows” in the input.

Signature
: `COUNT: STAR -> INT8`
: `COUNT: Any DATATYPE -> INT8`

Header
: `COUNT(*)`
: `COUNT(expr)`

Purpose
: Given a collection of variable in any date type, calculate the number of value in the collection.
Note: `COUNT(expr)` will skip the null and missing value, but COUNT(*) will not. 

Example
: 

```sql
COUNT(<< 1, 2, 3 >>)                     -- 3
COUNT(<< 1, 2, 3, << '1', {'a':1} >> >>) -- 4

-- COUNT(<exp>r) ignores null and missing
SELECT COUNT(a) FROM << MISSING >> AS a;       -- << { '_1': 0 } >>
SELECT COUNT(a) FROM << MISSING, 1, 2 >> AS a; -- << { '_1': 2 } >>

-- COUNT(*) includes null and missing
SELECT COUNT(*) FROM <<MISSING>> AS a;       -- << { '_1': 1 } >> 
SELECT COUNT(*) FROM <<MISSING, 1, 2>> AS a; -- << { '_1': 3 } >> 
```

## MIN/MAX

Returns the MIN/MAX value of the input expression values.

Signature
: `MIN: Any DataType -> DATATYPE`
: `MAX: Any DataType -> DATATYPE`

Header
: `MIN(expression)/MAX(expression)`

Purpose
: Given a collection of variables in any date type, find the minimum/maximum value in the collection.
: In general: Boolean < Number < Text < Blob/Clob < List < Struct < Bag

Example
: 

```SQL
MIN(<< 1, 2, 3 >>)            -- 1
MIN(<< '1', 2, 3 >>)          -- 2
MIN(<< '1', 2, 3, MISSING >>) -- 2

MAX(<< 1, 2, 3 >>)            -- 3
MAX(<< '1', 2, 3 >>)          -- '1'
MAX(<< '1', 2, 3, MISSING >>) -- '1'
```

## SUM

Returns the sum of the input expression values.

Signature
: `SUM: COLLECTION OF NUMERIC TYPE -> DECIMAL OR NULL`

Header
: `SUM(expr)`

Purpose
: Given a collection of numeric type values, evaluates the sum of the input expression values.

Example
: 

```SQL
SUM(<< 1, 2, 3 >>)         -- 6
SUM(<< 1, 2,`3.0d0` >>)    -- 6.0
SUM(<< 1, 2, `3.0e0` >>)   -- 6.0e0
SUM(<< 1, 2, 3, '1' >>)    -- !! ERROR !!
SUM(<< MISSING >>)         -- NULL
```
