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
        fun family(type: PType.Kind): CoercionFamily {
            return when (type) {
                PType.Kind.TINYINT -> NUMBER
                PType.Kind.SMALLINT -> NUMBER
                PType.Kind.INTEGER -> NUMBER
                PType.Kind.NUMERIC -> NUMBER
                PType.Kind.BIGINT -> NUMBER
                PType.Kind.REAL -> NUMBER
                PType.Kind.DOUBLE -> NUMBER
                PType.Kind.DECIMAL -> NUMBER
                PType.Kind.STRING -> STRING
                PType.Kind.BOOL -> BOOLEAN
                PType.Kind.TIMEZ -> TIME
                PType.Kind.TIME -> TIME
                PType.Kind.TIMESTAMPZ -> TIMESTAMP
                PType.Kind.TIMESTAMP -> TIMESTAMP
                PType.Kind.DATE -> DATE
                PType.Kind.STRUCT -> STRUCTURE
                PType.Kind.ARRAY -> COLLECTION
                PType.Kind.BAG -> COLLECTION
                PType.Kind.ROW -> STRUCTURE
                PType.Kind.CHAR -> STRING
                PType.Kind.VARCHAR -> STRING
                PType.Kind.DYNAMIC -> DYNAMIC // TODO: REMOVE
                PType.Kind.BLOB -> BINARY
                PType.Kind.CLOB -> STRING
                PType.Kind.UNKNOWN -> UNKNOWN // TODO: REMOVE
                PType.Kind.VARIANT -> UNKNOWN // TODO: HANDLE VARIANT
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
            if (from.kind == PType.Kind.UNKNOWN) {
                return true
            }
            if (to.kind == PType.Kind.DYNAMIC) {
                return true
            }
            return family(from.kind) == family(to.kind)
        }
    }
}
