package org.partiql.planner.catalog

/**
 * Represents an SQL identifier (<regular identifier>) which is regular (unquoted) or delimited (double-quoted).
 *
 * @property text     The identifier body.
 * @property regular  True if the identifier should be treated as an SQL regular identifier.
 */
public class Identifier private constructor(
    private val text: String,
    private val regular: Boolean,
) {

    /**
     * Returns the identifier's case-preserved text.
     */
    public fun getText(): String = text

    /**
     * Compares this identifier to a string.
     */
    public fun matches(other: String): Boolean {
        return this.text.equals(other, ignoreCase = this.regular)
    }

    /**
     * Compares two identifiers, ignoring case iff at least one identifier is non-delimited.
     */
    public fun matches(other: Identifier): Boolean {
        return this.text.equals(other.text, ignoreCase = (this.regular || other.regular))
    }

    /**
     * Compares the case-preserved text of two identifiers — that is case-sensitive equality.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        return this.text == (other as Identifier).text
    }

    /**
     * Returns the hashcode of the identifier's case-preserved text.
     */
    override fun hashCode(): Int {
        return this.text.hashCode()
    }

    /**
     * Return the identifier as a SQL string.
     */
    override fun toString(): String = when (regular) {
        true -> "\"${text}\""
        false -> text
    }

    public companion object {

        @JvmStatic
        public fun regular(text: String): Identifier = Identifier(text, true)

        @JvmStatic
        public fun delimited(text: String): Identifier = Identifier(text, false)
    }
}
