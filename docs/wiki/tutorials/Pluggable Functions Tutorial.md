# Pluggable Functions Tutorial

The PartiQL Command Line Interface (CLI and REPL) supports loading user-defined scalar functions (UDFs) without the need of recompiling the tool. This document will guide you through creating custom functions and how to load them.

## Step 1: Add Dependencies and Build Your Module

To build your module, you can use build automation tools like Maven or Gradle. These tools will handle the dependency management and compilation process for you. Here's how you can specify the necessary dependencies:

```Kotlin
dependencies {
    implementation("org.partiql:partiql-spi:<latest_version>")
    implementation("org.partiql:partiql-types:<latest_version>")
}
```

## Step 2: Create a Custom Function

To create a custom function, you need to implement the `PartiQLFunction` interface. This interface allows you to define the function's behavior and its signature, including its name, return type, parameters, determinism, and optional description. If you're unfamiliar with the concept of determinism, consider linking to the [SQL-99 specification](http://web.cecs.pdx.edu/~len/sql1999.pdf) as it can provide clarity on the topic.

Here's a basic template to help you start:

```Kotlin
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.PartiQLValueType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.stringValue

@OptIn(PartiQLFunctionExperimental::class)
object TrimLead : PartiQLFunction {
    override val signature = FunctionSignature(
        name = "trim_lead", // Specify your function name
        returns = PartiQLValueType.STRING, // Specify the return type
        parameters = listOf(
        FunctionParameter.ValueParameter(name = "str", type = PartiQLValueType.STRING) // Specify parameters
        ),
        isDeterministic = true, // Specify determinism
        description = "Trims leading whitespace of a [str]." // A brief description of your function
    )

    @OptIn(PartiQLValueExperimental::class)
    override operator fun invoke(session: ConnectorSession, arguments: List<PartiQLValue>): PartiQLValue {
        // Implement the function logic here
        val str = (arguments[0] as? StringValue)?.string ?: ""
        val processed = str.trimStart()
        return stringValue(processed)
    }
}
```

Ensure that you replace the signature and function invoking with your actual implementations.

## Step 3: Implement the Plugin Interface

Next, you need to implement the `Plugin` interface in your code. This allows you to return a list of all the custom `PartiQLFunction` implementations you've created, using the `getFunctions()` method. This step is crucial as it allows the service loader to retrieve all your custom functions.

Here's an example of a `Plugin` implementation:

```Kotlin
package org.partiql.plugins.mockdb

import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.function.PartiQLFunction

public class LocalPlugin implements Plugin {
    override fun getConnectorFactories(): List<Connector.Factory> = listOf()

    @PartiQLFunctionExperimental
    override fun getFunctions(): List<PartiQLFunction> = listOf(
        TrimLead // Specify the functions
    )
}
```

## Step 4: Create Service Provider Configuration file

In order for the Java's ServiceLoader to recognize your plugin and load your custom functions, you'll need to specify your `Plugin` implementation in a Service Provider Configuration file:

1. In your project's main resources directory (usually `src/main/resources`), create a new directory named `META-INF/services`.

2. Within this directory, create a new file named after the full interface name as `org.partiql.spi.Plugin`.

3. Inside this file, write the fully qualified name of your Plugin implementation class. If you have multiple implementations, each should be listed on a new line.

Here's an example if your `Plugin` implementation class is `org.partiql.plugins.mockdb.LocalPlugin`:
```Kotlin
org.partiql.plugins.mockdb.LocalPlugin
```

Note: If you're working with nested classes, Java requires you to use the `$` delimiter between the outer class name and the nested class name. For a deeper understanding and to avoid potential naming errors, please refer to the [official Java documentation](https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1) on class naming.

## Step 5: Creating and Packaging a Custom PartiQL CLI Plugin in a .jar File

1. Compile and Package: You need to compile your function and `Plugin` implementation into bytecode and then package it into a .jar file. This .jar file will be used as a plugin for the PartiQL CLI.

2. Gradle Configuration:
If you're starting from scratch or using a standalone project, you'll need to make sure your Gradle project is properly set up. Ensure you apply the necessary plugins. Typically, you'll use the `application` plugin or `shadowJar`:

```Kotlin
plugins {
    id("application")
    // or
    // id("com.github.johnrengelman.shadow") version "X.X.X"
}
```

These plugins ensure that you have the required tasks to assemble a jar+distribution or a fatjar (a jar with distribution in it). The exact configuration may vary depending on your project's specifics and dependencies.

## Step 6: Load the Functions into CLI

Each of your .jar files should be stored in its own subdirectory under the `plugins` directory, which itself is inside the .partiql directory in your home directory. Here's what the directory structure should look like:

```
~/.partiql/plugins
    ├── firstPlugin
    │   └── firstPlugin.jar
    └── secondPlugin
        └── secondPlugin.jar
```

In the example above, `firstPlugin.jar` and `secondPlugin.jar` are the plugins you've created, each in its own directory under `~/.partiql/plugins`.

By default, the PartiQL CLI will search the `~/.partiql/plugins` directory for plugins. However, you can specify a different directory when starting the CLI with the `--plugins` option:

```shell
partiql --plugins /path/to/your/plugin/directory
```

With this command, the CLI will search for .jar files in the specified directory’s subdirectories and load them as plugins. This feature gives you the flexibility to organize your plugins in a manner that best suits your needs.

## Step 7: Invoke Custom Functions in CLI

To use the custom functions, you simply call them as you would any other function in the PartiQL CLI. For example, if you created a function named "trim_lead", you would invoke it like so:

```shell
partiql> trim_lead('       hello')

'hello'
```

Please replace string with the actual string you want to trim.

That's all there is to it! With this mechanism, you can introduce any number of custom functions to PartiQL, and these functions will be usable just like built-in functions, without the need to modify the PartiQL codebase. It's an excellent way to extend the capabilities of PartiQL and adapt it to the specific needs of your project or organization.
