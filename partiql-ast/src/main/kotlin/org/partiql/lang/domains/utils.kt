package org.partiql.lang.domains

/** PartiqlAst.Defnid.string() is a bridge method, indicating places from which introduction of semantic identifiers should spread further.
 *  Given e: SymbolPrimitive (formerly) or e: PartiqlAst.Defnid (currently),
 *  the former usage pattern was
 *      e.text         // results in a String
 *  which now, with this bridge method, became
 *      e.string()     // results is a String
 *  and should, in the future, transition to something like
 *      e.ident()      // results in an identifier
 *  with the downstream code using identifiers rather than strings.
 */
fun PartiqlAst.Defnid.string(): String =
    this.symb.text

fun PartiqlLogicalResolved.Defnid.string(): String =
    this.symb.text

fun PartiqlPhysical.Defnid.string(): String =
    this.symb.text
