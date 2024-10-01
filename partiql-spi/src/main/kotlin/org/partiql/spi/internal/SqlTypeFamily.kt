package org.partiql.spi.internal

import org.partiql.types.PType
import org.partiql.types.PType.Kind

/**
 * A basic "set" representation for type categorization; perhaps we optimize later..
 *
 * From Calcite,
 * > SqlTypeFamily provides SQL type categorization.
 * > Primary Families
 * > CHARACTER,
 * >  BINARY,
 * >  NUMERIC,
 * >  DATE,
 * >  TIME,
 * >  TIMESTAMP,
 * >  BOOLEAN,
 * > https://github.com/apache/calcite/blob/main/core/src/main/java/org/apache/calcite/sql/type/SqlTypeFamily.java
 *
 *
 * From Postgres,
 * > Data types are divided into several basic type categories, including boolean, numeric, string, bitstring, datetime,
 * > timespan, geometric, network, and user-defined. Within each category there can be one or more preferred types,
 * > which are preferred when there is a choice of possible types.
 */
internal class SqlTypeFamily private constructor(
    @JvmField val preferred: PType,
    @JvmField val members: Set<Kind>,
) {

    /**
     * Constructor a singleton [SqlTypeFamily].
     */
    constructor(preferred: PType) : this(preferred, setOf(preferred.kind))

    operator fun contains(type: PType) = type.kind in members

    companion object {

        @JvmStatic
        val TEXT = SqlTypeFamily(
            preferred = PType.string(),
            members = setOf(
                Kind.CHAR,
                Kind.VARCHAR,
                Kind.STRING,
                Kind.SYMBOL,
                Kind.CLOB,
            )
        )

        @JvmStatic
        val COLLECTION = SqlTypeFamily(
            preferred = PType.bag(),
            members = setOf(
                Kind.ARRAY,
                Kind.SEXP,
                Kind.BAG
            )
        )

        @JvmStatic
        val NUMERIC = SqlTypeFamily(
            preferred = PType.decimal(),
            members = setOf(
                Kind.TINYINT,
                Kind.SMALLINT,
                Kind.INTEGER,
                Kind.BIGINT,
                Kind.NUMERIC,
                Kind.REAL,
                Kind.DOUBLE,
                Kind.DECIMAL,
                Kind.DECIMAL_ARBITRARY
            )
        )
    }
}
