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

    /**
     * The expected value is expressed in Ion but does not contain the `$bag` or `$missing` annotations.
     * Otherwise, standard Ion equivalence is used to assert the expected result matches.
     *
     * This is for older test cases that existed prior to these attributes and which not been updated yet.  Yes, this is
     * technical debt, but at this time it is easier to support them than it is to migrate each test individually.
     */
    ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS,

    /**
     * The expected value is expressed using PartiQL syntax, which is evaluated under the same pipeline and compile
     * options and session as the query under test. PartiQL equivalence is used to compare the result.
     */
    PARTIQL,

    /**
     * The expected value is an arbitrary string.  [org.partiql.lang.eval.ExprValue.toString]` is called on the result
     * of the query and the expected/actual assertion is performed with regular string equality.
     *
     * This is suboptimal (really, don't use this in new tests) but it is easier to support this here than it is to
     * refactor hundreds of expected values.
     */
    STRING
}
