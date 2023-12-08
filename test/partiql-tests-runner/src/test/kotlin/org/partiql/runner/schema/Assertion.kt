package org.partiql.runner.schema

import com.amazon.ion.IonValue

sealed class Assertion {
    data class EvaluationSuccess(val expectedResult: IonValue) : Assertion()
    object EvaluationFailure : Assertion()
    // TODO: other assertion and test categories: https://github.com/partiql/partiql-tests/issues/35
}
