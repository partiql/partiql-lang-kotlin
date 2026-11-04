# PartiQL Function Extensions

This separately published module provides executable, reusable functions that are not part of PartiQL's core built-ins.
It depends on `partiql-spi`, but does not register or expose any SQL name automatically.

## Dependency

Use the same version as the other PartiQL artifacts in the application:

```kotlin
dependencies {
    implementation("org.partiql:partiql-function-extensions:<version>")
}
```

The `partiql-spi` dependency is exposed transitively.

## Functions

The `compat` package preserves selected pre-1.0 PLK behavior. The `custom` package contains general-purpose extensions.

| Package | Function | Signature |
| --- | --- | --- |
| `compat.datetime` | `date_add` | `(string, integer, timestamp) -> timestamp` |
| `compat.datetime` | `utcnow` | `() -> timestamp` |
| `custom.collection` | `contains` | `(array, string) -> bool` |
| `custom.conversion` | `hex_to_bigint` | `(string) -> bigint` |
| `custom.datetime` | `date_trunc` | `(string, timestamp) -> timestamp`, `(string, date) -> timestamp` |
| `custom.datetime` | `to_unixtime` | `(timestamp) -> bigint` |
| `custom.math` | `pow` | `(T, T) -> double precision`, where `T` is a numeric primitive type |

Each class exposes an immutable `overloads()` list. A host selects the overloads and SQL namespaces it wants:

```kotlin
import org.partiql.extensions.functions.compat.datetime.UtcNow
import org.partiql.extensions.functions.custom.math.Pow
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Namespace
import org.partiql.spi.catalog.RoutineCatalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.function.MemRoutineProvider

val provider = MemRoutineProvider.builder().apply {
    UtcNow.overloads().forEach { register(it, Namespace.of("datetime")) }
    Pow.overloads().forEach { register(it, Namespace.of("math")) }
}.build()

val catalog = object : RoutineCatalog {
    override fun getName(): String = "app"

    override fun resolveFunctions(session: Session, identifier: Identifier) =
        provider.getFunctions(identifier)

    override fun resolveAggregations(session: Session, identifier: Identifier) =
        provider.getAggregations(identifier)
}

val session = Session.builder()
    .catalog(catalog.getName())
    .catalogs(catalog)
    .path(Namespace.of("app", "datetime"), Namespace.of("app", "math"))
    .build()
```

A host with an existing `RoutineCatalog` should compose this provider with every other routine source its resolution
methods expose; those methods are authoritative for that catalog. With this session, the selected functions are
available by unqualified name or by their full names, such as `app.datetime.utcnow()` and `app.math.pow(2, 3)`.
