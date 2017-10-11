package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*

/**
 * From section 6.7 of SQL 92 spec:
 * ```
 * 6) If <trim function> is specified, then
 *   a) If FROM is specified, then either <trim specification> or <trim character> or both shall be specified.
 *
 *   b) If <trim specification> is not specified, then BOTH is implicit.
 *
 *   c) If <trim character> is not specified, then ' ' is implicit.
 *
 *   d) If TRIM ( SRC ) is specified, then TRIM ( BOTH ' ' FROM SRC ) is implicit.
 *
 *   e) The data type of the <trim function> is variable-length character string with maximum length equal to the
 *   fixed length or maximum variable length of the <trim source>.
 *
 *   f) If a <trim character> is specified, then <trim character> and <trim source> shall be comparable.
 *
 *   g) The character repertoire and form-of-use of the <trim function> are the same as those of the <trim source>.
 *
 *   h) The collating sequence and the coercibility attribute are determined as specified for monadic operators in
 *      Subclause 4.2.3, "Rules determining collating sequence usage", where the <trim source> of TRIM plays the
 *      role of the monadic operand.
 *  ```
 *
 *  Where:
 *  * `<trim specification> ::= LEADING | TRAILING | BOTH`
 *  * `<trim character> ::= <character value expression>`
 *  * `<trim source> ::= <character value expression>`
 */
internal class TrimExprFunction(private val ion: IonSystem) : ExprFunction {
    private val DEFAULT_TO_REMOVE = " ".codePoints().toArray()
    private val DEFAULT_SPECIFICATION = TrimSpecification.BOTH

    private fun IntArray.leadingTrimOffset(toRemove: IntArray): Int {
        var offset = 0

        while (offset < this.size && toRemove.contains(this[offset])) offset += 1

        return offset
    }

    private fun IntArray.trailingTrimOffSet(toRemove: IntArray): Int {
        var offset = 0

        while (offset < this.size && toRemove.contains(this[size - offset - 1])) offset += 1

        return offset
    }

    private fun IntArray.leadingTrim(toRemove: IntArray): String {
        val offset = this.leadingTrimOffset(toRemove)

        return String(this, offset, this.size - offset)
    }

    private fun IntArray.trailingTrim(toRemove: IntArray) = String(this, 0, this.size - this.trailingTrimOffSet(toRemove))

    private fun IntArray.trim(toRemove: IntArray): String {
        val leadingOffset= this.leadingTrimOffset(toRemove)
        val trailingOffset = this.trailingTrimOffSet(toRemove)

        return String(this, leadingOffset, this.size - trailingOffset - leadingOffset)
    }

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        val (type, toRemove, string) = extractArguments(args)

        return when (type) {
            TrimSpecification.BOTH     -> string.trim(toRemove).exprValue(ion)
            TrimSpecification.LEADING  -> string.leadingTrim(toRemove).exprValue(ion)
            TrimSpecification.TRAILING -> string.trailingTrim(toRemove).exprValue(ion)
        }
    }

    private fun ExprValue.codePoints() = this.stringValue().codePoints().toArray()

    private fun extractArguments(args: List<ExprValue>): Triple<TrimSpecification, IntArray, IntArray> {
        return when (args.size) {
            1    -> Triple(DEFAULT_SPECIFICATION, DEFAULT_TO_REMOVE, args[0].codePoints())
            2    -> Triple(TrimSpecification.from(args[0]), DEFAULT_TO_REMOVE, args[1].codePoints())
            3    -> Triple(TrimSpecification.from(args[0]), args[1].codePoints(), args[2].codePoints())

            else -> errNoContext("Trim takes between 1 and 3 arguments, received: ${args.size}")
        }
    }
}

private enum class TrimSpecification {
    BOTH, LEADING, TRAILING;

    companion object {
        fun from(arg: ExprValue) = when (arg.stringValue()) {
            "both"     -> BOTH
            "leading"  -> LEADING
            "trailing" -> TRAILING
            else       -> errNoContext("'${arg.stringValue()}' is an unknown trim specification, valid vales: ${TrimSpecification.values().joinToString()}")
        }
    }

    override fun toString(): String {
        return super.toString().toLowerCase()
    }
}
