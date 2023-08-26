package org.partiql.lang

/** Beginnings of the internal "semantic" identifier ADT
 *  that is to replace the current use of String for this purpose.
 */
class Ident private constructor (
    private val str: String
) {

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other !is Ident) return false
        return this.str.equals(other.str)
    }

    override fun hashCode(): Int {
        return str.hashCode()
    }

    /** For usages where only the print-out appearance of Defnid is needed.
     *  These might persist long-term.
     *  wVG-TODO: Maybe make this the toString override. */
    fun toDisplayString(): String = str

    /** A bridge hack, for usages where the legacy implementation used a string,
     *  but we should eventually transition to using an identifier. */
    fun underlyingString(): String = str

    /** The difference between Defnid creation methods is primarily in intent
     *  (to mark usages with different purposes, as opposed to invoke different implementations):
     *  - [createRegular] performs implementation-defined case normalization;
     *          with legacy identifiers, this is primarily used for the names of built-in functions and such;
     *          with SQL-conforming identifiers, this will be used with user-defined things as well.
     *  - [createDelimited] is for a Defnid that corresponds to a lexical delimited identifier;
     *          probably will be used only with SQL-conforming identifiers.
     *  - [createAsIs] is meant to indicate that the lexical provenance of the identifier is not important;
     *          this corresponds to the semantics of user-introduced legacy identifiers
     *          and should get phased out during transition to SQL-conforming identifiers.
     */
    companion object {
        fun createRegular(str: String): Ident =
            Ident(normalize(str))

        // wVG This is intended to be the one place to hold the design choice of whether
        // regular identifiers normalize to lower or upper case.
        private fun normalize(str: String): String =
            str.lowercase()

        // wVG-TODO: Decide whether this method should deal with quotes within the identifier,
        //  or the argument [str] should come properly pre-processed.
        //  Note: in legacy, this is done in PartiqlPigVisitor.visitIdentifier()
        fun createDelimited(str: String): Ident =
            Ident(str)

        fun createAsIs(str: String): Ident =
            Ident(str)
    }

    /** This marks lowercasing that is essential for implementing "case-sensitive lookup" of legacy identifiers.  */
    fun essentialLowercase(): Ident =
        Ident(str.lowercase())

    /** wVG This method is to mark lowercasing that happened in the prior implementation,
     *  but appeared extraneous. wVG-TODO: switch to no-op and see if tests pass. */
    fun extraLowercase(): Ident =
        Ident(str.lowercase())
}
