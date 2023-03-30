# Aggregate Functions

Aggregate functions output a single result value from a collection of input values. 

Unless otherwise specified, aggregate functions will skip `NULL` and `MISSING`.

## AVG

Description
: Given a collection of numeric type values, compute their arithmetic mean.

Signature
: `AVG(collection<NUMERIC | NULL | MISSING>) -> DECIMAL or NULL`

Examples
: 

```sql
AVG(<< 1, 2, 3 >>)          -- 2 `decimal(1, 0)`
AVG(<< MISSING >>)          -- NULL
AVG(<< 1, 2, 3, MISSING >>) -- 2
```

## COUNT

Description
: Given a collection of values of any data type, calculate how many there are in the collection.
Note: `COUNT(expr)` will skip the null and missing value, but COUNT(*) will not.

Signature
: `COUNT(*) -> INT8`

Signature
: `COUNT(any) -> INT8`

Examples
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
: Given a collection of values of any data type, find the minimum/maximum value in the collection.
: In general: Boolean < Number < Timestamp < Text < Blob/Clob < List < Struct < Bag, as usually, `NULL` and `MISSING` are ignored by the comparator.

Signature
: `MIN(any) -> any`

Signature
: `MAX(any) -> any`

Examples
: 

```sql
MIN(<< 1, 2, 3 >>)            -- 1
MIN(<< '1', 2, 3 >>)          -- 2
MIN(<< '1', 2, 3, MISSING >>) -- 2

MAX(<< 1, 2, 3 >>)            -- 3
MAX(<< '1', 2, 3 >>)          -- '1'
MAX(<< '1', 2, 3, MISSING >>) -- '1'
```

## SUM

Description
: Given a collection of numeric type values, compute their sum.

Signature
: `SUM(collection<NUMERIC | MISSING | NULL>) -> NUMERIC or NULL`

Examples
: 

```sql
SUM(<< 1, 2, 3 >>)         -- 6
SUM(<< 1, 2,`3.0d0` >>)    -- 6.0
SUM(<< 1, 2, `3.0e0` >>)   -- 6.0e0
SUM(<< 1, 2, 3, '1' >>)    -- !! ERROR !!
SUM(<< MISSING >>)         -- NULL
```

## EVERY

Description
: Returns true iff all items in the collection (excluding `NULL` and `MISSING`) are true. 
Requires all items to be booleans or undefined (`NULL` and `MISSING`). 
Returns `NULL` on an empty collection.

Examples
:

```sql
EVERY(<< true, false, true >>)         -- false
EVERY([ 1 < 5, true, NULL IS NULL])    -- true
EVERY(<< NULL, 2<3, MISSING, true >>)  -- true
EVERY([NULL, MISSING])                 -- NULL
EVERY(<< >>)                           -- NULL
EVERY(<< true, 5, true >>)             -- !! ERROR !!
```