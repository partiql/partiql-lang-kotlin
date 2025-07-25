package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.ddl.AttributeConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's data types. Includes both SQL and PartiQL's spec-defined types.
 */
@EqualsAndHashCode(callSuper = false)
public final class DataType extends AstEnum {
    /**
     * A field definition with in a Struct Type Definition
     */
    // At the moment, this is identical to column definition;
    // But we split those into two classes for the following reason:
    //  1. potentially feature addition to the columnDefinition node: See SQL-99 Grammar
    //     <column definition>    ::=
    //         <column name>
    //         { <data type> | <domain name> }
    //         [ <reference scope check> ]
    //         [ <default clause> ]
    //         [ <column constraint definition> ... ]
    //         [ <collate clause> ]
    //  2. the semantics of parameterized struct type has not been finalized,
    //     and the fact that parameterized struct type being an extension to SQL-99.
    @EqualsAndHashCode(callSuper = false)
    public static class StructField extends AstNode {
        @NotNull
        private final Identifier.Simple name;

        @NotNull
        private final DataType type;

        private final boolean optional;

        @NotNull
        private final List<AttributeConstraint> constraints;

        @Nullable
        private final String comment;

        public StructField(
                @NotNull Identifier.Simple name,
                @NotNull DataType type,
                boolean optional,
                @NotNull List<AttributeConstraint> constraints,
                @Nullable String comment) {
            this.name = name;
            this.type = type;
            this.optional = optional;
            this.constraints = constraints;
            this.comment = comment;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            ArrayList<AstNode> kids = new ArrayList<>();
            kids.add(name);
            kids.add(type);
            kids.addAll(constraints);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitDataTypeStructField(this, ctx);
        }

        @NotNull
        public Identifier.Simple getName() {
            return this.name;
        }

        @NotNull
        public DataType getType() {
            return this.type;
        }

        public boolean isOptional() {
            return this.optional;
        }

        @NotNull
        public List<AttributeConstraint> getConstraints() {
            return this.constraints;
        }

        @Nullable
        public String getComment() {
            return this.comment;
        }
    }

    // <character string type>
    public static final int CHARACTER = 0;
    public static final int CHAR = 1;
    public static final int CHARACTER_VARYING = 2;
    public static final int CHAR_VARYING = 3; // TODO not defined in parser yet
    public static final int VARCHAR = 4;
    public static final int CHARACTER_LARGE_OBJECT = 5; // TODO not defined in parser yet
    public static final int CHAR_LARGE_OBJECT = 6; // TODO not defined in parser yet
    public static final int CLOB = 7;
    public static final int STRING = 8;
    public static final int SYMBOL = 9;
    // <binary large object string type>
    public static final int BLOB = 10;
    public static final int BINARY_LARGE_OBJECT = 11; // TODO not defined in parser yet
    // <bit string type>
    public static final int BIT = 12; // TODO not defined in parser yet
    public static final int BIT_VARYING = 13; // TODO not defined in parser yet
    // <numeric type> - <exact numeric type>
    public static final int NUMERIC = 14;
    public static final int DECIMAL = 15;
    public static final int DEC = 16;
    public static final int BIGINT = 17;
    public static final int INT8 = 18;
    public static final int INTEGER8 = 19;
    public static final int INT4 = 20;
    public static final int INTEGER4 = 21;
    public static final int INTEGER = 22;
    public static final int INT = 23;
    public static final int INT2 = 24;
    public static final int INTEGER2 = 25;
    public static final int SMALLINT = 26;
    public static final int TINYINT = 27;
    // <numeric type> - <approximate numeric type>
    public static final int FLOAT = 28;
    public static final int REAL = 29;
    public static final int DOUBLE_PRECISION = 30;
    // <boolean type>
    public static final int BOOLEAN = 31;
    public static final int BOOL = 32;
    // <datetime type>
    public static final int DATE = 33;
    public static final int TIME = 34;
    public static final int TIME_WITH_TIME_ZONE = 35;
    public static final int TIMESTAMP = 36;
    public static final int TIMESTAMP_WITH_TIME_ZONE = 37;
    // <interval type>
    public static final int INTERVAL = 38;
    // <container type>
    public static final int STRUCT = 39;
    public static final int TUPLE = 40;
    // <collection type>
    public static final int LIST = 41;
    public static final int ARRAY = 42;
    public static final int BAG = 43;
    public static final int SEXP = 44;
    // <user defined type>
    public static final int USER_DEFINED = 45;

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

