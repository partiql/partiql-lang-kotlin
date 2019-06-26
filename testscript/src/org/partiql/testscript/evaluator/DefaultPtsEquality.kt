package org.partiql.testscript.evaluator

import com.amazon.ion.*
import com.amazon.ion.IonType.*
import java.lang.IllegalArgumentException

/**
 * Default definition of Equality for PartiQL PTS. Although similar to PartiQL equivalency definition there are 
 * some differences as PartiQL `=` operator coerces types and for PTS two values are equivalent if and only if 
 * they are of the same type.
 */
internal object DefaultPtsEquality : PtsEquality {
    override fun areEqual(left: IonValue, right: IonValue): Boolean {
        if (left.type != right.type) {
            return false
        }

        return when (left.type!!) {
            NULL -> {
                if (left.isMissing() || right.isMissing()) {
                    left.isMissing() && right.isMissing()
                } else {
                    right.isNullValue
                }
            }
            BOOL, INT, FLOAT, SYMBOL, STRING, CLOB, BLOB -> left == right
            DECIMAL -> {
                val leftDecimal = left as IonDecimal
                val rightDecimal = right as IonDecimal

                // we use compareTo to ignore differences in scale since 
                // for PartiQL 1.0 == 1.00 while that's not true for Ion
                leftDecimal.bigDecimalValue().compareTo(rightDecimal.bigDecimalValue()) == 0
            }
            TIMESTAMP -> {
                val leftTimestamp = left as IonTimestamp
                val rightTimestamp = right as IonTimestamp

                leftTimestamp.timestampValue().compareTo(rightTimestamp.timestampValue()) == 0
            }
            LIST -> ptsSequenceEquals(left as IonList, right as IonList)
            SEXP -> {
                val leftSexp = left as IonSexp
                val rightSexp = right as IonSexp

                if (leftSexp.isBag() || rightSexp.isBag()) {
                    ptsBagEquals(leftSexp, rightSexp)
                } else {
                    ptsSequenceEquals(leftSexp, rightSexp)
                }
            }
            STRUCT -> {
                val leftStruct = left as IonStruct
                val rightStruct = right as IonStruct

                leftStruct.size() == rightStruct.size() && leftStruct.all { it == rightStruct[it.fieldName] }
            }
            DATAGRAM -> throw IllegalArgumentException("DATAGRAM are not a valid type in PTS")
        }
    }
        
    private fun IonSexp.isBag(): Boolean =
            this.size > 1
                    && this[0].type == SYMBOL
                    && (this[0] as IonSymbol).stringValue() == "bag"

    private fun ptsSequenceEquals(left: IonSequence, right: IonSequence): Boolean =
            left.size == right.size &&
                    left.asSequence()
                            .mapIndexed { index, leftElement -> index to leftElement }
                            .all { (index, leftElement) -> areEqual(leftElement, right[index]) }

    // bags can contain repeated elements so they are equal if and only if: 
    // * Same size
    // * All elements in one are contained in the other at the same quantities 
    private fun ptsBagEquals(left: IonSexp, right: IonSexp): Boolean =
            when {
                left.size != right.size -> false
                left.isBag() && right.isBag() -> {
                    left.all { leftEl ->
                        val leftQtd = left.count { areEqual(leftEl, it) }
                        val rightQtd = right.count { areEqual(leftEl, it) }

                        leftQtd == rightQtd
                    }
                }
                else -> false
            }


    private fun IonValue.isMissing(): Boolean = this.isNullValue
            && this.hasTypeAnnotation("missing")
            && this.typeAnnotations.size == 1

}
