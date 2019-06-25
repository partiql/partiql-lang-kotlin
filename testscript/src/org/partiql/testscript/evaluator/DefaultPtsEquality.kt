package org.partiql.testscript.evaluator

import com.amazon.ion.*
import com.amazon.ion.IonType.*
import java.lang.IllegalArgumentException

/**
 * Default definition of Equality for PartiQL.  
 */
internal object DefaultPtsEquality : PtsEquality {
    override fun isEqual(left: IonValue?, right: IonValue?): Boolean = when {
        left == null && right == null -> true
        left == null && right != null -> false
        left != null && right == null -> false
        else -> left!!.ptsEqual(right!!)
    }

    private fun IonValue.ptsEqual(other: IonValue): Boolean {
        if (type != other.type) {
            return false
        }

        return when (type!!) {
            NULL -> {
                if(isMissing()) {
                    other.isMissing()
                }
                else {
                    other.isNullValue
                }
            }
            BOOL, INT, FLOAT, SYMBOL, STRING, CLOB, BLOB -> this == other
            DECIMAL -> {
                val thisDecimal = this as IonDecimal
                val otherDecimal = other as IonDecimal

                // we use compareTo to ignore differences in scale since 
                // for PartiQL 1.0 == 1.00 while that's not true for Ion
                thisDecimal.bigDecimalValue().compareTo(otherDecimal.bigDecimalValue()) == 0
            }
            TIMESTAMP -> {
                val thisTimestamp = this as IonTimestamp
                val otherTimestamp = other as IonTimestamp
                
                thisTimestamp.millis == otherTimestamp.millis
            }
            LIST -> (this as IonList).ptsSequenceEqual(other as IonList)
            SEXP -> {
                val thisSexp = this as IonSexp
                val otherSexp = other as IonSexp
                
                if (this.isBag() || otherSexp.isBag()) {
                    this.ptsBagEquals(otherSexp)
                } else {
                    thisSexp.ptsSequenceEqual(otherSexp)
                }
            }
            STRUCT -> {
                val thisStruct = this as IonStruct
                val otherStruct = other as IonStruct

                thisStruct.size() == otherStruct.size() && thisStruct.all { it == otherStruct[it.fieldName] }
            }
            DATAGRAM -> throw IllegalArgumentException("DATAGRAM are not a valid type in PTS")
        }
    }

    private fun IonSexp.isBag(): Boolean =
            this.size > 1
                    && this[0].type == SYMBOL
                    && (this[0] as IonSymbol).stringValue() == "bag"

    private fun IonSequence.ptsSequenceEqual(other: IonSequence): Boolean =
            this.size == other.size &&
                    this.asSequence()
                            .mapIndexed { index, thisElement -> index to thisElement }
                            .all { (index, thisElement) -> thisElement.ptsEqual(other[index]) }

    private fun IonSexp.ptsBagEquals(other: IonSexp): Boolean =
            this.size == other.size && this.all { thisElement ->
                other.any { it.ptsEqual(thisElement) }
            }

    private fun IonValue.isMissing(): Boolean = this.isNullValue
            && this.hasTypeAnnotation("missing")
            && this.typeAnnotations.size == 1

}
