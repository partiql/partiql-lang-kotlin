package org.partiql.lang.types

import org.partiql.lang.ast.SqlDataType

/**
 * Data class enclosing the custom data type. It has the following properties:
 *  - [name]
 *  - [typedOpParameter]
 *  - [aliases]
 * @property name The [SqlDataType.CustomDataType] representing one of the core [SqlDataType]
 * @property typedOpParameter Validation logic required at the evaluation time for the custom type.
 * @property aliases List of type name aliases if any for this custom data type.
 */
data class CustomType(
    val name: String,
    val typedOpParameter: TypedOpParameter,
    val aliases: List<String> = listOf()
)
