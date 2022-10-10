package org.partiql.lang.types

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.util.TypeRegistry

/** Helper to convert [PartiqlAst.Type] in AST to a [TypedOpParameter]. */
internal fun PartiqlAst.Type.toTypedOpParameter(customTypedOpParameters: Map<String, TypedOpParameter>, typeRegistry: TypeRegistry): TypedOpParameter {
    // hack: to avoid duplicating the function `PartiqlAst.Type.toTypedOpParameter`, we have to convert this
    // PartiqlAst.Type to PartiqlPhysical.Type. The easiest way to do that without using a visitor transform
    // (which is overkill and comes with some downsides for something this simple), is to transform to and from
    // s-expressions again.  This will work without difficulty as long as PartiqlAst.Type remains unchanged in all
    // permuted domains between PartiqlAst and PartiqlPhysical.

    // This is really just a temporary measure, however, which must exist for as long as the type inferencer works only
    // on PartiqlAst.  When it has been migrated to use PartiqlPhysical instead, there should no longer be a reason
    // to keep this function around.
    val sexp = this.toIonElement()
    val physicalType = PartiqlPhysical.transform(sexp) as PartiqlPhysical.Type
    return physicalType.toTypedOpParameter(customTypedOpParameters, typeRegistry)
}
