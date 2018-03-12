
This is the template for writing documentations for an IonSQL++ built-in function. 

The template uses Pandoc's definition lists feature https://pandoc.org/MANUAL.html#definition-lists

There are 5 parts to a function's documentation 

1. One sentence statement -- much like the first sentence of a Java method's Javadoc
1. Signature -- the type signature of the function should use data types already defined on define a local data type using a WHERE clause 
1. Header -- function with formal parameters only that we will use in the next step 
1. Purpose -- english explanation of what the function does referring to formal argument names from Header. State any pre- and post-conditions as well as any exceptions to default behaviour e.g., propagation of unknowns `null` and `missing` 
1. Examples -- examples and their expected results. Make sure the examples cover the preceding explanations and **always** include examples for uncommon behaviour 

Here is an example for the imaginary function `add` 

### ADD

Given 1 or more values return ther sum 


Signature
: `ADD: PosInt PosInt -> PosInt`

where `PosInt` is a positive `Integer`

Header
: `ADD(v1, v2 ... vn)`

Purpose
: Given 1 or more values `v1 .. vn` return their sum. The summation proceeds from left-to-right. If any of the values passed is **not** a `PosInt` the 
function returns the current sum up to that point. 
  
Examples
: 
<!-- intentional blank line to make pdf generation work -->                     
```sql  
ADD(1)         -- 1 (wrap extra explanations with parens)
ADD(1,2)       -- 3
ADD(1,2,"a",3) -- 3
ADD()          -- 0
ADD("a")       -- 0
```

