package org.partiql.lang.eval

import com.amazon.ion.IonValue
import org.junit.Test

class EvaluatingCompilerFromSourceByTests : EvaluatorTestBase() {

    class AddressedExprValue(val value: Long, val valueFactory: ExprValueFactory): BaseExprValue(), Scalar, Addressed {

        override val ionValue: IonValue
            get() = valueFactory.ion.newInt(value)

        override val type: ExprValueType
            get() = ExprValueType.INT

        /** This dummy address is [value] + 100. */
        override val address: ExprValue
            get() = valueFactory.newInt(value + 100)

        override val scalar: Scalar get() = this

        override fun numberValue(): Number? = value
    }

    val session = EvaluationSession.build {
        globals(Bindings.ofMap(
            mapOf(
                "someList" to valueFactory.newList(sequenceOf(
                    AddressedExprValue(1, valueFactory),
                    AddressedExprValue(2, valueFactory),
                    AddressedExprValue(3, valueFactory))),
                "someBag" to valueFactory.newBag(sequenceOf(
                    AddressedExprValue(11, valueFactory),
                    AddressedExprValue(12, valueFactory),
                    AddressedExprValue(13, valueFactory))))))
    }

    @Test
    fun rangeOverListWithBy() = assertEval(
        "SELECT VALUE addr FROM someList BY addr",
        """[101, 102, 103]""",
        session
    )

    @Test
    fun rangeOverBagWithBy() = assertEval(
        "SELECT VALUE addr FROM someBag BY addr",
        """[111, 112, 113]""",
        session
    )
    @Test
    fun rangeOverListWithAsAndAt() = assertEval(
        "SELECT VALUE [i, v, z] FROM someList AS v AT i BY z",
        """[[0, 1, 101], [1, 2, 102], [2, 3, 103]]""",
        session
    )
    @Test
    fun rangeOverBagWithAsAndAt() = assertEval(
        "SELECT VALUE [i, v, z] FROM someBag AS v AT i BY z",
        """[[null, 11, 111], [null, 12, 112], [null, 13, 113]]""",
        session
    )

    @Test
    fun rangeOverListNested() = assertEval(
        "SELECT VALUE [i, addr, v] FROM (SELECT VALUE v FROM someList AS v) AS v AT i BY addr",
        // the result of the inner query is a bag, so i should always be MISSING
        // However, addr should still contain an address since the items of that bag are unchanged
        """[[null, 101, 1], [null, 102, 2], [null, 103, 3]]""",
        session
    )

    @Test
    fun rangeOverListNestedArithmetic() = assertEval(
        "SELECT VALUE [i, addr, v] FROM (SELECT VALUE v + 1000 FROM someList AS v) AS v AT i BY addr",
        // However, since we + 1000 to v in the inner query, we create a new value that does not have an address.
        """[[null, null, 1001], [null, null, 1002], [null, null, 1003]]""",
        session
    )
}