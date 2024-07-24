package org.partiql.eval.internal.helpers

import org.partiql.types.PType.Kind

/**
 * Map of the type precedences.
 */
internal object TypePrecedence : Map<Kind, Int> {

    private val _delegate: Map<Kind, Int> = listOf(
        Kind.BOOL,
        Kind.TINYINT,
        Kind.SMALLINT,
        Kind.INT,
        Kind.BIGINT,
        Kind.INT_ARBITRARY,
        Kind.DECIMAL,
        Kind.REAL,
        Kind.DOUBLE_PRECISION,
        Kind.DECIMAL_ARBITRARY, // Arbitrary precision decimal has a higher precedence than FLOAT
        Kind.CHAR,
        Kind.VARCHAR,
        Kind.SYMBOL,
        Kind.STRING,
        Kind.CLOB,
        Kind.BLOB,
        Kind.DATE,
        Kind.TIME_WITHOUT_TZ,
        Kind.TIME_WITH_TZ,
        Kind.TIMESTAMP_WITHOUT_TZ,
        Kind.TIMESTAMP_WITH_TZ,
        Kind.LIST,
        Kind.SEXP,
        Kind.BAG,
        Kind.ROW,
        Kind.STRUCT,
        Kind.DYNAMIC,
    ).mapIndexed { precedence, type -> type to precedence }.toMap()

    override val entries: Set<Map.Entry<Kind, Int>> = _delegate.entries

    override val keys: Set<Kind> = _delegate.keys

    override val size: Int = _delegate.size

    override val values: Collection<Int> = _delegate.values

    override fun isEmpty(): Boolean = _delegate.isEmpty()

    override fun get(key: Kind): Int? = _delegate.get(key)

    override fun containsValue(value: Int): Boolean = _delegate.containsValue(value)

    override fun containsKey(key: Kind): Boolean = _delegate.containsKey(key)
}
