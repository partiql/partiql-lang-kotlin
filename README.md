
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-lang/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-lang)
[![License](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/partiql/partiql-lang-kotlin/blob/main/LICENSE)
[![CI Build](https://github.com/partiql/partiql-lang-kotlin/actions/workflows/build.yml/badge.svg)](https://github.com/partiql/partiql-lang-kotlin/actions?query=workflow%3A%22Build+and+Report+Generation%22)
[![codecov](https://codecov.io/gh/partiql/partiql-lang-kotlin/branch/main/graph/badge.svg)](https://codecov.io/gh/partiql/partiql-lang-kotlin)

# PartiQL Lang Kotlin

This is a Kotlin implementation of the [PartiQL specification](https://partiql.org/assets/PartiQL-Specification.pdf).
PartiQL is based on SQL:1999 and has added support for working with schemaless hierarchical data.
PartiQL’s extensions to SQL are easy to understand, treat nested data as first class citizens and
compose seamlessly with each other and SQL.

This repository contains an embeddable reference interpreter, test framework, and tests for PartiQL in Kotlin.

## About

Check out the [PartiQL website](https://www.partiql.org) for documentation, usage guides, and more!

## Status

Users of PartiQL should consider the PartiQL library to be stable. It has been leveraged within a number of Amazon internal
systems and AWS products for multiple years. The behavior of the language itself is mostly stable.

## Using In Your Project

This project is published to [Maven Central](https://central.sonatype.com/artifact/org.partiql/partiql-lang).

| Group ID      | Artifact ID    | Recommended Version |
|---------------|----------------|---------------------|
| `org.partiql` | `partiql-lang` | `1.0.0`             |

For Maven builds, add the following to your `pom.xml`:

```xml
<dependency>
  <groupId>org.partiql</groupId>
  <artifactId>partiql-lang</artifactId>
  <version>${version}</version>
</dependency>
```

For Gradle 5+, add the following to your `build.gradle`:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "org.partiql:partiql-lang:${version}"
}
```

Be sure to replace `${version}` with the desired version.

## Building

**Pre-requisite**: Building this project requires Java 11+.

This project uses a [git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules) to pull in 
[partiql-tests](https://github.com/partiql/partiql-tests). The easiest way to pull everything in is to clone the 
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
