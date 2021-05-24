package org.partiql.lang.thread

import com.amazon.ion.IonSystem
import org.partiql.lang.syntax.SourceSpan
import org.partiql.lang.syntax.Token
import org.partiql.lang.syntax.TokenType

/**
 * A synthetic list of tokens containing [Int.MAX_VALUE] elements, in the format of `1 + 2 + 3...`
 *
 * This class is useful for testing the ability to abort [SqlParser] when parsing something really big.
 */
internal class EndlessTokenList(val ion: IonSystem, val startIndex: Int = 0) : AbstractList<Token>() {

    override val size: Int
        get() = Int.MAX_VALUE

    override fun get(index: Int): Token {
        val span = SourceSpan(index / 80L, index % 80L, 1L)

        return when((startIndex + index) % 2) {
            0 -> Token(
                type = TokenType.LITERAL,
                value = ion.newInt(startIndex + index),
                span = span
            )
            1 -> Token(
                TokenType.OPERATOR,
                ion.newSymbol("+"),
                span
            )
            else -> error("shouldn't happen")
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Token> =
        EndlessTokenList(ion, startIndex + fromIndex)
}