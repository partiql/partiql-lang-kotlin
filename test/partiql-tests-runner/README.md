# PartiQL Kotlin Test Runner

This package checks whether the conformance tests (in [partiql-tests](https://github.com/partiql/partiql-tests)) run 
using the `partiql-lang-kotlin` implementation return the correct result.

This package enables:
1. Verifying conformance tests are defined correctly (e.g. verify evaluation environment is not missing a table)
2. Identifying areas in the Kotlin implementation that diverge from the [PartiQL Specification](https://partiql.org/assets/PartiQL-Specification.pdf)


## Run Conformance Tests Locally

```shell
# default, test data from partiql-tests submodule will be used
./gradlew :test:partiql-tests-runner:generateTestReport  

# override test data location
PARTIQL_TESTS_DATA=/path/to/partiql-tests/data \
./gradlew :test:partiql-tests-runner:generateTestReport  
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

You can also check the report via JUnitPlatform:

```shell
     open ./build/reports/tests/generateTestReport/index.html
```

## Values of type JSON (`$json`)

The runner reads and matches `$json` values as `docs/partiql-tests-schema-proposal.md` in partiql-tests describes
(added in partiql/partiql-tests#173). The reader is in `io/DatumIonReader.kt`, and the matching is in
`util/ResultMatch.kt`.

### PLK runner deviations from the `$json` rules

- **Decimals.** The runner rejects a decimal with a negative scale, such as `1d3` (write `1000.`), and a decimal of
  more than 1000 digits, a guard against pathological input. The doc allows both.
- **Datetimes inside `$json`.** The runner reads `second` only from 0 to under 60, and an offset only from -1080 to
  1080 minutes, the range `java.time.ZoneOffset` allows.
- **Negative zero.** PLK stores no negative zero, so a FLOAT `-0e0` and `0e0` match. The doc says they differ.
- **Plain `$time` and `$timestamp`.** Outside `$json`, a null offset is still read as UTC. Inside `$json` it gives a
  time without a time zone, as the doc says.

