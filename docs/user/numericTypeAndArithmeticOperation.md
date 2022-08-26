# Numeric Data Type

## Static Type:

| Name	    | Storage	  | Description	                                                        | Range	                                       |
|----------|-----------|---------------------------------------------------------------------|----------------------------------------------|
| INT2	    | 2 Bytes	  | small-range Integer	                                                | -32768 to +32767	                            |
| INT4	    | 4 Bytes 	 | mid-range Integer	                                                  | -2147483648 to +2147483647	                  |
| INT8	    | 8 Bytes	  | large-range Integer; Default Integer Choice	                        | -9223372036854775808 to 9223372036854775807	 |
| INT	     | variable	 | User-specified precision; Default 8	                                | Arbitrary Precision Numbers	                 |
| Float	   | 8 byte	   | IEEE 754 binary floating point values; variable-precision, inexact	 | 15 decimal digits precision	                 |
| Decimal	 | variable	 | user-specified precision, exact	                                    | Arbitrary Precision Numbers	                 |

PartiQL also support `Positive Infinity, Negative Infinity, and NaN` as `FLOAT` as specified in IEEE-754.

## Mathematical Operation

Unless otherwise specified, a mathematical operator: 
1) takes a field name or expression of a numeric data type as operand
2) For Unary operation, the result type will be the same as the operand
3) For Binary operation, and the two operands are of the same type, the result will be the same as the type of operands.
4) For Binary operation, and the two operands are not of the same type, PartiQL will attempt to automatically coerce the operand.
5) If one or more operands are MISSING, then the result will be missing. else if one or more operands are null, then the result will be null.

### Overflow
With Type Inferencer and runtime type check enabled, Integer constraint will be honored and we check if the result of the mathematical operation exceeds the range that can be represented with the result type. 

Without type inferencer and runtime type check, the default runtime integer representation is `INT8`, and overflow can still happen if the result exceed the range that can be represented with the `INT8` type.

When Permissive mode is enabled, overflowed values will be shown as `MISSING` in the output. 

### Conversion Map
Operators involving multiple argument data types, such as Integer + Float, the conversion map determines the datatype PartiQL uses. Decimal has the highest numeric precedence, followed by float, and finally by INT.

If either operand has type of Decimal, then PartiQL will attempt to convert the operands implicitly to Decimal before performing the operation.
If none of the operand has Decimal type but any of the operands is Float, then PartiQL will attempt to convert the operands implicitly to Float before performing the operation.

### HonorParameter
If precision and scale matter, i.e. doing operation on monetary value, make sure to turn on the honorTypedOpParameters() option in Compile Option. 

The honorTypedOpParameters() determines how CAST and other typed operations behave. The default CompileOptions uses LEGACY which ignores the additional type arguments. Using the HONOR_PARAMETERS mode will take into account type parameters.

### Unary Plus:
Returns the operand without operation.

Syntax
: ` + expression`

Example
:

```sql
    + MISSING -- missing
    + NULL    -- null
    + 1       -- 1
```

### Unary Minus:
Returns the negation of the operand.

Note: Negation on the `most negative` value may cause overflow if the underlying type utilizes 2's complement.

Syntax
: `- expression`

Example
:

```sql
    - (-1) -- 1
    - (MISSING) -- MISSING
    - (NULL) -- NULL
    - (-MIN_INT8) -- MISSING
    -- with Type Inferencer Enabled
    - CAST(MIN_INT2 AS INT2) -- MISSING
```

### Addition:
Returns the sum of the two expressions.

Note: Addition may cause overflow.

Syntax
: `expression + expression`

Example
:

