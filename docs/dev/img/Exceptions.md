# Evaluation Exceptions

During evaluation the interpreter can only throw `EvaluationException`s. They fall into two categories: 
* ​Direct exceptions thrown by the evaluation of an PartiQL query, for example `No such binding: <binding name>`
* Wrapped exceptions that originate out of evaluation an PartiQL query, for example: `/ by zero` an `ArithmeticException` 
  thrown by Java when dividing a number by zero. One important thing to note is that only `T extends java.lang.Exception` 
  are wrapped by an `EvaluationException`. `T extends java.lang.Errors`, e.g. `OutOfMemoryError`, are not `Exception`s 
  and therefore not wrapped.
 
The table below lists some of the Exceptions messages thrown during evaluation, their causes and when they happen. 
This is ​**not** a complete list. Also look at `org.partiql.lang.eval.EvaluatingCompilerExceptionsTest`.

| Exception message        | Cause           | When it happens  |
| ------------------------ |-----------------| -----------------|
| ​​​​​​​​Expected `type` `value`  | `IllegalArgumentException` | Value is not of the expected type, e.g.: `1 IS NOT true` |
| Cannot Compare values: `left`, `right` | -- | Two values are not comparable, e.g.: `'A' < 2` |
| `binding` is ambiguous: `[val1, ... valn]` | -- | Impossible to determine a variable value binding, e.g: `SELECT VALUE a FROM `[{v:5}]` AS item, @item.v as a, @item.v AS a` |
| No such syntax handler for `ast node name`| -- | Unsupported ast node names, e.g.: `COUNT(*)` | 
| No such binding for: `binding name` | -- | There is no binding for that name, e.g.: `SELECT y FROM << 'el1' >> AS x` |
| Expected a single argument for exists but found: `n of args`| -- | Wrong number of arguments is passed to `exists` |
| No such function: `function name` | -- | Using an unknown function |
| RIGHT and FULL JOIN not supported | -- | There is a `RIGHT` or `OUTER` join in the query |
| Expected 2 or 3 arguments for substring instead of `n of args` | -- | Wrong arity for `substring` |
| Argument `2 or 3` of substring was not numeric | -- | 2nd or 3rd parameters are not numeric |  
| Invalid start position or length arguments to substring function | -- | `endPosition < startPosition` |
| Expected 1 argument for `functionName` instead of `n of args` | -- | 1 argument function is called with wrong arity |
| Internal error, For input string: `string` | NumberFormatException | `CAST` from String is unsuccessful  |
| / by zero | ArithmeticException | Division by zero | 
| `Not enough or Too many` arguments | -- | Wrong operation arity, e.g.: `1+1+` |

