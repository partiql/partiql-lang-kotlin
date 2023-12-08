package org.partiql.runner.util

import com.amazon.ion.IonDecimal
import com.amazon.ion.IonList
import com.amazon.ion.IonSequence
import com.amazon.ion.IonSexp
import com.amazon.ion.IonStruct
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Different methods for asserting value equality. The legacy comparator needed to compared on lowered IonValue but
 * we now have PartiQLValue which defines its own `equals` methods.
 *
 * @param T
 */
interface ValueEquals<T> {

    fun equals(left: T, right: T): Boolean

    companion object {

        @JvmStatic
        val legacy: ValueEquals<IonValue> = LegacyValueEquals

        @OptIn(PartiQLValueExperimental::class)
        @JvmStatic
        val partiql: ValueEquals<PartiQLValue> = PartiQLValueEquals
    }
}

/**
 * Value equality using the [PartiQLValue] equality implementation.
 */
@OptIn(PartiQLValueExperimental::class)
private object PartiQLValueEquals : ValueEquals<PartiQLValue> {

    override fun equals(left: PartiQLValue, right: PartiQLValue) = left.equals(right)
}

/**
 * Checks the equality of two PartiQL values defined using its [IonValue] representation. This definition first requires
 * the types to be the same, whereas PartiQL's equal operator can assert equivalence with implicit type coercion. This
 * differs from Ion's definition of equality in the following ways:
 * 1. Bag comparison checks ignore ordering of IonLists
 * 2. Null checks check for `missing` annotation
 */
private object LegacyValueEquals : ValueEquals<IonValue> {

    override fun equals(left: IonValue, right: IonValue): Boolean {
        if (left.type != right.type) {
            return false
        }

        // typed nulls
        if (!left.isMissing() && !right.isMissing() && (left.isNullValue || right.isNullValue)) {
            return left.isNullValue && right.isNullValue
        }

        return when (left.type!!) {
            IonType.NULL -> {
                if (left.isMissing() || right.isMissing()) {
                    left.isMissing() && right.isMissing()
                } else {
                    right.isNullValue
                }
            }
            IonType.BOOL,
            IonType.INT,
            IonType.FLOAT,
            IonType.SYMBOL,
            IonType.STRING,
            IonType.CLOB,
            IonType.BLOB -> left == right
            IonType.DECIMAL -> {
                val leftDecimal = left as IonDecimal
                val rightDecimal = right as IonDecimal

                // we use compareTo to ignore differences in scale since
                // for PartiQL 1.0 == 1.00 while that's not true for Ion
                leftDecimal.bigDecimalValue().compareTo(rightDecimal.bigDecimalValue()) == 0
            }
            IonType.TIMESTAMP -> {
                val leftTimestamp = left as IonTimestamp
                val rightTimestamp = right as IonTimestamp

                leftTimestamp.timestampValue().compareTo(rightTimestamp.timestampValue()) == 0
            }
            IonType.LIST -> {
                val leftList = left as IonList
                val rightList = right as IonList

                if (leftList.isBag() || rightList.isBag()) {
                    ptsBagEquals(leftList, rightList)
                } else {
                    ptsSequenceEquals(leftList, rightList)
                }
            }
            IonType.SEXP -> ptsSequenceEquals(left as IonSexp, right as IonSexp)
            IonType.STRUCT -> left as IonStruct == right as IonStruct
            IonType.DATAGRAM -> throw IllegalArgumentException("DATAGRAM are not a valid type in CTS")
        }
    }

    private fun IonList.isBag(): Boolean =
        this.typeAnnotations.size == 1 &&
            this.typeAnnotations[0] == BAG_ANNOTATION

    private fun ptsSequenceEquals(left: IonSequence, right: IonSequence): Boolean =
        left.size == right.size &&
            left.asSequence()
                .mapIndexed { index, leftElement -> index to leftElement }
                .all { (index, leftElement) -> equals(leftElement, right[index]) }

    // bags can contain repeated elements, so they are equal if and only if:
    // * Same size
    // * All elements in one are contained in the other at the same quantities
    private fun ptsBagEquals(left: IonList, right: IonList): Boolean =
        when {
            left.size != right.size -> false
            left.isBag() && right.isBag() -> {
                left.all { leftEl ->
                    val leftQtd = left.count { equals(leftEl, it) }
                    val rightQtd = right.count { equals(leftEl, it) }

                    leftQtd == rightQtd
                }
            }
            else -> false
        }

    private fun IonValue.isMissing(): Boolean = this.isNullValue &&
        this.hasTypeAnnotation(MISSING_ANNOTATION) &&
        this.typeAnnotations.size == 1
}
