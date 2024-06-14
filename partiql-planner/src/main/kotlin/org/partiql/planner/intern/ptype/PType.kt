package org.partiql.planner.intern.ptype

public class PType(
    public val kind: Kind,
    public val maxLength: Int? = null
) {

    // example
    public fun getMaxLength(): Int = maxLength!!

    public enum class Kind {
        DYNAMIC,
        BOOL,
        TINYINT,
        SMALLINT,
        INT,
        BIGINT,
        INT_ARBITRARY,
        DECIMAL,
        DECIMAL_ARBITRARY,
        REAL,
        DOUBLE_PRECISION,
        CHAR,
        VARCHAR,
        STRING,
        SYMBOL,
        BLOB,
        CLOB,
        DATE,
        TIME_WITH_TZ,
        TIME_WITHOUT_TZ,
        TIMESTAMP_WITH_TZ,
        TIMESTAMP_WITHOUT_TZ,
        BAG,
        LIST,
        ROW,
        SEXP,
        STRUCT,
        UNKNOWN;
    }
}
