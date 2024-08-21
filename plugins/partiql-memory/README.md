# PartiQL Memory Plugin

This is a plugin for defining namespaces and tables in memory.

## Usage

```kotlin
// define the data and types.
val catalog = MemoryCatalog.builder()
    .name("hello")
    .defineTable("pi", MemoryTable(
        type = PType.real(),
        data = ionFloat(3.14),
    ))
    .build()

// create a connector to be used in a session
val connector = MemoryConnector.from(catalog)
```
