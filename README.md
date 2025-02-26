
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-lang/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-lang)
[![License](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/partiql/partiql-lang-kotlin/blob/main/LICENSE)
[![CI Build](https://github.com/partiql/partiql-lang-kotlin/actions/workflows/build.yml/badge.svg)](https://github.com/partiql/partiql-lang-kotlin/actions?query=workflow%3A%22Build+and+Report+Generation%22)
[![codecov](https://codecov.io/gh/partiql/partiql-lang-kotlin/branch/main/graph/badge.svg)](https://codecov.io/gh/partiql/partiql-lang-kotlin)

[partiql-specification]: https://partiql.org/assets/PartiQL-Specification.pdf
[partiql-website]: https://www.partiql.org
[partiql-website-plk]: https://partiql.org/plk/1.0/
[partiql-tests]: https://github.com/partiql/partiql-tests
[plk-releases]: https://github.com/partiql/partiql-lang-kotlin/releases
[maven-partiql-namespace]: https://central.sonatype.com/namespace/org.partiql
[git-submodule]: https://git-scm.com/book/en/v2/Git-Tools-Submodules

# PartiQL Lang Kotlin

This is a Kotlin/JVM implementation of the [PartiQL specification][partiql-specification].
PartiQL is based on SQL:1999 and has added support for working with schemaless hierarchical data.
PartiQL’s extensions to SQL are easy to understand, treat nested data as first class citizens, and
compose seamlessly with SQL.

## About

Check out the [PartiQL website][partiql-website] for documentation, usage guides, and more!

## Status

Users of PartiQL should consider the PartiQL library to be stable. It has been leveraged within a number of Amazon internal
systems and AWS products for multiple years. The behavior of the language itself is mostly stable.

## Using In Your Project

All [releases][plk-releases] are published to [Maven Central][maven-partiql-namespace]. For the most up-to-date version
of our library, please add the following to your `build.gradle.kts`.

```kotlin
dependencies {
    implementation("org.partiql:partiql-lang:1.+")
}
```

For more information on how to integrate this library (or a specific subproject) into your project, please follow the
guidelines on the [partiql-lang-kotlin documentation site][partiql-website-plk] under the [PartiQL website][partiql-website].

## Building

**Pre-requisite**: Building this project requires Java 11+.

This project uses a [git submodule][git-submodule] to pull in 
[partiql-tests][partiql-tests]. The easiest way to pull everything in is to clone the 
repository recursively:

```bash
$ git clone --recursive https://github.com/partiql/partiql-lang-kotlin.git
```

You can also initialize the submodules as follows:

```bash
$ git submodule update --init --recursive
```

To build this project, from the root directory execute:

```shell
./gradlew assemble
```

This will build the reference interpreter and test framework, then run all unit and integration tests.

## Directory Structure

```
$ tree -d -L 2 -I build -I src`
.
├── buildSrc                    Gradle multi-project build
├── lib
│   └── sprout                  IR codegen
├── partiql-ast                 PartiQL ast data structures and utilities
├── partiql-cli                 CLI & Shell application
├── partiql-coverage            Code coverage library
├── partiql-eval                PartiQL compiler
├── partiql-lang                Top-level project depending on all subprojects
├── partiql-parser              PartiQL parser
├── partiql-plan                PartiQL plan data structures and utilities
├── partiql-planner             PartiQL planner
├── partiql-spi                 Common interfaces: types, values, catalogs, functions, etc.
└── test
    ├── partiql-tests           Conformance test data
    └── partiql-tests-runner    Conformance test runner
```

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md).

## License

The works contained within this repository are licensed under the Apache 2.0 License.

See the [LICENSE](LICENSE) file.
