package org.partiql.spi.internal

import org.partiql.spi.types.PType

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
    @JvmField val members: Set<Int>,
) {

    /**
     * Constructor a singleton [SqlTypeFamily].
     */
    constructor(preferred: PType) : this(preferred, setOf(preferred.code()))

    operator fun contains(type: PType) = type.code() in members

    companion object {

        fun of(type: PType): SqlTypeFamily = when (type.code()) {
            PType.INTEGER, PType.TINYINT, PType.SMALLINT, PType.BIGINT, PType.REAL, PType.DOUBLE, PType.DECIMAL, PType.NUMERIC -> NUMBER
            PType.CHAR, PType.VARCHAR, PType.STRING, PType.CLOB -> TEXT
            PType.BAG, PType.ARRAY -> COLLECTION
            else -> SqlTypeFamily(type)
        }

        @JvmStatic
        val BOOLEAN = SqlTypeFamily(PType.bool(), setOf(PType.BOOL))

        @JvmStatic
        val TEXT = SqlTypeFamily(
            preferred = PType.string(),
            members = setOf(
                PType.CHAR,
                PType.VARCHAR,
                PType.STRING,
                PType.CLOB,
            )
        )

        @JvmStatic
        val COLLECTION = SqlTypeFamily(
            preferred = PType.bag(),
            members = setOf(
                PType.ARRAY,
                PType.BAG
            )
        )

        @JvmStatic
        val NUMBER = SqlTypeFamily(
            preferred = PType.decimal(38, 19),
            members = setOf(
                PType.TINYINT,
                PType.SMALLINT,
                PType.INTEGER,
                PType.BIGINT,
                PType.NUMERIC,
                PType.REAL,
                PType.DOUBLE,
                PType.DECIMAL,
            )
        )

        @JvmStatic
        val TIME = SqlTypeFamily(
            preferred = PType.time(),
            members = setOf(
                PType.TIME,
                PType.TIMEZ,
            )
        )

        @JvmStatic
        val TIMESTAMP = SqlTypeFamily(
            preferred = PType.timestamp(),
            members = setOf(
                PType.TIMESTAMP,
                PType.TIMESTAMPZ,
            )
        )

        @JvmStatic
        val DATETIME = SqlTypeFamily(
            preferred = PType.date(),
            members = setOf(
                PType.DATE,
                PType.TIME,
                PType.TIMEZ,
                PType.TIMESTAMP,
                PType.TIMESTAMPZ
            )
        )

        @JvmStatic
        val INTERVAL = SqlTypeFamily(
            preferred = PType.date(),
            members = setOf(
                PType.INTERVAL_YM,
                PType.INTERVAL_DT
            )
        )
    }
}
