package org.partiql.lang.eval.io

import com.amazon.ion.IonInt
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.toIonValue
import org.partiql.lang.util.stringValue

/** Brings together in one place the use cases where an IonSystem is apparently used only
 *  to parse a string into a value of another type.
 *  That is, the parses that could conceivably be done by other means, but doing them
 *  via IonSystem/IonValue appeared expedient.
 *  The problem is that this has lead to a pervasive dependency on IonSystem (with its instance
 *  often passed around) for no strong reason.

 *  It seems all the usages here can be simplified by working with IonElement, bypassing IonValue/IonSystem,
 *  but may have to consider/test for corner cases.
 *  After that, further simplifications could be possible, considering the use sites.
 */
object IonicParse {

    /** The one instance of [IonSystem] shared for all parsing purposes. */
    private val ion = IonSystemBuilder.standard().build()

    /** For usages where the input is the text of a simple (scalar) Ion value
     *  and the returned [IonValue] is immediately converted to the corresponding simple-typed [ExprValue].  */
    fun simpleIon4ExprValue(str: String): IonValue =
        ion.singleValue(str)

    /** For usages where the input is a complex Ion value (as text)
     *  and the returned [IonValue] is immediately consumed by [ExprValue.of].
     *  That is, the goal is to construct a complex ExprValue.
     *  Example:
     *  ExprValue.of(ion.singleValue( """[ { name: "Neil",    mass: 80.5 },
     *                                     { name: "Buzz",    mass: 72.3 } ]"""))
     */
    fun complexIon4ExprValue(str: String): IonValue =
        ion.singleValue(str)

    /** Usages where input is an [IonElement]
     *  and the returned [IonValue] is immediately consumed by [ExprValue.of].
     *  [IonElement] seems to be coming from the parser, which out to be able
     *  to better represent a literal it has just parsed!
     */
    fun element4ExprValue(elem: com.amazon.ionelement.api.AnyElement): IonValue =
        elem.toIonValue(ion)

    /** A usage where the [IonValue] equivalent to the input [IonElement]
     *  is immediately checked for a specific subtype of [IonValue].
     *  (Specifically, only for [IonString] currently.)
     */
    fun element4Tycheck(elem: com.amazon.ionelement.api.AnyElement): IonValue =
        elem.toIonValue(ion)

    /** A usage where the input [IonElement] is assumed to be a string,
     *  which is then extracted. */
    fun element2String(elem: com.amazon.ionelement.api.AnyElement): String? =
        // elem.stringValueOrNull // -- should do the same?
        elem.toIonValue(ion).stringValue()

    /** Parse a string as an [IonInt] integer, throwing an exception if the string does not represent such.
     *  Care is taken to allow leading `+` and zeroes.
     */
    fun parseToIonInt(s: String): IonInt {
        val ion = IonSystemBuilder.standard().build()
        val normalized = s.normalizeForCastToInt()
        return ion.singleValue(normalized) as IonInt
    }

    /**
     * Remove leading zeroes in decimal notation and the plus sign
     *
     * Examples:
     * - `"00001".normalizeForIntCast() == "1"`
     * - `"-00001".normalizeForIntCast() == "-1"`
     * - `"0x00001".normalizeForIntCast() == "0x00001"`
     * - `"+0x00001".normalizeForIntCast() == "0x00001"`
     * - `"000a".normalizeForIntCast() == "a"`
     */
    private fun String.normalizeForCastToInt(): String {
        fun Char.isSign() = this == '-' || this == '+'
        fun Char.isHexOrBase2Marker(): Boolean {
            val c = this.toLowerCase()

            return c == 'x' || c == 'b'
        }

        fun String.possiblyHexOrBase2() = (length >= 2 && this[1].isHexOrBase2Marker()) ||
            (length >= 3 && this[0].isSign() && this[2].isHexOrBase2Marker())

        return when {
            length == 0 -> this
            possiblyHexOrBase2() -> {
                if (this[0] == '+') {
                    this.drop(1)
                } else {
                    this
                }
            }
            else -> {
                val (isNegative, startIndex) = when (this[0]) {
                    '-' -> Pair(true, 1)
                    '+' -> Pair(false, 1)
                    else -> Pair(false, 0)
                }

                var toDrop = startIndex
                while (toDrop < length && this[toDrop] == '0') {
                    toDrop += 1
                }

                when {
                    toDrop == length -> "0" // string is all zeros
                    toDrop == 0 -> this
                    toDrop == 1 && isNegative -> this
                    toDrop > 1 && isNegative -> '-' + this.drop(toDrop)
                    else -> this.drop(toDrop)
                }
            }
        }
    }
}
