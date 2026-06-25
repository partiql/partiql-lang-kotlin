package org.partiql.spi.internal

import org.partiql.spi.types.PType

/**
 * This represents SQL:1999 Section 4.1.2 "Type conversions and mixing of data types" and breaks down the different
 * coercion groups.
 *
 * TODO: [UNKNOWN] should likely be removed in the future. However, it is needed due to literal nulls and missings.
 * TODO: [DYNAMIC] should likely be removed in the future. This is currently only kept to map function signatures.
 */
internal enum class CoercionFamily {
    NUMBER,
    STRING,
    BINARY,
    BOOLEAN,
    STRUCTURE,
    DATE_TIMESTAMP,
    TIME,
    COLLECTION,
    MAP,
    UNKNOWN,
    DYNAMIC,
    INTERVAL_YM,
    INTERVAL_DT,
    ;

    companion object {
        @JvmStatic
        fun family(type: Int): CoercionFamily {
            return when (type) {
                PType.TINYINT -> NUMBER
                PType.SMALLINT -> NUMBER
                PType.INTEGER -> NUMBER
                PType.NUMERIC -> NUMBER
                PType.BIGINT -> NUMBER
                PType.REAL -> NUMBER
                PType.DOUBLE -> NUMBER
                PType.DECIMAL -> NUMBER
                PType.STRING -> STRING
                PType.CHAR -> STRING
                PType.VARCHAR -> STRING
                PType.CLOB -> STRING
                PType.BOOL -> BOOLEAN
                PType.TIME -> TIME
                PType.TIMEZ -> TIME
                PType.TIMESTAMP -> DATE_TIMESTAMP
                PType.TIMESTAMPZ -> DATE_TIMESTAMP
                PType.DATE -> DATE_TIMESTAMP
                PType.STRUCT -> STRUCTURE
                PType.ROW -> STRUCTURE
                PType.ARRAY -> COLLECTION
                PType.BAG -> COLLECTION
                PType.MAP -> MAP
                PType.BLOB -> BINARY
                PType.DYNAMIC -> DYNAMIC
                PType.UNKNOWN -> UNKNOWN
                PType.VARIANT -> UNKNOWN
                PType.INTERVAL_YM -> INTERVAL_YM
                PType.INTERVAL_DT -> INTERVAL_DT
                else -> error("Unknown type: $type")
            }
        }

        @JvmStatic
        fun canCoerce(from: Int, to: Int): Boolean {
            if (from == PType.UNKNOWN) return true
            if (to == PType.DYNAMIC) return true
            return family(from) == family(to)
        }
    }
}
