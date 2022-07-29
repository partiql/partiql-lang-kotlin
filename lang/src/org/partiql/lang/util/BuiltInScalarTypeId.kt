package org.partiql.lang.util

/**
 * All the built-in scalar type IDs. Will be finally removed as we achieve pluggability goal of OTS
 */
class BuiltInScalarTypeId {
    companion object {
        const val BLOB = "blob"
        const val BOOLEAN = "boolean"
        const val CHARACTER = "character"
        const val CHARACTER_VARYING = "character_varying"
        const val CLOB = "clob"
        const val DATE = "date"
        const val DECIMAL = "decimal"
        const val DOUBLE_PRECISION = "double_precision"
        const val FLOAT = "float"
        const val INTEGER4 = "integer4"
        const val INTEGER8 = "integer8"
        const val INTEGER = "integer"
        const val NUMERIC = "numeric"
        const val REAL = "real"
        const val SMALLINT = "smallint"
        const val STRING = "string"
        const val SYMBOL = "symbol"
        const val TIME = "time"
        const val TIME_WITH_TIME_ZONE = "time_with_time_zone"
        const val TIMESTAMP = "timestamp"
    }
}
