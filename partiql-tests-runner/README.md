# PartiQL Kotlin Test Runner

This package checks whether the conformance tests (in [partiql-tests](https://github.com/partiql/partiql-tests)) run 
using the `partiql-lang-kotlin` implementation return the correct result.

This package will allow us to:
1. verify conformance tests are defined correctly (e.g. verify evaluation environment is not missing a table)
2. identify areas in the Kotlin implementation that diverge from the PartiQL specification

Eventually, the Kotlin test runner should replace the `pts` and `testscript` Gradle subprojects along with some other 
tests in `lang` that were ported to `partiql-tests` 
(see [partiql-lang-kotlin#789](https://github.com/partiql/partiql-lang-kotlin/issues/789)).
