package org.partiql.eval.internal.helpers

import org.partiql.types.PType

/**
 * This is a utility class to help with planning.
 */
internal object TypeFamily {
    val NUMBERS = setOf(
        PType.Kind.TINYINT,
        PType.Kind.SMALLINT,
        PType.Kind.INT,
        PType.Kind.BIGINT,
        PType.Kind.INT_ARBITRARY,
        PType.Kind.DECIMAL,
        PType.Kind.DECIMAL_ARBITRARY,
        PType.Kind.REAL,
        PType.Kind.DOUBLE_PRECISION,
    )
}
