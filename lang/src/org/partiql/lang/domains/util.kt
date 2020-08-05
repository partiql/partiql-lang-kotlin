package org.partiql.lang.domains

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlAst.Builder.id(name: String) =
    id(name, caseInsensitive(), unqualified())

