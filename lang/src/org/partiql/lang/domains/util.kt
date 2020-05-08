package org.partiql.lang.domains

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun partiql_ast.builder.id(name: String) =
    id(name, case_insensitive(), unqualified())

