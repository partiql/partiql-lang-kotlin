package org.partiql.lang.util

import org.partiql.lang.errors.Property


/** Returns a Map<Property, Any> with the specified line & column number. */
internal fun sourceLocationProperties(lineNum: Long, colNum: Long): Map<Property, Any> =
    mapOf(Property.LINE_NUMBER to lineNum, Property.COLUMN_NUMBER to colNum)

