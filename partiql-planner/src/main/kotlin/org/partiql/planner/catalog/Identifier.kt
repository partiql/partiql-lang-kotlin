package org.partiql.planner.catalog

/**
 * Represents an SQL identifier (possibly qualified).
 *
 * @property qualifier   If
 * @property identifier
 */
public class Identifier private constructor(
    private val qualifier: Array<Part>,
    private val identifier: Part,
) {

    /**
     * Returns the unqualified name part.
     */
    public fun getIdentifier(): Part = identifier

    /**
     * Returns the name's namespace.
     */
    public fun getQualifier(): Array<Part> = qualifier

    /**
     * Returns true if the namespace is non-empty.
     */
    public fun hasQualifier(): Boolean = qualifier.isNotEmpty()

    /**
     * Compares one identifier to another, possibly ignoring case.
     */
    public fun matches(other: Identifier, ignoreCase: Boolean = false): Boolean {
        //
        if (this.qualifier.size != other.qualifier.size) {
            return false
        }
        // Compare identifier
        if (ignoreCase && !(this.identifier.matches(other.identifier))) {
            return false
        } else if (this.identifier != other.identifier) {
            return false
        }
        for (i in this.qualifier.indices) {
            val lhs = this.qualifier[i]
            val rhs = other.qualifier[i]
            if (ignoreCase && !lhs.matches(rhs)) {
                return false
            } else if (lhs != rhs) {
                return false
            }
        }
        return true
    }

    /**
     * Compares the case-preserved text of two identifiers — that is case-sensitive equality.
     */
    public override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        other as Identifier
        return (this.identifier == other.identifier && this.qualifier.contentEquals(other.qualifier))
    }

    /**
     * The hashCode() is case-sensitive — java.util.Arrays.hashCode
     */
    public override fun hashCode(): Int {
        var result = 1
        result = 31 * result + qualifier.hashCode()
        result = 31 * result + identifier.hashCode()
        return result
    }

    /**
     * Return the SQL representation of this identifier.
     */
    public override fun toString(): String = buildString {
        if (qualifier.isNotEmpty()) {
            append(qualifier.joinToString("."))
            append(".")
        }
        append(identifier)
    }

    /**
     * Represents an SQL identifier part which is either regular (unquoted) or delimited (double-quoted).
     *
     * @property text     The case-preserved identifier text.
     * @property regular  True if the identifier should be treated as an SQL regular identifier.
     */
    public class Part private constructor(
        private val text: String,
        private val regular: Boolean,
    ) {

        /**
         * Compares two identifiers, ignoring case iff at least one identifier is non-delimited.
         */
        public fun matches(other: Part): Boolean {
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
            return this.text == (other as Part).text
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
            public fun regular(text: String): Part = Part(text, true)

            @JvmStatic
            public fun delimited(text: String): Part = Part(text, false)
        }
    }

    public companion object {

        @JvmStatic
        public fun regular(text: String): Identifier = Identifier(emptyArray(), Part.regular(text))

        @JvmStatic
        public fun delimited(text: String): Identifier = Identifier(emptyArray(), Part.delimited(text))

        @JvmStatic
        public fun of(part: Part): Identifier = Identifier(emptyArray(), part)

        @JvmStatic
        public fun of(vararg parts: Part): Identifier = TODO()

        @JvmStatic
        public fun of(parts: Collection<Part>): Identifier = TODO()

        @JvmStatic
        public fun of(vararg parts: String): Identifier {
            TODO()
        }

        @JvmStatic
        public fun of(parts: Collection<String>): Identifier {
            TODO()
        }
    }
}
