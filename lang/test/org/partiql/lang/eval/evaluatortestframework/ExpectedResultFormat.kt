package org.partiql.lang.eval.evaluatortestframework

/**
 * Determines the format and equivalence that is used to perform assertions on the expected result of
 * an [EvaluatorTestCase].
 *
 * TODO: https://github.com/partiql/partiql-lang-kotlin/issues/560
 */
enum class ExpectedResultFormat {
    /**
     * The expected value is expressed in Ion and Ion's equivalence is used to assert the correct option.  This is the
     * strictest (and preferred) option when the result isn't a bag because it doesn't support PartiQL's BAG semantics,
     * thus, a query might have the correct result but in a different order the expected value, this would cause a
     * false negative.
     */
    ION,

    PARTIQL_STRICT
}