```sql
    1 + 1 -- 2
    1 + 1.0 -- 2.0
    1 + `1e0` -- `2e0`
    1 + MISSING -- MISSING
    1 + NULL -- NULL
    MISSING + NULL -- MISSING
    MAX_INT8 + 1 -> MISSING
    -- with Type Inferencer Enabled
    CAST(MAX_INT2 AS INT2) + CAST(1 AS INT2) --  MISSING
    -- Legacy Mode
    CAST(1 AS DECIMAL(2,1)) + CAST(1 AS DECIMAL(3,2)) -- 2 (DECIMAL OF PRECISION 1 and SCALE 0)
    -- Honor Parameter Mode
    CAST(1 AS DECIMAL(2,1)) + CAST(1 AS DECIMAL(3,2)) -- 2.00
```

Operand
: The field name or expression of a numeric data type

### Subtraction:
Returns the subtraction result of two expressions.

Note: Subtraction may cause overflow, in `Permissive` mode, the result will be missing.

Syntax
: `expression - expression`

Example
:

```sql
    1 - 1 -- 0
    1 - 1.0 -- 0.0
    1 - `1e0` -- `0e0`
    1 - MISSING -- MISSING
    1 - NULL -- NULL
    MISSING - NULL -- MISSING
    MIN_INT8 - 1 -> MISSING
    -- with Type Inferencer Enabled
    CAST(MIN_INT2 AS INT2) - CAST(1 AS INT2) -> MISSING --  MISSING
    -- Legacy Mode
    CAST(1 AS DECIMAL(2,1)) - CAST(1 AS DECIMAL(3,2)) -- 0 (DECIMAL OF PRECISION 1 and SCALE 0)
    -- Honor Parameter Mode
    CAST(1 AS DECIMAL(2,1)) - CAST(1 AS DECIMAL(3,2)) -- 0.00
```

### Multiplication:

Returns the multiplication result of two expressions.

Syntax
: `expression * expression`

Example
: 

```sql
    1 * 2 -- 2
    1 * 2.0 -- 2.0
    1 * `2e0`-- `2e0`
    MISSING * 1 -- MISSING
    NULL * 1 -- NULL
    MISSING * NULL -- MISSING
    MIN_INT8 * 2 -- MISSING
    -- with Type Inferencer Enabled
    CAST(MIN_INT2 AS INT2) * CAST(2 AS INT2) -- MISSING  
    -- In LEGACY MODE
    CAST(1 as decimal(3,2)) * CAST(2 as DECIMAL(3,2)) -- 2
    -- IN HONOR PARAMETER MODE
    CAST(1 as decimal(3,2)) * CAST(2 as DECIMAL(3,2)) -- 2.0000
```
: `1 * 2 -> 2`
: `MIN_INT8 * 2 -> MISSING`
: `CAST(MIN_INT2 AS INT2) * CAST(2 AS INT2) -> MISSING // with Type Inferencer Enabled`

### Division:
Returns the division result of two expressions.

Note: for Integer Type, the division result will be round towards zero.

Syntax
: `expression / expression`

Example
: 

```sql
    5 / 2 --2
    -5 / 2 -- -2
    5 / 2.0 -- 2.5
    5 / `2e0` -- `2.5e0`
    MISSING / 1 -- MISSING
    NULL / 1 -- NULL
    MISSING / NULL -- MISSING
    -- In LEGACY MODE
    CAST(1 as decimal(3,2)) / CAST(2 as DECIMAL(3,2)) -- 0.5
    -- IN HONOR PARAMETER MODE
    CAST(1 as decimal(3,2)) * CAST(2 as DECIMAL(3,2)) -- 0.5
```

### Modulo
Returns the remainder of a division

Syntax
: `expression % expression`

Example
: 

```sql
    5 % 2 -- 1
    5 % 2.0 -- 1.0
    5 % `2e0` -- `1e0`
    MISSING % 1 -- MISSING
    NULL % 1 -- NULL
    MISSING % NULL -- MISSING
    -- In LEGACY MODE
    CAST(5 as decimal(3,2)) % CAST(2 as DECIMAL(3,2)) -- 1
    -- IN HONOR PARAMETER MODE
    CAST(5 as decimal(3,2)) % CAST(2 as DECIMAL(3,2)) -- 1.00
```