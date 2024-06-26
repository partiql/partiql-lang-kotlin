# PartiQL Memory Plugin

This is a plugin for defining namespaces and tables in memory.

## Usage

```kotlin
// define the namespace
val namespace = MemoryNamespace.builder()
    .name("hello")
    .define("pi", MemoryTable(
        type = PType.typeFloat32(),
        data = ionFloat(3.14),
    ))
    .build()

// create a connector to be used in a session
val connector = MemoryConnector(namespace)
```