    public static DataType ARRAY() {
        return new DataType(ARRAY);
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

    /**
     * Returns a data type with code {@link #INTERVAL}, however, the interval qualifier is null.
     * @deprecated Use {@link #INTERVAL(IntervalQualifier)} instead.
     * @return a data type with code {@link #INTERVAL}, however, the interval qualifier is null.
     */
    @Deprecated
    public static DataType INTERVAL() {
        return new DataType(INTERVAL);
    }

    /**
     * Returns a data type with code {@link #INTERVAL} and the specified interval qualifier.
     * @param qualifier The interval qualifier.
     * @return a data type with code {@link #INTERVAL} and the specified interval qualifier.
     */
    public static DataType INTERVAL(@NotNull IntervalQualifier qualifier) {
        return new DataType(qualifier);
    }

    public static DataType USER_DEFINED() {
        return new DataType(USER_DEFINED);
    }

    public static DataType USER_DEFINED(@NotNull Identifier name) {
        return new DataType(USER_DEFINED, name);
    }

    // Parameterized Complex Data Type
    public static DataType ARRAY(DataType elementType) {
        return new DataType(ARRAY, elementType);
    }

    public static DataType STRUCT(List<StructField> fields) {
        return new DataType(STRUCT, fields);
    }

    private final int code;
    private final Integer precision;
    private final Integer scale;
    private final Integer length;
    private final DataType elementType;
    private final List<StructField> fields;
    private final Identifier name;
    private final IntervalQualifier intervalQualifier;

    // Private constructor for no parameter DataTypes
    private DataType(int code) {
        this.code = code;
        this.precision = null;
        this.scale = null;
        this.length = null;
        this.elementType = null;
        this.fields = null;
        this.name = null;
        this.intervalQualifier = null;
    }

    /**
     * Creates a data type with code {@link #INTERVAL} and the specified interval qualifier.
     * @param intervalQualifier the interval qualifier.
     */
    private DataType(IntervalQualifier intervalQualifier) {
        this(INTERVAL, null, null, null, null, null, null, intervalQualifier);
    }

    /**
     * The constructor for DataTypes with all parameters.
     * @param code the code of the data type
     * @param precision the precision of the data type
     * @param scale the scale of the data type
     * @param length the length of the data type
     * @param elementType the element type of the data type
     * @param fields the fields of the data type
     * @param name the name of the data type
     * @param intervalQualifier the interval qualifier of the data type
     */
    private DataType(
            int code,
            Integer precision,
            Integer scale,
            Integer length,
            DataType elementType,
            List<StructField> fields,
            Identifier name,
            IntervalQualifier intervalQualifier
    ) {
        this.code = code;
        this.precision = precision;
        this.scale = scale;
        this.length = length;
        this.elementType = elementType;
        this.fields = fields;
        this.name = name;
        this.intervalQualifier = intervalQualifier;
    }

    // Private constructor for DataTypes with Integer parameters; set `name` to null
    private DataType(int code, Integer precision, Integer scale, Integer length) {
        this.code = code;
        this.precision = precision;
        this.scale = scale;
        this.length = length;
        this.elementType = null;
        this.fields = null;
        this.name = null;
        this.intervalQualifier = null;
    }

    // Private constructor for DataTypes with elementType parameter; set `name` to null
    private DataType(int code, DataType elementType) {
        this.code = code;
        this.precision = null;
        this.scale = null;
        this.length = null;
        this.elementType = elementType;
        this.fields = null;
        this.name = null;
        this.intervalQualifier = null;
    }

    private DataType(int code, List<StructField> fields) {
        this.code = code;
        this.precision = null;
        this.scale = null;
        this.length = null;
        this.elementType = null;
        this.fields = fields;
        this.name = null;
        this.intervalQualifier = null;
    }

    // Private constructor for user-defined type w/ an `IdentifierChain` `name`; other parameters set to null
    private DataType(int code, Identifier name) {
        this.code = code;
        this.name = name;
        this.precision = null;
        this.scale = null;
        this.length = null;
        this.elementType = null;
        this.fields = null;
        this.intervalQualifier = null;
    }

    @Override
    public int code() {
        return code;
    }

    @NotNull
    @Override
    public String name() {
        switch (code) {
            case CHARACTER: return "CHARACTER";
            case CHAR: return "CHAR";
            case CHARACTER_VARYING: return "CHARACTER_VARYING";
            case CHAR_VARYING: return "CHAR_VARYING";
            case VARCHAR: return "VARCHAR";
            case CHARACTER_LARGE_OBJECT: return "CHARACTER_LARGE_OBJECT";
            case CHAR_LARGE_OBJECT: return "CHAR_LARGE_OBJECT";
            case CLOB: return "CLOB";
            case STRING: return "STRING";
            case SYMBOL: return "SYMBOL";
            case BLOB: return "BLOB";
            case BINARY_LARGE_OBJECT: return "BINARY_LARGE_OBJECT";
            case BIT: return "BIT";
            case BIT_VARYING: return "BIT_VARYING";
            case NUMERIC: return "NUMERIC";
            case DECIMAL: return "DECIMAL";
            case DEC: return "DEC";
            case BIGINT: return "BIGINT";
            case INT8: return "INT8";
            case INTEGER8: return "INTEGER8";
            case INT4: return "INT4";
            case INTEGER4: return "INTEGER4";
            case INTEGER: return "INTEGER";
            case INT: return "INT";
            case INT2: return "INT2";
            case INTEGER2: return "INTEGER2";
            case SMALLINT: return "SMALLINT";
            case TINYINT: return "TINYINT";
            case FLOAT: return "FLOAT";
            case REAL: return "REAL";
            case DOUBLE_PRECISION: return "DOUBLE_PRECISION";
            case BOOLEAN: return "BOOLEAN";
            case BOOL: return "BOOL";
            case DATE: return "DATE";
            case TIME: return "TIME";
            case TIME_WITH_TIME_ZONE: return "TIME_WITH_TIME_ZONE";
            case TIMESTAMP: return "TIMESTAMP";
            case TIMESTAMP_WITH_TIME_ZONE: return "TIMESTAMP_WITH_TIME_ZONE";
            case INTERVAL: return "INTERVAL";
            case STRUCT: return "STRUCT";
            case TUPLE: return "TUPLE";
            case LIST: return "LIST";
            case ARRAY: return "ARRAY";
            case BAG: return "BAG";
            case SEXP: return "SEXP";
            case USER_DEFINED: return "USER_DEFINED";
            default: throw new IllegalStateException("Invalid DataType code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
        CHARACTER,
        CHAR,
        CHARACTER_VARYING,
        CHAR_VARYING,
        VARCHAR,
        CHARACTER_LARGE_OBJECT,
        CHAR_LARGE_OBJECT,
        CLOB,
        STRING,
        SYMBOL,
        BLOB,
        BINARY_LARGE_OBJECT,
        BIT,
        BIT_VARYING,
        NUMERIC,
        DECIMAL,
        DEC,
        BIGINT,
        INT8,
        INTEGER8,
        INT4,
        INTEGER4,
        INTEGER,
        INT,
        INT2,
        INTEGER2,
        SMALLINT,
        TINYINT,
        FLOAT,
        REAL,
        DOUBLE_PRECISION,
        BOOLEAN,
        BOOL,
        DATE,
        TIME,
        TIME_WITH_TIME_ZONE,
        TIMESTAMP,
        TIMESTAMP_WITH_TIME_ZONE,
        INTERVAL,
        STRUCT,
        TUPLE,
        LIST,
        ARRAY,
        BAG,
        SEXP,
        USER_DEFINED
    };

    @NotNull
    public static DataType parse(@NotNull String value) {
        switch (value) {
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
            case "ARRAY": return ARRAY();
            case "SEXP": return SEXP();
            case "BAG": return BAG();
            case "TIME": return TIME();
            case "TIME_WITH_TIME_ZONE": return TIME_WITH_TIME_ZONE();
            case "TIMESTAMP": return TIMESTAMP();
            case "TIMESTAMP_WITH_TIME_ZONE": return TIMESTAMP_WITH_TIME_ZONE();
            case "INTERVAL": return INTERVAL();
            case "USER_DEFINED": return USER_DEFINED();
            default: throw new IllegalArgumentException("No enum constant DataType." + value);
        }
    }

    @NotNull
    public static int[] codes() {
        return codes;
    }

    /**
     * @return the precision of this data type. If there is no precision, null is returned.
     */
    public Integer getPrecision() {
        return precision;
    }

    /**
     * @return the scale of this data type. If there is no scale, null is returned.
     */
    public Integer getScale() {
        return scale;
    }

    /**
     * @return the length of this data type. If there is no length, null is returned.
     */
    public Integer getLength() {
        return length;
    }

    /**
     * @return the name of this data type. If there is no name, null is returned.
     */
    public Identifier getName() {
        return name;
    }

    /**
     * @return the element type of this data type. If there is no element type, null is returned.
     */
    public DataType getElementType() {
        return elementType;
    }

    /**
     * @return the struct fields of this data type. If there are no struct fields defined, null is returned.
     */
    public List<StructField> getFields() {
        return fields;
    }

    /**
     * Returns the interval qualifier of this data type. If there is no interval qualifier, null is returned.
     * @return the interval qualifier of this data type. If there is no interval qualifier, null is returned.
     */
    @Nullable
    public IntervalQualifier getIntervalQualifier() {
        return intervalQualifier;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        if (name != null) {
            kids.add(name);
        }
        if (elementType != null) {
            kids.add(elementType);
        }
        if (fields != null) {
            kids.addAll(fields);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitDataType(this, ctx);
    }
}
