# PartiQL Kotlin Test Runner

This package checks whether the conformance tests (in [partiql-tests](https://github.com/partiql/partiql-tests)) run 
using the `partiql-lang-kotlin` implementation return the correct result.

This package enables:
1. Verifying conformance tests are defined correctly (e.g. verify evaluation environment is not missing a table)
2. Identifying areas in the Kotlin implementation that diverge from the [PartiQL Specification](https://partiql.org/assets/PartiQL-Specification.pdf)


## Run Conformance Tests Locally

```shell
# default, test data from partiql-tests submodule will be used
./gradlew :test:partiql-tests-runner:test --tests "*ConformanceTestsReportRunner" -PconformanceReport

# override test data location
PARTIQL_TESTS_DATA=/path/to/partiql-tests/data \
./gradlew :test:partiql-tests-runner:test --tests "*ConformanceTestsReportRunner" -PconformanceReport
```
The report is written into file `test/partiql-tests-runner/conformance_test_results.ion`.

## Run Conformance Tests in IntelliJ

The above project property `-PconformanceReport` is checked in `test/partiql-tests-runner/build.gradle.kts`,
to prevent the conformance test suite from executing during a normal project-build test run. 
Unfortunately, this also disables running `ConformanceTestsReportRunner` via IntelliJ UI for unit tests. 
To make that possible locally, temporarily comment out the check in `test/partiql-tests-runner/build.gradle.kts`.

## Compare Conformance Reports locally

```shell
./gradlew :test:partiql-tests-runner:run --args="pathToFirstConformanceTestResults pathToSecondConformanceTestResults firstCommitId secondCommitId pathToComparisonReport"
```
The last argument, `pathToComparisonReport` is for the `.md` file into which to write the comparison report.
