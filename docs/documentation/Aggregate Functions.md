# Aggregate Functions

Aggregate functions output a single result value from a collection of input values. 

Unless otherwise specified, aggregate functions will skip `NULL` and `MISSING`.

## AVG

Description
: Given a collection of numeric type values, evaluates the arithmetic mean of the input expression values.

Signatures
: `AVG(collection<NUMERIC | NULL | MISSING>) -> DECIMAL or NULL`

Example
: 

```SQL
AVG(<< 1, 2, 3 >>)          -- 2 `decimal(1, 0)`
AVG(<< MISSING >>)          -- NULL
AVG(<< 1, 2, 3, MISSING >>) -- 2
```

## COUNT

Description
: Given a collection of variable in any date type, calculate the number of value in the collection.
Note: `COUNT(expr)` will skip the null and missing value, but COUNT(*) will not.

Signatures
: `COUNT(*) -> INT8`
: `COUNT(any) -> INT8`

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

Description
: Given a collection of variables in any date type, find the minimum/maximum value in the collection.
: In general: Boolean < Number < Timestamp < Text < Blob/Clob < List < Struct < Bag, as usually, `NULL` and `MISSING` are ignored by the comparator.

Signature
: `MIN(any) -> any`
: `MAX(any) -> any`

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

Description
: Given a collection of numeric type values, evaluates the sum of the input expression values.

Signature
: `SUM(collection<NUMERIC | MISSING | NULL>) -> NUMERIC OR NULL`

Example
: 

```SQL
SUM(<< 1, 2, 3 >>)         -- 6
SUM(<< 1, 2,`3.0d0` >>)    -- 6.0
SUM(<< 1, 2, `3.0e0` >>)   -- 6.0e0
SUM(<< 1, 2, 3, '1' >>)    -- !! ERROR !!
SUM(<< MISSING >>)         -- NULL
```
