package org.partiql.ast.v1;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
public class DataType extends AstNode implements Enum {
    public static final int UNKNOWN = 0;
    // <absent types>
    public static final int NULL = 1;
    public static final int MISSING = 2;
    // <character string type>
    public static final int CHARACTER = 3;
    public static final int CHAR = 4;
    public static final int CHARACTER_VARYING = 5;
    public static final int CHAR_VARYING = 6; // TODO not defined in parser yet
    public static final int VARCHAR = 7;
    public static final int CHARACTER_LARGE_OBJECT = 8; // TODO not defined in parser yet
    public static final int CHAR_LARGE_OBJECT = 9; // TODO not defined in parser yet
    public static final int CLOB = 10;
    public static final int STRING = 11;
    public static final int SYMBOL = 12;
    // <binary large object string type>
    public static final int BLOB = 13;
    public static final int BINARY_LARGE_OBJECT = 14; // TODO not defined in parser yet
    // <bit string type>
    public static final int BIT = 15; // TODO not defined in parser yet
    public static final int BIT_VARYING = 16; // TODO not defined in parser yet
    // <numeric type> - <exact numeric type>
    public static final int NUMERIC = 17;
    public static final int DECIMAL = 18;
    public static final int DEC = 19;
    public static final int BIGINT = 20;
    public static final int INT8 = 21;
    public static final int INTEGER8 = 22;
    public static final int INT4 = 23;
    public static final int INTEGER4 = 24;
    public static final int INTEGER = 25;
    public static final int INT = 26;
    public static final int INT2 = 27;
    public static final int INTEGER2 = 28;
    public static final int SMALLINT = 29;
    public static final int TINYINT = 30; // TODO not defined in parser yet
    // <numeric type> - <approximate numeric type>
    public static final int FLOAT = 31;
    public static final int REAL = 32;
    public static final int DOUBLE_PRECISION = 33;
    // <boolean type>
    public static final int BOOLEAN = 34;
    public static final int BOOL = 35;
    // <datetime type>
    public static final int DATE = 36;
    public static final int TIME = 37;
    public static final int TIME_WITH_TIME_ZONE = 38;
    public static final int TIMESTAMP = 39;
    public static final int TIMESTAMP_WITH_TIME_ZONE = 40;
    // <interval type>
    public static final int INTERVAL = 41; // TODO not defined in parser yet
    // <container type>
    public static final int STRUCT = 42;
    public static final int TUPLE = 43;
    // <collection type>
    public static final int LIST = 44;
    public static final int BAG = 45;
    public static final int SEXP = 46;
    // <user defined type>
    public static final int USER_DEFINED = 47;

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

