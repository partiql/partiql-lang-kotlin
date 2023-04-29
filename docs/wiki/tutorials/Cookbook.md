# Code Examples 

Code examples on how to use the PartiQL library inside your code base. Please consult the `kdoc` for more 
information on individual classes and interfaces.

## Java

```java
IonSystem ion = IonSystemBuilder.standard().build();

String myTable = "[{name: \"zoe\",  age: 12},\n" +
        "{name: \"jan\",  age: 20},\n" +
        "{name: \"bill\", age: 19},\n" +
        "{name: \"lisa\", age: 10},\n" +
        "{name: \"tim\",  age: 30},\n" +
        "{name: \"mary\", age: 19}]";

CompilerPipeline pipeline = CompilerPipeline.standard(ion);

Expression expression = pipeline.compile("SELECT t.name FROM myTable AS t WHERE t.age > 20");

ExprValue boundValue = pipeline.getValueFactory().newFromIonValue(ion.singleValue(myTable));
Bindings myGlobalBindings = bindingName -> {
    ExprValue exprValue;
    if (BindingCase.SENSITIVE.equals(bindingName.getBindingCase())) {
        exprValue = "myTable".equals(bindingName.getName()) ? boundValue : null;
    } else {
        exprValue = "myTable".equalsIgnoreCase(bindingName.getName()) ? boundValue : null;
    }
    
    return exprValue;
};

EvaluationSession session = EvaluationSession.builder()
    .globals(myGlobalBindings)
    .build();

ExprValue exprValue = expression.eval(session);
IonValue queryResultAsIon = exprValue.getIonValue();
```

## Kotlin
```kotlin
val ion = IonSystemBuilder.standard().build()

val myTable = """[
        {name: "zoe",  age: 12},
        {name: "jan",  age: 20},
        {name: "bill", age: 19},
        {name: "lisa", age: 10},
        {name: "tim",  age: 30},
        {name: "mary", age: 19}]
    """

val pipeline = CompilerPipeline.standard(ion)
val expression = pipeline.compile("SELECT t.name FROM myTable AS t WHERE t.age > 20")

val boundValue = pipeline.valueFactory.newFromIonValue(ion.singleValue(myTable))
val session = EvaluationSession.builder()
    .globals(Bindings.buildLazyBindings { addBinding("myTable") { boundValue } })
    .build()

val exprValue = expression.eval(session)
val queryResultAsIon = exprValue.toIonValue(ion)
```

# Data Types

### Decimal

PartiQL decimals are based on [Ion decimals] from the Ion Specification[@IonSpec] but with a maximum precision of 38
digits, numbers outside this precision range will be rounded using a round [half even strategy]. Examples:

    1.00000000000000000000000000000000000000000001 -> 1.0000000000000000000000000000000000000
    1.99999999999999999999999999999999999999999999 -> 2.0000000000000000000000000000000000000 
