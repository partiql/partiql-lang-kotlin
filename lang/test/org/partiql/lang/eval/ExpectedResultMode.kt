package org.partiql.lang.eval

/**
 * Determines the format and equivalence that is used to perform assertions on the expected result of
 * an [EvaluatorTestCase].
 *
 * TODO: ideally, all the test cases actually use a variation of Ion equivalence that recognizes the proper semantics
 * regarding `$partiql_bag::[]` (the order of items in bags is not relevant).  PARTIQL's equivalence supports bags
 * but is otherwise too "loosey-goosey" in that it coerces values of mismatched but similar data types before comparison
 * (i.e. blobs and clobs, strings and symbols, as well as ints, floats and decimals).  Today, unfortunately, the
 * expected values of *all* of our tests utilize one of these two sub-optimal approaches--something that is currently
 * outside of the scope of current work.  Thus, [ExpectedResultMode] was created.
 */
enum class ExpectedResultMode {
    /**
     * The expected value is expressed in Ion and Ion's equivalence is used to assert the correct option.  This is the
     * strictest (and preferred) option when the result isn't a bag because it doesn't support PartiQL's BAG semantics,
     * thus, a query might have the correct result but in a different order the expected value, this would cause a
     * false negative.
     */
    ION,

    /**
     * The expected value is expressed in Ion but does not contain the `$partiql_bag` or `$partiql_missing` annotations.
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
    PARTIQL
}
