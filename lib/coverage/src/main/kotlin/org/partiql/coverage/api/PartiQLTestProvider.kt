package org.partiql.coverage.api

public interface PartiQLTestProvider {
    public val query: String
    public fun getTestCases(): Iterable<PartiQLTestCase>
}
