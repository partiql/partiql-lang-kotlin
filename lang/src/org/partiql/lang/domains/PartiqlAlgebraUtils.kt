package org.partiql.lang.domains

import com.amazon.ionelement.api.ionSymbol

fun PartiqlAlgebra.Builder.id(name: String) =
    id(name, caseInsensitive(), unqualified())
