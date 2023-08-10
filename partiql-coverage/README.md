# PartiQL Code Coverage

PartiQL's Code Coverage library stands as a mechanism for risk mitigation by ensuring that your PartiQL source is
thoroughly tested before deploying to your production environment.

## What is Code Coverage?

Code coverage is a metric to help you understand how much of your source is tested. There are several types of Code
Coverage including **Branch Coverage** and **Branch-Condition** Coverage.

### Branch Coverage

**Branch Coverage** is a metric calculated by determining how many *branches* are in a particular PartiQL query and
how many of those branches were executed. A branch is the result of a control-flow expression, such as an `IF` statement
in many programming languages (which results in two branches -- `true` and `false`).

Consider the following PartiQL statement:
```partiql
SELECT *
FROM t
WHERE
    t.a > t.b
    AND
    t.b > t.c
```
In the above PartiQL statement, there is 1 demonstrated type of control flow (the `WHERE` clause). Given that each
control-flow expression can lead to two branches, this PartiQL statement contains 2 branches.

There are other expressions that dictate control flow, such as `CASE-WHEN`, `HAVING`, `COALESCE`, and `NULLIF`. Here's
a slightly more complicated example:
```partiql
SELECT
    measurement AS measurement,
    CASE (measurement > 0)
        WHEN true THEN 'measurement is positive!'
        WHEN false THEN 'measurement is negative!'
    END AS measurementDescription,
    CASE
        WHEN (complexity < 0) THEN 'complexity is negative!'
        WHEN (complexity = 0) THEN 'complexity is zero!'
        WHEN (complexity > 0) THEN 'complexity is positive!'
    END AS complexityDescription
FROM
    t AS t
WHERE
    t.radius > 0
GROUP BY
    t.measurement AS measurement,
    t.complexity AS complexity
HAVING
    measurement != 0;
```

In the above example, there are 14 branches.

## Branch-Condition Coverage

**Branch-Condition Coverage** is a metric calculated by determining how many *branch-conditions* are in a particular PartiQL statement and
how many of those branch-conditions were executed. A branch-condition is the result of a boolean expression contained
within a control-flow expression, such as an `IF` statement in many programming languages. Consider the following example:
```partiql
SELECT *
FROM t
WHERE
    t.a > t.b
    AND
    t.b < t.c
```

In the above PartiQL statement, there is one control-flow expression (the `WHERE` clause). The control-flow expression
results in two branches, however, there are 3 expressions dictating the result of the `WHERE` clause, specifically: the
greater-than expression, the `AND` expression, and the less-than expression. Now, depending on what typing mode you are using,
each one of these expressions can result in 3 or 4 outcomes (`TRUE`, `FALSE`, `NULL`, or `MISSING`).
- If you are in `PERMISSIVE` mode, each of these expressions could potentially return `TRUE`, `FALSE`, `NULL`, or `MISSING`. In
the above example, this would result in 12 branch-conditions.
- If you are not in `PERMISSIVE` mode, each of these expressions could potentially return `TRUE`, `FALSE`, or `MISSING`. In
the above example, this would result in 9 branch-conditions.

## Things to Note

1. While PartiQL’s current evaluator does not statically resolve the types of expressions, it is unknown whether a
   specific branch-condition might return TRUE, FALSE, NULL, or MISSING. Therefore, the existing Evaluating Compiler will,
   based on the typing mode, assume that all boolean expressions can return ALL possible outcomes. In the future, when
   constant-folding and static-typing are part of the planning process, this will be mitigated.
2. Similar to #1, as the current evaluate does not statically resolve the types of expressions, functions and variable
   references will not be part of a Branch-Condition Coverage result. However, in the future, support for this will be added.

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
    // Branch Configurations
    systemProperty("partiql.coverage.lcov.branch.enabled", true)
    systemProperty("partiql.coverage.lcov.branch.report.path", "$buildDir/partiql/coverage/branch/lcov.info")
    systemProperty("partiql.coverage.lcov.branch.html.dir", "$buildDir/reports/partiql/branch/test")
    systemProperty("partiql.coverage.lcov.branch.threshold.min", 0.75)

    // Branch Condition Configurations
    systemProperty("partiql.coverage.lcov.branch-condition.enabled", true)
    systemProperty("partiql.coverage.lcov.branch-condition.report.path", "$buildDir/partiql/coverage/condition/lcov.info")
    systemProperty("partiql.coverage.lcov.branch-condition.html.dir", "$buildDir/reports/partiql/condition/test")
    systemProperty("partiql.coverage.lcov.branch-condition.threshold.min", 0.75)
}
```

### All System Properties
| Key | Value Type | Description | Example Value |
| --- | ---------- | ----------- | ------------- |
| partiql.coverage.lcov.branch.enabled | BOOLEAN | Specifies that LCOV reporting should be enabled | true |
| partiql.coverage.lcov.branch.report.path | STRING | Required if LCOV is enabled. Specifies where to place the LCOV report. | build/partiql/coverage/branch/lcov.info |
| partiql.coverage.lcov.branch.html.dir | STRING | Specifies where to output the JGenHTML output. | build/reports/partiql/branch/test |
| partiql.coverage.lcov.branch.threshold.min | DECIMAL | Specifies the minimum percentage (in decimal form) for branch coverage. Builds will fail when the threshold is not met. | 0.75 |

| Key | Value Type | Description | Example Value |
| --- | ---------- | ----------- | ------------- |
| partiql.coverage.lcov.branch-condition.enabled | BOOLEAN | Specifies that LCOV reporting should be enabled | true |
| partiql.coverage.lcov.branch-condition.report.path | STRING | Required if LCOV is enabled. Specifies where to place the LCOV report. | build/partiql/coverage/branch/lcov.info |
| partiql.coverage.lcov.branch-condition.html.dir | STRING | Specifies where to output the JGenHTML output. | build/reports/partiql/branch/test |
| partiql.coverage.lcov.branch-condition.threshold.min | DECIMAL | Specifies the minimum percentage (in decimal form) for branch coverage. Builds will fail when the threshold is not met. | 0.75 |

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
    override val statement: String = "EXISTS(SELECT * FROM << x >> AS t WHERE t < 7)"
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
