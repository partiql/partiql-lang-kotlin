package org.partiql.planner.internal

import org.partiql.types.PType

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
    DATE,
    TIME,
    TIMESTAMP,
    COLLECTION,
    UNKNOWN,
    DYNAMIC;

    companion object {

        /**
         * Gets the coercion family for the given [PType.Kind].
         *
         * @see CoercionFamily
         * @see PType.Kind
         * @see family
         */
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
                PType.BOOL -> BOOLEAN
                PType.TIMEZ -> TIME
                PType.TIME -> TIME
                PType.TIMESTAMPZ -> TIMESTAMP
                PType.TIMESTAMP -> TIMESTAMP
                PType.DATE -> DATE
                PType.STRUCT -> STRUCTURE
                PType.ARRAY -> COLLECTION
                PType.BAG -> COLLECTION
                PType.ROW -> STRUCTURE
                PType.CHAR -> STRING
                PType.VARCHAR -> STRING
                PType.DYNAMIC -> DYNAMIC // TODO: REMOVE
                PType.BLOB -> BINARY
                PType.CLOB -> STRING
                PType.UNKNOWN -> UNKNOWN // TODO: REMOVE
                PType.VARIANT -> UNKNOWN // TODO: HANDLE VARIANT
                else -> error("Unknown type: $type")
            }
        }

        /**
         * Determines if the [from] type can be coerced to the [to] type.
         *
         * @see CoercionFamily
         * @see PType
         * @see family
         */
        @JvmStatic
        fun canCoerce(from: PType, to: PType): Boolean {
            if (from.code() == PType.UNKNOWN) {
                return true
            }
            if (to.code() == PType.DYNAMIC) {
                return true
            }
            return family(from.code()) == family(to.code())
        }
    }
}
