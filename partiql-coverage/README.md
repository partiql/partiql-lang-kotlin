# PartiQL Coverage

## Status

This package should be considered **experimental**. All updates to the public APIs
may break without prior notice.

## Requirements

- Must be using JUnit5.

## Build Configuration

### JUnit5 Configuration

Please follow the directions outlined in [JUnit5's User Guide](https://junit.org/junit5/docs/current/user-guide/).

### Adding Dependencies

```kotlin
// File: build.gradle.kts
const val LATEST_VERSION = "0.13.0" // Please search Maven Central for latest version
dependencies {
    testImplementation("org.partiql:partiql-coverage:$LATEST_VERSION")
}
```

### Adding Coverage Thresholds

The PartiQL Code Coverage library allows consumers to fail their local builds if they haven't reached
a minimum branch coverage. See below:
```kotlin
// File: build.gradle.kts
tasks.test {
    systemProperty("org.partiql.coverage.config.branch-minimum", 0.75)
}
```

### All System Properties
| Key | Value | Description | Default Value | Example Custom Value |
| --- | ----- | ----------- | ------------- | ------------- |
| org.partiql.coverage.config.branch-minimum | DECIMAL | Specifies the minimum percentage (in decimal form) for branch coverage. Builds will fail when the threshold is not met. | null | 0.75 |
| org.partiql.coverage.config.report-location | STRING | Specifies the absolute path where the LCOV.INFO file should be written to. | build/partiql/coverage/report/lcov.info | /tmp/lcov.info |

## API Usage

Now, we can actually begin writing tests! See the below Kotlin example:

```kotlin
/**
 * Here's an example of a test method. This method will get loaded up by JUnit 5's
 * test framework and be executed.
 *
 * Note that each test method annotated with @PartiQLTest needs to take in an implementation of a PartiQLTestCase and a PartiQLResult.
 * Note that PartiQLResult is a sealed interface -- you can opt to assume the variant that will be returned, or you can
 * just take in a PartiQLResult.
 */
@PartiQLTest(provider = SimpleTestProvider::class)
fun simpleTest(tc: SimpleTestCase, result: PartiQLResult.Value) {
    val exprValue = result.value
    assertEquals(ExprValueType.BOOL, exprValue.type)
    assertEquals(tc.expected, exprValue.booleanValue())
}

/**
 * PartiQLTestProviders represent a set of tests aimed at testing a single query.
 */
class SimpleTestProvider : PartiQLTestProvider {
    override val query: String = "x < 7"
    override fun getTestCases(): List<PartiQLTestCase> = listOf(
        SimpleTestCase(x = 6, expected = true),
        SimpleTestCase(x = 8, expected = false)
    )
}

/**
 * An implementation of a PartiQLTestCase. Note that we pass in 
 */
class SimpleTestCase(private val x: Int, val expected: Boolean) : PartiQLTestCase {
    override val session: EvaluationSession = EvaluationSession.builder {
        globals(mapOf("x" to x))
    }
}
```

In the above example, we are testing a PartiQL query: `x < 7`. Note that the `SimpleTestProvider` provides two test cases:
1. The first makes the variable `x` equal to 6. Therefore, the PartiQL query is expected to return true.
2. The second makes the variable `x` equal to 8. Therefore, the PartiQL query is expected to return false.

## Report Generation

The current and only reporting format supported by PartiQL Code Coverage is LCOV. Upon the execution of test methods annotated
with `@PartiQLTest`, the PartiQL Code Coverage library will generate a report specified by the corresponding system property.
See the `All System Properties` further above for more information.

## HTML Generation
To render:
```shell
# Set Environment Variables
PROJECT_TO_TEST=<path to project> # Path to project containing tests
REPORT_PATH=${PROJECT_TO_TEST}/build/partiql/coverage/report/cov.info
OUTPUT_HTML_DIR=${PROJECT_TO_TEST}/build/reports/partiql/html
PREFIX_TO_REMOVE=${PROJECT_TO_TEST}/build/partiql/coverage/source

# Invoke LCOV's GENHTML Command
genhtml
  --title "PartiQL Code Coverage Report" \
  --ignore-errors source \
  --legend \
  --prefix ${PREFIX_TO_REMOVE} \
  --output-directory ${OUTPUT_HTML_DIR}
  --branch-coverage \
  --show-noncode \
  --show-navigation \
  --show-details \
  ${REPORT_PATH}
```
