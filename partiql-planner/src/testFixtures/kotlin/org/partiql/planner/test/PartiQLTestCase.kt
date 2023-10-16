package org.partiql.planner.test

/**
 * Simple class to hold a test; for example,
 *
 * --#[example-test]
 * SELECT * FROM example;
 *
 * PlannerTest(
 *  name = "example-test",
 *  statement = "SELECT * FROM example",
 * )
 */
public data class PartiQLTestCase(
    public val id: String,
    public val statement: String,
)
