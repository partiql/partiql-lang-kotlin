# PartiQL Kotlin Test Runner

This package checks whether the conformance tests (in [partiql-tests](https://github.com/partiql/partiql-tests)) run 
using the `partiql-lang-kotlin` implementation return the correct result.

This package enables:
1. Verifying conformance tests are defined correctly (e.g. verify evaluation environment is not missing a table)
2. Identifying areas in the Kotlin implementation that diverge from the [PartiQL Specification](https://partiql.org/assets/PartiQL-Specification.pdf)

Eventually, the `partiql-test-runner` module will replace the `partiql-pts` and `partiql-testscript` modules along with some other 
tests in `lang` that were ported to `partiql-tests` 
(see [partiql-lang-kotlin#789](https://github.com/partiql/partiql-lang-kotlin/issues/789)).
