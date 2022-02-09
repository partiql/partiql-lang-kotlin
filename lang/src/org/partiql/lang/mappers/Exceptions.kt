package org.partiql.lang.mappers

/**
 * General exception class for ISL and StaticType mappers.
 */

/**
 * Exception for cases when an expected ISL type definition is not found.
 *
 * @param name the type name corresponding to the ISL Type
 * @param message the message for this exception
 */
class TypeNotFoundException(name: String, message: String = "Type not found") : RuntimeException("$message : $name")