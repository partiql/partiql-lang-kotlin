package org.partiql.lang.eval

/**
 * Determines the format and equivalence that is used to perform assertions on the expected result of
 * an [EvaluatorTestCase].
 *
 * ## TODO
 *
 * Ideally, all the test cases actually use a variation of Ion equivalence that recognizes the proper semantics
 * regarding `$partiql_bag::[]` (the order of items in bags is not relevant).  We can't have this yet because this
 * would involve changing the expected values of up to several 10s of thousands of unit tests (most of which are
 * generated).  Neither of the notions of equality utilized by the integration tests today are quite ideal.
 *
 * Today, the integration tests use one of two approaches:
 *
 * 1.  PartiQL equivalence (which involves evaluating a PartiQL expression to obtain the expected value), which does
 * support bag semantics, but this is otherwise too "loosey-goosey" in that it coerces values of mismatched but similar
 * data types before comparison (i.e. blobs and clobs, strings and symbols, as well as ints, floats and decimals).
 * This might result in some subtle bugs where the actual value returned by an expression is "equivalent", but still
 * have different types. See [PARTIQL].
 * 2. Ion's equivalence, (which involves parsing an Ion string to obtain the expected value), which avoids the
 * coercion pitfalls of the above with the down side that bag semantics are not supported.  See [ION]
 * an [EvaluatorTestHarness].
 * 2-a. Some of the older test cases use a slight variation of [ION], wherein the type annotations `$partiql_bag` and
 * `$partiql_missing` are removed from the actual result before equivalence assertions.  This to support older test
 * cases that existed before those annotations. See [ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS].
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
