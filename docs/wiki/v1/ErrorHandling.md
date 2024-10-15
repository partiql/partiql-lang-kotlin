# Usage Guide: Error Handling

## Introduction

The PartiQL library provides a robust error reporting mechanism, and this usage guide aims to show
how you can leverage the exposed APIs.

## Who is this for?

This usage guide is aimed at developers who use any one of [PartiQL's components](https://github.com/partiql/partiql-lang-kotlin/wiki/TODO) for their
application. If you are looking for how to change how errors are reported in the CLI, please run: `partiql --help`.

To elaborate on why this usage guide may be useful to you, the developer, let us assume that your
company provides a CLI to enable your customers to execute PartiQL queries. When a user is typing a query and
references a table that doesn't exist, your CLI might want to highlight that error and halt processing of the
query to save on computational costs. Or, your CLI might want to highlight the error but continue processing the query to accumulate
errors to better enable the developer to see all of their mistakes at once. In any case, the PartiQL
library allows developers to register their own error listeners to take control over their customers' experience.

## Error Listeners

Each major component (parser, planner, compiler) of the PartiQL Library allows for the registration of an `ErrorListener`
that will receive every warning/error that the particular component emits. The default error listener aborts the
component's execution, by throwing an `ErrorListenerException`, upon encountering the first error. This behavior aims to
protect developers who might have decided to avoid reading this documentation. However, as seen further below, this is
easy to override.

## Halting a Component's Execution

In the scenario where you want to halt one of the components when a particular warning/error is emitted, error listeners
have the ability to throw an `ErrorListenerException`. This exception acts as a wrapper over any exception you'd like to
halt with. For example:

```java
import org.partiql.spi.errors.PError;
import org.partiql.spi.errors.PErrorListener;

import java.lang.annotation.Native;

class AbortWhenAlwaysMissing extends ErrorListener {
    // This is to be used to halt my application after the component finishes execution
    boolean hasErrors = false;

    @Override
    void error(@NotNull Error error) throws ErrorListenerException {
        System.out.println("e: " + getErrorMessage(error));
        hasErrors = true;
    }

    @Override
    void warning(@NotNull Error error) throws ErrorListenerException {
        if (error.getCode() == Error.ALWAYS_MISSING) {
            Exception cause = new IllegalStateException("This system does not allow for expressions that always return missing!");
            throw new ErrorListenerException(cause);
        }
        println("w: " + getErrorMessage(error));
    }

    private fun getErrorMessage(@NotNull Error error) {
        // Internal implementation details
    }
}
```

**NOTE**: If you throw an exception that is not an `ErrorListenerException`, the component that contains your registered
`ErrorListener` will catch the exception and send an error to your `ErrorListener` with a code of
`Error.INTERNAL_ERROR`. This will lead to a duplication of errors (which can be a bad experience for your
customers).

## Registering Error Listeners

Each component allows for the registration of a custom error listener upon instantiation. For example, let's say you
intend on registering the `AbortWhenAlwaysMissing` error listener from above:
```java

public class Foo {
    public static void main(String[] args) {
        // Error Listener
        ErrorListener listener = AbortWhenAlwaysMissing();

        // User Input
        String query = args[0];
        Statement ast = parse(query);

        // Planner Component
        PartiQLPlanner planner = PartiQLPlanner.standard();
        PlannerConfig plannerConfig = PlannerConfigBuilder().setErrorListener(listener).build(); // Registration here!!

        // Planning and catching the ErrorListenerException
        Plan plan;
        try {
            plan = planner.plan(ast, plannerConfig);
        } catch (ErrorListenerException ex) {
            throw ex.cause;
        }

        // Do more ...
    }

    private Statement parse(String query) {
        // Calling the PartiQL Parser, handling ErrorListenerExceptions, etc.
    }
}
```

## Errors and Warnings

Errors and warnings are both represented by the same data structure, an `Error`. In the case of an error/warning, it is
up to the respective component to correctly send the `Error` to either `ErrorListener.error()` or
`ErrorListener.warning()`.

The `Error` Java class allows for developers to introspect its properties to determine how to create their own error
messages. See the [Javadocs] for the available methods.

## Writing Quality Error Messages

As mentioned above, the `Error` Java class exposes information for database implementers to write high quality error
messages. Specifically, `Error` exposes a method, `int getCode()`, to return the enumerated error code received. All
possible error codes are represented as static final fields in
the [Error Javadocs](https://github.com/partiql/partiql-lang-kotlin/wiki/TODO).

Each error code's documentation highlights the _potentially available_ properties that the `Error` may hold. These
properties are NOT guaranteed to exist, and their values are subject to change between PartiQL versions. Please read
each error code's Javadocs before using them in your application.

Now, here's an example of how you might write a quality error message:
```java
public class ConsoleErrorListener extends ErrorListener {

    boolean hasErrors = false;

    @Override
    void error(@NotNull Error error) throws ErrorListenerException {
        String message = getMessage(error, "e: ");
        System.out.println(message);
        hasErrors = true;
    }

    @Override
    void warning(@NotNull Error error) throws ErrorListenerException {
        String message = getMessage(error, "w: ");
        System.out.println(message);
    }

    static String getMessage(@NotNull Error error, @NotNull String prefix) {
        switch (error.getCode()) {
            case Error.ALWAYS_MISSING:
                Integer line = (Integer) error.getProperty(Property.LINE_NO);
                Integer column = (Integer) error.getProperty(Property.COL_NO);
                String location = getNullSafeLocation(line, column);
                return prefix + location + " Expression always evaluates to missing.";
            case Error.FEATURE_NOT_SUPPORTED:
                String name = (String) error.getProperty(Property.FEATURE_NAME);
                if (name == null) {
                    name = "UNKNOWN";
                }
                return prefix + "Feature (" + name + ") not yet supported.";
            default:
                return "Unhandled error code received.";
        }
    }

    String getNullSafeLocation(Integer line, Integer col) {
        // Internal implementation
    }
}
```

## A Component's Output Structures

Each of PartiQL's components produce a structure for future use. The parser outputs an AST, the planner outputs a plan,
and the compiler outputs an executable. What happens when any of the components experience an error/warning?

The answer, as is often in software, depends. Since this error reporting mechanism allows developers to register error
listeners that accumulate all errors, the PartiQL components still continue processing until terminated by an error
listener. That being said, when error listeners receive an error, one must assume that the output of the component
is a dud and is incorrect. Therefore, if the parser has produced errors with a malformed AST, you shouldn't pass
the AST to the planner to continue evaluation.

However, if warnings have been emitted, the output can still be safely relied upon. For example, let's use the same
error listener we wrote further above:
```java
class Example {

    public Plan planInternal(Statement ast) throws PlanningFailure {
        AbortWhenAlwaysMissing listener = AbortWhenAlwaysMissing();
        PartiQLPlanner planner = PartiQLPlanner.standard();
        PlannerConfig plannerConfig = PlannerConfigBuilder().setErrorListener(listener).build();

        Plan plan;
        try {
            plan = planner.plan(ast, plannerConfig);
        } catch (ErrorListenerException ex) {
            throw new PlanningFailure(ex);
        }
        // If an error has been reported to the listener, implementers
        // should NOT trust the plan that has been returned.
        if (listener.hasErrors) {
            throw new PlanningFailure("Errors encountered. Exiting.");
        }
        return plan;
    }
}
```

## What about Execution?

Error listeners are specifically meant to provide control over the reporting of errors for PartiQL's major components (parser,
planner, and compiler). However, for the execution of compiled statements, PartiQL still provides errors (and error codes)
by throwing an `EvaluationException` which exposes a method, `Error getError()`. The `EvaluationException` does not
expose a message, cause, or stacktrace.

Here is an example of how you can leverage this functionality below:
```java
class MyApplication {
    void executeAndPrint(PreparedStatement stmt, Session session) {
        Datum lazyData;
        try {
            lazyData = stmt.execute(session);
            // Iterate through the lazyData and print to the console.
        } catch (EvaluationException e) {
            System.out.println(ConsoleErrorListener.getMessage(e.getError(), "e: "));
        }
    }
}
```

## Reference Implementations

The PartiQL CLI offers multiple ways to process warnings/errors. See the flags `-Werror`, `-w`,
`--max-errors`, and more when you run `partiql --help`. See the CLI Usage Guide
[here](https://github.com/partiql/partiql-lang-kotlin/wiki/TODO). The implementation details can be found in the
[CLI subproject](https://github.com/partiql/partiql-lang-kotlin/wiki/TODO).
