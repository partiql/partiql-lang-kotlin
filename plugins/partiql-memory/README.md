# PartiQL In-Memory Plugin

This is a PartiQL plugin for in-memory DB. The primary purpose of this plugin is for testing. 

## Provider

The plugin is backed by a catalog provider. This enables use to easily modify a catalog for testing. 

```kotlin
val provider = MemoryCatalog.Provider()
provider[catalogName] = MemoryCatalog.of(
    t1 to StaticType.INT2,
    ...
)
```

## Catalog path

The in-memory connector can handle arbitrary depth catalog path: 

```kotlin
val provider = MemoryCatalog.Provider()
provider[catalogName] = MemoryCatalog.of(
    "schema.tbl" to StaticType.INT2,
)
```

The full path is `catalogName.schema.tbl`

The lookup logic is identical to localPlugin. 

```
|_ catalogName
   |_ schema 
     |_ tbl.ion
```