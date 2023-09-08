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

    // wVG-TODO Remove after dust settles.  Was just to maintain provenance from BindingName.isEquivalentTo
    fun isEquivalentTo(other: Ident) = equals(other)

    /** For usages where only the print-out appearance of identifier is needed.
     *  These might persist long-term.
     *  TODO: Maybe make this the toString override? */
    fun toDisplayString(): String = str

    /** A bridge hack, for usages where the legacy implementation used a string,
     *  but we should eventually transition to using an identifier. */
    fun underlyingString(): String = str

    companion object {
        /** Create a semantic identifier corresponding to a lexical SQL *regular* identifier. */
        //   with legacy identifiers, this is primarily used for the names of built-in functions at definition sites;
        //   with SQL-conforming identifiers, this will be used with user-defined things as well.
        fun createFromRegular(str: String): Ident =
            Ident(normalizeRegular(str))

        /** Normalize the string contents of a *regular* SQL identifier.  */
        // TODO This is intended to be the one place to encapsulate the design choice of whether
        // regular identifiers normalize to lower or upper case. Transitioning to this is just in its beginning --
        // there are still many sites sprinkled around where this is done by ad hoc string lower-casing.
        // TODO Make this private when its uses outside of this class are phased out.
        fun normalizeRegular(str: String): String =
            str.lowercase()

        /** Create a semantic identifier corresponding to a lexical SQL *delimited* identifier. */
        //  Probably will only be used with SQL-conforming identifiers.
        fun createFromDelimited(str: String): Ident =
            Ident(normalizeDelimited(str))

        /** Normalize the string contents of a *delimited* SQL identifier.
         *  (Such as recognizing quoted double-quote.) */
        // TODO At the start, this is a no-op, since the normalization is done in the parser (in PartiqlPigVisitor.visitIdentifier).
        private fun normalizeDelimited(str: String): String =
            str

        /** Create a semantic identifier from the given string content as is, without processing it in any way. */
        // Using this constructor indicates that the lexical provenance of the identifier is not important;
        // this corresponds to the semantics of legacy identifiers at definition sites (defnids).
        // This constructor should get phased out during transition to Ident ADT, in the setting of SQL-conforming identifiers
        // (primarily by being replaced with createFromDelimited).
        fun createAsIs(str: String): Ident =
            Ident(str)
    }

    /** This marks lowercasing that is essential for implementing "case-sensitive lookup" of legacy identifiers.  */
    fun essentialLowercase(): Ident =
        Ident(str.lowercase())

    /** This method marks lowercasing that happened in the prior implementation, but appears extraneous.
     *  This is a precaution; can be removed if all continues being well.  */
    fun extraLowercase(): Ident =
        // Ident(str.lowercase())
        this // the no-op is just as good
}
