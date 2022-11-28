package org.partiql.lang.eval.evaluatortestframework

/**
 * Determines the format and equivalence that is used to perform assertions on the expected result of
 * an [EvaluatorTestCase].
 *
 * TODO: https://github.com/partiql/partiql-lang-kotlin/issues/560
 */
enum class ExpectedResultFormat {
    /**
     * Transform the compiler's actual output [ExprValue] to [IonValue], and then compare it with the specified
     * expected [IonValue]. It can cause false negative when comparing PartiQL bags, since order matters in Ion list
     */
    ION,

    /**
     * Directly compare the compiler's output [ExprValue] with an expected [ExprValue] evaluated from a specified
     * PartiQL expression. This is preferred as it resolves the bag comparison problem [ExpectedResultFormat.ION] has.
     * Also, PartiQL expressions are easier to read & write (no annotations needed).
     *
     * New tests should use this format.
     */
    PARTIQL
}
