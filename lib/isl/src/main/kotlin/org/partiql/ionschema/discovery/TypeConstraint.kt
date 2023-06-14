package org.partiql.ionschema.discovery

import org.partiql.ionschema.model.IonSchemaModel

/**
 * Enum representing the core [IonSchemaModel.TypeReference.NamedType]s along with their corresponding [typeName]s.
 */
enum class TypeConstraint(val typeName: String) {
    // scalar types
    BOOL("bool"),
    INT("int"),
    FLOAT("float"),
    DECIMAL("decimal"),
    TIMESTAMP("timestamp"),
    SYMBOL("symbol"),
    STRING("string"),
    CLOB("clob"),
    BLOB("blob"),
    NULL("\$null"),

    // sequence types
    SEXP("sexp"),
    LIST("list"),

    // struct type
    STRUCT("struct")
}
