# PartiQL Kotlin Test Runner

This package checks whether the conformance tests (in [partiql-tests](https://github.com/partiql/partiql-tests)) run 
using the `partiql-lang-kotlin` implementation return the correct result.

This package enables:
1. Verifying conformance tests are defined correctly (e.g. verify evaluation environment is not missing a table)
2. Identifying areas in the Kotlin implementation that diverge from the [PartiQL Specification](https://partiql.org/assets/PartiQL-Specification.pdf)


## Run Conformance Tests Locally

```shell
# default, test data from partiql-tests submodule will be used
./gradlew :test:partiql-tests-runner:ConformanceTestReport

# override test data location
PARTIQL_TESTS_DATA=/path/to/partiql-tests/data \
./gradlew :test:partiql-tests-runner:ConformanceTestReport
```
The report is written into folder `test/partiql-tests-runner/build/conformance_test_results`.

## Compare Conformance Reports locally
The report contains two type of comparison: 
1. Cross Commit: Comparing using the same engine based on the pull request commit and head of target branch commit. 
2. Cross Engine: Comparing using different engine based on the pull request commit. 

```shell
./gradlew :test:partiql-tests-runner:run --args="pathToFirstConformanceTestResults pathToSecondConformanceTestResults firstCommitId secondCommitId pathToComparisonReport"
```
The last argument, `pathToComparisonReport` is for the `.md` file into which to write the comparison report.
