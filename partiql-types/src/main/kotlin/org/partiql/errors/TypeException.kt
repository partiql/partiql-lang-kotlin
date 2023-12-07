package org.partiql.errors

import org.partiql.value.PartiQLValueType

/**
 * TODO
 */
public class TypeCheckException(
    private val actual: PartiQLValueType,
) : RuntimeException()
