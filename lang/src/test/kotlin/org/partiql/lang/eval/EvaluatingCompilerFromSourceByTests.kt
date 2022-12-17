package org.partiql.lang.eval

import org.junit.Test

class EvaluatingCompilerFromSourceByTests : EvaluatorTestBase() {

    class AddressedExprValue(val value: Long) : BaseExprValue(), Scalar, Addressed {
        override val type: ExprValueType
            get() = ExprValueType.INT

        /** This dummy address is [value] + 100. */
        override val address: ExprValue
            get() = ExprValue.newInt(value + 100)

        override val scalar: Scalar get() = this

        override fun numberValue(): Number? = value
    }

    val session = EvaluationSession.build {
        globals(
            Bindings.ofMap(
                mapOf(
                    "someList" to ExprValue.newList(
                        sequenceOf(
                            AddressedExprValue(1),
                            AddressedExprValue(2),
                            AddressedExprValue(3)
                        )
                    ),
                    "someBag" to ExprValue.newBag(
                        sequenceOf(
                            AddressedExprValue(11),
                            AddressedExprValue(12),
                            AddressedExprValue(13)
                        )
                    )
                )
            )
        )
    }

    @Test
    fun rangeOverListWithBy() = runEvaluatorTestCase(
        "SELECT VALUE addr FROM someList BY addr",
        session,
        "$BAG_ANNOTATION::[101,102,103]"
    )

    @Test
    fun rangeOverBagWithBy() = runEvaluatorTestCase(
        "SELECT VALUE addr FROM someBag BY addr",
        session,
        "$BAG_ANNOTATION::[111,112,113]"
    )
    @Test
    fun rangeOverListWithAsAndAt() = runEvaluatorTestCase(
        "SELECT VALUE [i, v, z] FROM someList AS v AT i BY z",
        session,
        "$BAG_ANNOTATION::[[0,1,101],[1,2,102],[2,3,103]]"
    )
    @Test
    fun rangeOverBagWithAsAndAt() = runEvaluatorTestCase(
        "SELECT VALUE [i, v, z] FROM someBag AS v AT i BY z",
        session,
        "$BAG_ANNOTATION::[[$MISSING_ANNOTATION::null,11,111],[$MISSING_ANNOTATION::null,12,112],[$MISSING_ANNOTATION::null,13,113]]"
    )

    @Test
    fun rangeOverListNested() = runEvaluatorTestCase(
        "SELECT VALUE [i, addr, v] FROM (SELECT VALUE v FROM someList AS v) AS v AT i BY addr",
        // the result of the inner query is a bag, so i should always be MISSING
        // However, addr should still contain an address since the items of that bag are unchanged
        session,
        "$BAG_ANNOTATION::[[$MISSING_ANNOTATION::null,101,1],[$MISSING_ANNOTATION::null,102,2],[$MISSING_ANNOTATION::null,103,3]]"
    )

    @Test
    fun rangeOverListNestedArithmetic() = runEvaluatorTestCase(
        "SELECT VALUE [i, addr, v] FROM (SELECT VALUE v + 1000 FROM someList AS v) AS v AT i BY addr",
        // However, since we + 1000 to v in the inner query, we create a new value that does not have an address.
        session,
        "$BAG_ANNOTATION::[[$MISSING_ANNOTATION::null,$MISSING_ANNOTATION::null,1001],[$MISSING_ANNOTATION::null,$MISSING_ANNOTATION::null,1002],[$MISSING_ANNOTATION::null,$MISSING_ANNOTATION::null,1003]]"
    )
}
