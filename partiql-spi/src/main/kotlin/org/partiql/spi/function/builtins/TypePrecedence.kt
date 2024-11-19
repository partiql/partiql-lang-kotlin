package org.partiql.spi.function.builtins

import org.partiql.types.PType.Kind

internal object TypePrecedence {

    /**
     * @return the precedence of the types for the PartiQL comparator.
     * @see .TYPE_PRECEDENCE
     */
    internal val TYPE_PRECEDENCE: Map<Kind, Int> = listOf(
        Kind.UNKNOWN,
        Kind.BOOL,
        Kind.TINYINT,
        Kind.SMALLINT,
        Kind.INTEGER,
        Kind.BIGINT,
        Kind.NUMERIC,
        Kind.DECIMAL,
        Kind.REAL,
        Kind.DOUBLE,
        Kind.CHAR,
        Kind.VARCHAR,
        Kind.STRING,
        Kind.CLOB,
        Kind.BLOB,
        Kind.DATE,
        Kind.TIME,
        Kind.TIMEZ,
        Kind.TIMESTAMP,
        Kind.TIMESTAMPZ,
        Kind.ARRAY,
        Kind.BAG,
        Kind.ROW,
        Kind.STRUCT,
        Kind.DYNAMIC
    ).mapIndexed { precedence, type -> type to precedence }.toMap()
}
