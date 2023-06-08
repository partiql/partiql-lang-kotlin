# Coding Style

All contributions to the project must comply with the guidelines set forth in this document. Some older parts of the
codebase do not follow these guidelines. If you are modifying such code, it is generally best to clean it up to comply
with the current guidelines.

### Explicit API Mode

In order to better position our developers to produce quality, public-facing code, all new PartiQL projects will enable
[strict explicit API mode](https://kotlinlang.org/docs/whatsnew14.html#explicit-api-mode-for-library-authors). In this
Kotlin compilation mode, "the compiler performs additional checks that help make the library's API clearer and more consistent".
The implications are **required** visibility modifiers and return types.

### Implementation Packages

All `internal` classes should be placed within `impl` packages to help distinguish our public APIs from the internal
codebase.

See an example below:
```text
⚬ partiql-types
└── src/main/kotlin/org/partiql/types
    ├── impl
    |  ├── StructElement.kt (internal)
    |  ├── StructType.kt (internal)
    |  ├── ...
    |  └── IntElement.kt
    ├── PartiQLElement.kt (Public Interface & Builder)
    └── PartiQLType.kt (Public Interface & Builder)
```

### Top-Level Functions

For Public APIs, the Kotlin implementation will not allow top-level functions. Similarly, due to the resulting JVM bytecode,
there shall not be `internal` top-level functions. They should be generally avoided.

### General Styling

All contributions must abide by the styling guidelines set by `ktlint`. To automatically format the codebase to comply
with `ktlint`, run:
```
./gradlew ktlintFormat
```

To check whether your contributions already comply with the style
guidelines, run:
```
./gradlew ktlintCheck
```
