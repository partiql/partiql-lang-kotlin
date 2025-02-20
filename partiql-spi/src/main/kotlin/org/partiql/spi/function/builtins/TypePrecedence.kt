package org.partiql.spi.function.builtins

import org.partiql.spi.types.PType

internal object TypePrecedence {

    /**
     * @return the precedence of the types for the PartiQL comparator.
     * @see TYPE_PRECEDENCE
     */
    internal val TYPE_PRECEDENCE: Map<Int, Int> = listOf(
        PType.UNKNOWN,
        PType.BOOL,
        PType.TINYINT,
        PType.SMALLINT,
        PType.INTEGER,
        PType.BIGINT,
        PType.NUMERIC,
        PType.DECIMAL,
        PType.REAL,
        PType.DOUBLE,
        PType.CHAR,
        PType.VARCHAR,
        PType.STRING,
        PType.CLOB,
        PType.BLOB,
        PType.DATE,
        PType.TIME,
        PType.TIMEZ,
        PType.TIMESTAMP,
        PType.TIMESTAMPZ,
        PType.INTERVAL_YM,
        PType.INTERVAL_DT,
        PType.ARRAY,
        PType.BAG,
        PType.ROW,
        PType.STRUCT,
        PType.DYNAMIC
    ).mapIndexed { precedence, type -> type to precedence }.toMap()
}
