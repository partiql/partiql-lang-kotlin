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
     * The expected value is expressed using PartiQL syntax, which is evaluated under the same pipeline and compile
     * options and session as the query under test. PartiQL equivalence is used to compare the result.
     *
     * Note that this is not being used anywhere in test currently. But we keep it here, since we cannot be 100% sure
     * whether this is not useful at all. We will remove it once we finish the work of test formats cleanup.
     */
    PARTIQL,

    /**
     * The expected value is expressed using PartiQL syntax, which is evaluated under the same pipeline & compile
     * options and session as the query under test. [ExprValue.strictEquals] is used here.
     *
     * This is preferred as it resolves the bag comparison problem [ExpectedResultFormat.ION] has, and respects both
     * data types & data values, compares to [ExpectedResultFormat.PARTIQL]
     *
     * New tests should use this format.
     */
    STRICT
}
