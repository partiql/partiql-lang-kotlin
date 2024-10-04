package org.partiql.ast.v1;

public class DataType implements Enum {
    public static final int UNKNOWN = 0;
    public static final int NULL = 1;
    public static final int MISSING = 2;
    public static final int BOOL = 3;
    public static final int BOOLEAN = 4;
    public static final int TINYINT = 5; // TODO not defined in parser yet
    public static final int SMALLINT = 6;
    public static final int INTEGER2 = 7;
    public static final int INT2 = 8;
    public static final int INTEGER = 9;
    public static final int INT = 10;
    public static final int INTEGER4 = 11;
    public static final int INT4 = 12;
    public static final int INTEGER8 = 13;
    public static final int INT8 = 14;
    public static final int BIGINT = 15;
    public static final int REAL = 16;
    public static final int DOUBLE_PRECISION = 17;
    public static final int FLOAT = 18;
    public static final int DECIMAL = 19;
    public static final int DEC = 20;
    public static final int NUMERIC = 21;
    public static final int BIT = 22; // TODO not defined in parser yet
    public static final int BIT_VARYING = 23; // TODO not defined in parser yet
    public static final int CHAR = 24;
    public static final int CHARACTER = 25;
    public static final int VARCHAR = 26;
    public static final int CHARACTER_VARYING = 27;
    public static final int STRING = 28;
    public static final int SYMBOL = 29;
    public static final int BLOB = 30;
    public static final int CLOB = 31;
    public static final int DATE = 32;
    public static final int STRUCT = 33;
    public static final int TUPLE = 34;
    public static final int LIST = 35;
    public static final int SEXP = 36;
    public static final int BAG = 37;
    public static final int TIME = 38;
    public static final int TIMESTAMP = 39;
    public static final int TIME_WITH_TIME_ZONE = 40;
    public static final int TIMESTAMP_WITH_TIME_ZONE = 41;
    public static final int INTERVAL = 42; // TODO not defined in parser yet
    public static final int USER_DEFINED = 43;

    public static DataType UNKNOWN() {
        return new DataType(UNKNOWN);
    }

    public static DataType NULL() {
        return new DataType(NULL);
    }

    public static DataType MISSING() {
        return new DataType(MISSING);
    }

    public static DataType BOOL() {
        return new DataType(BOOL);
    }

    public static DataType BOOLEAN() {
        return new DataType(BOOLEAN);
    }

    public static DataType TINYINT() {
        return new DataType(TINYINT);
    }

    public static DataType SMALLINT() {
        return new DataType(SMALLINT);
    }

    public static DataType INTEGER2() {
        return new DataType(INTEGER2);
    }

    public static DataType INT2() {
        return new DataType(INT2);
    }

    public static DataType INTEGER() {
        return new DataType(INTEGER);
    }

    public static DataType INT() {
        return new DataType(INT);
    }

    public static DataType INTEGER4() {
        return new DataType(INTEGER4);
    }

    public static DataType INT4() {
        return new DataType(INT4);
    }

    public static DataType INTEGER8() {
        return new DataType(INTEGER8);
    }

    public static DataType INT8() {
        return new DataType(INT8);
    }

    public static DataType BIGINT() {
        return new DataType(BIGINT);
    }

    public static DataType REAL() {
        return new DataType(REAL);
    }

    public static DataType DOUBLE_PRECISION() {
        return new DataType(DOUBLE_PRECISION);
    }

    public static DataType FLOAT() {
        return new DataType(FLOAT);
    }

    public static DataType DECIMAL() {
        return new DataType(DECIMAL);
    }

    public static DataType DECIMAL(int precision) {
        return new DataType(DECIMAL, precision, null, null);
    }

    public static DataType DECIMAL(int precision, int scale) {
        return new DataType(DECIMAL, precision, scale, null);
    }

    public static DataType DEC() {
        return new DataType(DEC);
    }

    public static DataType NUMERIC() {
        return new DataType(NUMERIC);
    }

    public static DataType NUMERIC(int precision) {
        return new DataType(NUMERIC, precision, null, null);
    }

