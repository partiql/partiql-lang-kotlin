# Code Examples 

Code examples on how to use the PartiQL library inside your code base. Please consult the `kdoc` for more 
information on individual classes and interfaces.

## Java

```java
IonSystem ion = IonSystemBuilder.standard().build();

IonValue myTable = ion.newLoader()
    .load("{name: \"zoe\",  age: 12}\n" +
        "{name: \"jan\",  age: 20}\n" +
        "{name: \"bill\", age: 19}\n" +
        "{name: \"lisa\", age: 10}\n" +
        "{name: \"tim\",  age: 30}\n" +
        "{name: \"mary\", age: 19}\n");

EvaluatingCompiler compiler = new EvaluatingCompiler(ion);
Expression expression = compiler.compile("SELECT t.name FROM myTable AS t WHERE t.age > 20");

ExprValue boundValue = new IonExprValue(myTable);
Bindings myGlobalBindings = bindingName -> {
    ExprValue exprValue;
    if(BindingCase.SENSITIVE.equals(bindingName.getBindingCase())) {
        exprValue = "myTable".equals(bindingName.getName()) ? boundValue : null;
    }
    else {
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
val ion: IonSystem = IonSystemBuilder.standard().build()

val myTable: IonValue = ion.newLoader().load("""
        {name: "zoe",  age: 12}
        {name: "jan",  age: 20}
        {name: "bill", age: 19}
        {name: "lisa", age: 10}
        {name: "tim",  age: 30}
        {name: "mary", age: 19}
    """)

val compiler = EvaluatingCompiler(ion)
val expression = compiler.compile("SELECT t.name FROM myTable AS t WHERE t.age > 20")

val boundValue: ExprValue = IonExprValue(myTable)
val session = EvaluationSession.build {
    globals(Bindings.over { bindingName ->
        when (bindingName.bindingCase) {
            BindingCase.SENSITIVE   -> if (bindingName.name == "myTable") boundValue else null 
            BindingCase.INSENSITIVE -> if (bindingName.name.equals("myTable", ignoreCase = false)) boundValue else null
        }
    })
}

val exprValue = expression.eval(session)
val queryResultAsIon = exprValue.ionValue
```