    public static DataType FLOAT(int precision) {
        return new DataType(FLOAT, precision, null, null);
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

    public static DataType DEC(int precision) {
        return new DataType(DEC, precision, null, null);
    }

    public static DataType DEC(int precision, int scale) {
        return new DataType(DEC, precision, scale, null);
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

    public static DataType CHARACTER_LARGE_OBJECT() {
        return new DataType(CHARACTER_LARGE_OBJECT);
    }

    public static DataType CHARACTER_LARGE_OBJECT(int length) {
        return new DataType(CHARACTER_LARGE_OBJECT, null, null, length);
    }

    public static DataType CHAR_LARGE_OBJECT() {
        return new DataType(CHAR_LARGE_OBJECT);
    }

    public static DataType CHAR_LARGE_OBJECT(int length) {
        return new DataType(CHAR_LARGE_OBJECT, null, null, length);
    }

    public static DataType CHARACTER_VARYING() {
        return new DataType(CHARACTER_VARYING);
    }

    public static DataType CHAR_VARYING() {
        return new DataType(CHAR_VARYING);
    }

    public static DataType CHAR_VARYING(int length) {
        return new DataType(CHAR_VARYING, null, null, length);
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

    public static DataType BINARY_LARGE_OBJECT() {
        return new DataType(BINARY_LARGE_OBJECT);
    }

    public static DataType BINARY_LARGE_OBJECT(int length) {
        return new DataType(BINARY_LARGE_OBJECT, null, null, length);
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

    public static DataType USER_DEFINED(@NotNull IdentifierChain name) {
        return new DataType(USER_DEFINED, name);
    }

    private final int code;
    private final Integer precision;
    private final Integer scale;
    private final Integer length;
    private final IdentifierChain name;

    // Private constructor for no parameter DataTypes
    private DataType(int code) {
        this.code = code;
        this.precision = null;
        this.scale = null;
        this.length = null;
        this.name = null;
    }

    // Private constructor for DataTypes with Integer parameters; set `name` to null
    private DataType(int code, Integer precision, Integer scale, Integer length) {
        this.code = code;
        this.precision = precision;
        this.scale = scale;
        this.length = length;
        this.name = null;
    }

    // Private constructor for user-defined type w/ an `IdentifierChain` `name`; other parameters set to null
    private DataType(int code, IdentifierChain name) {
        this.code = code;
        this.name = name;
        this.precision = null;
        this.scale = null;
        this.length = null;
    }

    @Override
    public int code() {
        return code;
    }

    public static DataType valueOf(String value) {
        switch (value) {
            case "NULL": return NULL();
            case "MISSING": return MISSING();
            case "BOOL": return BOOL();
            case "BOOLEAN": return BOOLEAN();
            case "TINYINT": return TINYINT();
            case "SMALLINT": return SMALLINT();
            case "INTEGER2": return INTEGER2();
            case "INT2": return INT2();
            case "INTEGER": return INTEGER();
            case "INT": return INT();
            case "INTEGER4": return INTEGER4();
            case "INT4": return INT4();
            case "INTEGER8": return INTEGER8();
            case "INT8": return INT8();
            case "BIGINT": return BIGINT();
            case "REAL": return REAL();
            case "DOUBLE_PRECISION": return DOUBLE_PRECISION();
            case "FLOAT": return FLOAT();
            case "DECIMAL": return DECIMAL();
            case "DEC": return DEC();
            case "NUMERIC": return NUMERIC();
            case "BIT": return BIT();
            case "BIT_VARYING": return BIT_VARYING();
            case "CHAR": return CHAR();
            case "CHARACTER": return CHARACTER();
            case "VARCHAR": return VARCHAR();
            case "CHARACTER_LARGE_OBJECT": return CHARACTER_LARGE_OBJECT();
            case "CHAR_LARGE_OBJECT": return CHAR_LARGE_OBJECT();
            case "CHAR_VARYING": return CHAR_VARYING();
            case "STRING": return STRING();
            case "SYMBOL": return SYMBOL();
            case "BLOB": return BLOB();
            case "BINARY_LARGE_OBJECT": return BINARY_LARGE_OBJECT();
            case "CLOB": return CLOB();
            case "DATE": return DATE();
            case "STRUCT": return STRUCT();
            case "TUPLE": return TUPLE();
            case "LIST": return LIST();
            case "SEXP": return SEXP();
            case "BAG": return BAG();
            case "TIME": return TIME();
            case "TIME_WITH_TIME_ZONE": return TIME_WITH_TIME_ZONE();
            case "TIMESTAMP": return TIMESTAMP();
            case "TIMESTAMP_WITH_TIME_ZONE": return TIMESTAMP_WITH_TIME_ZONE();
            case "INTERVAL": return INTERVAL();
            case "USER_DEFINED": return USER_DEFINED();
            default: return UNKNOWN();
        }
    }

    public static DataType[] values() {
        return new DataType[] {
            NULL(),
            MISSING(),
            BOOL(),
            BOOLEAN(),
            TINYINT(),
            SMALLINT(),
            INTEGER2(),
            INT2(),
            INTEGER(),
            INT(),
            INTEGER4(),
            INT4(),
            INTEGER8(),
            INT8(),
            BIGINT(),
            REAL(),
            DOUBLE_PRECISION(),
            FLOAT(),
            DECIMAL(),
            DEC(),
            NUMERIC(),
            BIT(),
            BIT_VARYING(),
            CHAR(),
            CHARACTER(),
            VARCHAR(),
            CHARACTER_LARGE_OBJECT(),
            CHAR_LARGE_OBJECT(),
            CHAR_VARYING(),
            STRING(),
            SYMBOL(),
            BLOB(),
            BINARY_LARGE_OBJECT(),
            CLOB(),
            DATE(),
            STRUCT(),
            TUPLE(),
            LIST(),
            SEXP(),
            BAG(),
            TIME(),
            TIME_WITH_TIME_ZONE(),
            TIMESTAMP(),
            TIMESTAMP_WITH_TIME_ZONE(),
            INTERVAL(),
            USER_DEFINED()
        };
    }

    /**
     * TODO docs
     * @return
     */
    public Integer getPrecision() {
        return precision;
    }

    /**
     * TODO docs
     * @return
     */
    public Integer getScale() {
        return scale;
    }

    /**
     * TODO docs
     * @return
     */
    public Integer getLength() {
        return length;
    }

    /**
     * TODO docs
     * @return
     */
    public IdentifierChain getName() {
        return name;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        if (name != null) {
            kids.add(name);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return null;
    }
}