    public static DataType NUMERIC(int precision, int scale) {
        return new DataType(NUMERIC, precision, scale, null);
    }

    public static DataType BIT() {
        return new DataType(BIT);
    }

    public static DataType BIT(int length) {
        return new DataType(BIT, null, null, length);
    }

    public static DataType BIT_VARYING() {
        return new DataType(BIT_VARYING);
    }

    public static DataType BIT_VARYING(int length) {
        return new DataType(BIT_VARYING, null, null, length);
    }

    public static DataType CHAR() {
        return new DataType(CHAR);
    }

    public static DataType CHAR(int length) {
        return new DataType(CHAR, null, null, length);
    }

    public static DataType CHARACTER() {
        return new DataType(CHARACTER);
    }

    public static DataType CHARACTER(int length) {
        return new DataType(CHARACTER, null, null, length);
    }

    public static DataType VARCHAR() {
        return new DataType(VARCHAR);
    }

    public static DataType VARCHAR(int length) {
        return new DataType(VARCHAR, null, null, length);
    }

    public static DataType CHARACTER_VARYING() {
        return new DataType(CHARACTER_VARYING);
    }

    public static DataType CHARACTER_VARYING(int length) {
        return new DataType(CHARACTER_VARYING, null, null, length);
    }

    public static DataType STRING() {
        return new DataType(STRING);
    }

    public static DataType STRING(int length) {
        return new DataType(STRING, null, null, length);
    }

    public static DataType SYMBOL() {
        return new DataType(SYMBOL);
    }

    public static DataType BLOB() {
        return new DataType(BLOB);
    }

    public static DataType BLOB(int length) {
        return new DataType(BLOB, null, null, length);
    }

    public static DataType CLOB() {
        return new DataType(CLOB);
    }

    public static DataType CLOB(int length) {
        return new DataType(CLOB, null, null, length);
    }

    public static DataType DATE() {
        return new DataType(DATE);
    }

    public static DataType STRUCT() {
        return new DataType(STRUCT);
    }

    public static DataType TUPLE() {
        return new DataType(TUPLE);
    }

    public static DataType LIST() {
        return new DataType(LIST);
    }

    public static DataType SEXP() {
        return new DataType(SEXP);
    }

    public static DataType BAG() {
        return new DataType(BAG);
    }

    public static DataType TIME() {
        return new DataType(TIME);
    }

    public static DataType TIME(int precision) {
        return new DataType(TIME, precision, null, null);
    }

    public static DataType TIMESTAMP() {
        return new DataType(TIMESTAMP);
    }

    public static DataType TIMESTAMP(int precision) {
        return new DataType(TIMESTAMP, precision, null, null);
    }

    public static DataType TIME_WITH_TIME_ZONE() {
        return new DataType(TIME_WITH_TIME_ZONE);
    }

    public static DataType TIME_WITH_TIME_ZONE(int precision) {
        return new DataType(TIME_WITH_TIME_ZONE, precision, null, null);
    }

    public static DataType TIMESTAMP_WITH_TIME_ZONE() {
        return new DataType(TIMESTAMP_WITH_TIME_ZONE);
    }

    public static DataType TIMESTAMP_WITH_TIME_ZONE(int precision) {
        return new DataType(TIMESTAMP_WITH_TIME_ZONE, precision, null, null);
    }

    public static DataType INTERVAL() {
        return new DataType(INTERVAL);
    }

    public static DataType USER_DEFINED() {
        return new DataType(USER_DEFINED);
    }

    private final int code;
    private final Integer precision;
    private final Integer scale;
    private final Integer length;

    // Private constructor for no parameter DataTypes
    private DataType(int code) {
        this.code = code;
        this.precision = null;
        this.scale = null;
        this.length = null;
    }

    // Private constructor for DataTypes with parameters
    private DataType(int code, Integer precision, Integer scale, Integer length) {
        this.code = code;
        this.precision = precision;
        this.scale = scale;
        this.length = length;
    }

    @Override
    public int code() {
        return code;
    }
}
