# PartiQL Code Examples

Code examples on how to use the PartiQL reference implementation library can be found in both Java and Kotlin. To 
run an example execute the command from the project root folder: 

```
$ ./gradlew :examples:run -q --args="<example class names>"
``` 

For example:

```
$ ./gradlew :examples:run -q --args="SimpleExpressionEvaluation"
```

Multiple examples can be ran at the same time, example: 
```
$ ./gradlew :examples:run -q --args="SimpleExpressionEvaluation ParserExample"
```

List of Examples: 
* Kotlin:
    * CsvExprValueExample: how to create an `ExprValue` for a custom data format, in this case CSV  
    * CustomFunctionsExample: how to create and register user defined functions (UDF)
    * CustomProceduresExample: how to create and register stored procedures
    * EvaluationWithBindings: query evaluation with global bindings
    * EvaluationWithLazyBindings: query evaluation with global bindings that are lazily evaluated
    * ParserErrorExample: inspecting errors thrown by the `Parser`
    * ParserExample: how to parse a query and serialize the query AST
    * PartialEvaluationVisitorTransform: simple visitor transform example that partially evaluates simple operations like `1+1`
    * PreventJoinVisitor: visitor example to validate a query, in this case it prevents queries with `JOIN`
    * SimpleExpressionEvaluation: how to run a simple query
* Java:
    * CSVJavaExample: similar to CsvExprValueExample but implemented in Java
    * S3Example: running queries from data stored in a S3 bucket. To run this example it is necessary to create a S3 
    bucket, upload a file and change the example code to point to that bucket. Please look at the source code of 
    S3Example and its documentation on how to adapt the test for your needs. 
 