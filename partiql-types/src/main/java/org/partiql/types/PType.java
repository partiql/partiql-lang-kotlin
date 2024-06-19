package org.partiql.types;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This represents a PartiQL type, whether it be a PartiQL primitive or user-defined.
 * <p></p>
 * This implementation allows for parameterization of the core type ({@link Kind}) while allowing for APIs
 * to access their parameters ({@link PType#getPrecision()}, {@link PType#getTypeParameter()}, etc.)
 * <p></p>
 * Before using these methods, please be careful to read each method's documentation to ensure that it applies to the current
 * {@link PType#getKind()}. If one carelessly invokes the wrong method, an {@link UnsupportedOperationException} will be
 * thrown.
 * <p></p>
 * This representation of a PartiQL type is intentionally modeled as a "fat" interface -- holding all methods relevant
 * to any of the types. The maintainers of PartiQL have seen an unintentional reliance on Java's type semantics that
 * make it cumbersome (with explicit Java casts) to gain access to methods. This modeling makes it simpler for the
 * PartiQL planner to have immediate access to the available type's parameters.
 * <p></p>
 * Users should NOT author their own implementation. The current recommendation is to use the static methods
 * (exposed by this interface) to instantiate a type.
 */
public interface PType {

    /**
     * Dictates the associates {@link Kind} of this instance. This method should be called and its return should be
     * analyzed before calling any other method. For example:
     * <p></p>
     * {@code
     *     public int getPrecisionOrNull(PType type) {
     *         if (type.base == {@link Kind#DECIMAL}) {
     *             return type.getPrecision();
     *         }
     *         return null;
     *     }
     * }
     * @return the corresponding PartiQL {@link Kind}.
     */
    @NotNull
    Kind getKind();

    /**
     * The fields of the type
     * @throws UnsupportedOperationException if this is called on a type whose {@link Kind} is not:
     * {@link Kind#ROW}
     */
    @NotNull
    default Collection<Field> getFields() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * The decimal precision of the type
     * @return decimal precision
     * @throws UnsupportedOperationException if this is called on a type whose {@link Kind} is not:
     * {@link Kind#DECIMAL}, {@link Kind#TIMESTAMP_WITH_TZ}, {@link Kind#TIMESTAMP_WITHOUT_TZ}, {@link Kind#TIME_WITH_TZ},
     * {@link Kind#TIME_WITHOUT_TZ}, {@link Kind#REAL}, {@link Kind#DOUBLE_PRECISION}
     */
    default int getPrecision() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * The max length of the type
     * @return max length of a type
     * @throws UnsupportedOperationException if this is called on a type whose {@link Kind} is not:
     * {@link Kind#CHAR}, {@link Kind#CLOB}, {@link Kind#BLOB}
     */
    default int getLength() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * The scale of the type. Example: <code>DECIMAL(&lt;param&gt;, &lt;scale&gt;)</code>
     * @return the scale of the type
     * @throws UnsupportedOperationException if this is called on a type whose {@link Kind} is not:
     * {@link Kind#DECIMAL}
     */
    default int getScale() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * The type parameter of the type. Example: <code>BAG(&lt;param&gt;)</code>
     * @return type parameter of the type
     * @throws UnsupportedOperationException if this is called on a type whose {@link Kind} is not:
     * {@link Kind#LIST}, {@link Kind#BAG}, {@link Kind#SEXP}
     */
    @NotNull
    default PType getTypeParameter() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * PartiQL Core Type Kinds
     * <p></p>
     * Each of these types correspond with a subset of APIs established in {@link PType}. Each of these can be seen as
     * a category of types, distinguished only by the APIs available to them. For instance, all instances of {@link Kind#DECIMAL}
     * may utilize {@link PType#getPrecision()} (and may return different results), however, they may never return a
     * valid value for {@link PType#getFields()}. Consumers of this API should be careful to read the documentation
     * for each API exposed in {@link PType} before using them.
     * <p></p>
     * Future additions <b>may</b> add enums such as INTERVAL_YEAR_MONTH, INTERVAL_DAY_TIME, and more.
     * @see PType
     */
    enum Kind {

        /**
         * PartiQL's dynamic type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>DYNAMIC</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         */
        DYNAMIC,

        /**
         * SQL's boolean type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>BOOL</code>, <code>BOOLEAN</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         */
        BOOL,

        /**
         * PartiQL's tiny integer type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>TINYINT</code>
         * <br>
         * <b>Applicable methods</b>: {@link PType#getPrecision()}, {@link PType#getScale()}
         */
        TINYINT,

        /**
         * SQL's small integer type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>SMALLINT</code>
         * <br>
         * <b>Applicable methods</b>: {@link PType#getPrecision()}, {@link PType#getScale()}
         */
        SMALLINT,

        /**
         * SQL's integer type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>INT</code>, <code>INTEGER</code>
         * <br>
         * <b>Applicable methods</b>: {@link PType#getPrecision()}, {@link PType#getScale()}
         */
        INT,

        /**
         * PartiQL's big integer type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>BIGINT</code>
         * <br>
         * <b>Applicable methods</b>: {@link PType#getPrecision()}, {@link PType#getScale()}
         */
        BIGINT,

        /**
         * PartiQL's big integer type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>TO_BE_DETERMINED</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
         */
        @Deprecated
        INT_ARBITRARY,

        /**
         * SQL's decimal type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>DECIMAL(&lt;precision&gt;, &lt;scale&gt;)</code>, <code>DECIMAL(&lt;precision&gt;)</code>
         * <br>
         * <b>Applicable methods</b>: {@link PType#getPrecision()}, {@link PType#getScale()}
         * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
         */
        DECIMAL,

        /**
         * Ion's arbitrary precision and scale decimal type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>TO_BE_DETERMINED</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
         */
        @Deprecated
        DECIMAL_ARBITRARY,

        /**
         * SQL's real type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>REAL</code>
         * <br>
         * <b>Applicable methods</b>: {@link PType#getPrecision()}
         */
        REAL,

        /**
         * SQL's double precision type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>DOUBLE PRECISION</code>
         * <br>
         * <b>Applicable methods</b>: {@link PType#getPrecision()}
         */
        DOUBLE_PRECISION,

        /**
         * SQL's char type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>CHAR(&lt;length&gt;)</code>, <code>CHARACTER(&lt;length&gt;)</code>, <code>CHAR</code>, <code>CHARACTER</code>
         * <br>
         * <b>Applicable methods</b>: {@link PType#getLength()}
         */
        CHAR,

        /**
         * PartiQL's string type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>TO_BE_DETERMINED</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         */
        STRING,

        /**
         * Ion's symbol type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>TO_BE_DETERMINED</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
         */
        @Deprecated
        SYMBOL,

        /**
         * SQL's blob type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>BLOB</code>, <code>BLOB(&lt;large object length&gt;)</code>,
         * <code>BINARY LARGE OBJECT</code>, <code>BINARY LARGE OBJECT(&lt;large object length&gt;)</code>
         * <br>
         * <b>Applicable methods</b>: {@link PType#getLength()}
         * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
         */
        @Deprecated
        BLOB,

        /**
         * SQL's clob type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>CLOB</code>, <code>CLOB(&lt;large object length&gt;)</code>,
         * <code>CHARACTER LARGE OBJECT</code>, <code>CHARACTER LARGE OBJECT(&lt;large object length&gt;)</code>
         * <br>
         * <b>Applicable methods</b>: {@link PType#getLength()}
         * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
         */
        @Deprecated
        CLOB,

        /**
         * SQL's date type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>DATE</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         */
        DATE,

        /**
         * SQL's time with timezone type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>TIME WITH TIME ZONE</code>, <code>TIME(&lt;precision&gt;) WITH TIME ZONE</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         */
        TIME_WITH_TZ,

        /**
         * SQL's time without timezone type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>TIME</code>, <code>TIME WITHOUT TIME ZONE</code>,
         * <code>TIME(&lt;precision&gt;)</code>, <code>TIME(&lt;precision&gt;) WITHOUT TIME ZONE</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         */
        TIME_WITHOUT_TZ,

        /**
         * SQL's timestamp with timezone type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>TIMESTAMP WITH TIME ZONE</code>, <code>TIMESTAMP(&lt;precision&gt;) WITH TIME ZONE</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         */
        TIMESTAMP_WITH_TZ,

        /**
         * SQL's timestamp without timezone type.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>TIMESTAMP</code>, <code>TIMESTAMP WITHOUT TIME ZONE</code>,
         * <code>TIMESTAMP(&lt;precision&gt;)</code>, <code>TIMESTAMP(&lt;precision&gt;) WITHOUT TIME ZONE</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         */
        TIMESTAMP_WITHOUT_TZ,

        /**
         * PartiQL's bag type. There is no size limit.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>BAG</code>, <code>BAG(&lt;type&gt;)</code>
         * <br>
         * <b>Applicable methods</b>:
         * {@link PType#getTypeParameter()}
         */
        BAG,

        /**
         * Ion's list type. There is no size limit.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>LIST</code>, <code>LIST(&lt;type&gt;)</code>
         * <br>
         * <b>Applicable methods</b>:
         * {@link PType#getTypeParameter()}
         */
        LIST,

        /**
         * SQL's row type. Characterized as a closed, ordered collection of fields.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>ROW(&lt;str&gt;: &lt;type&gt;, ...)</code>
         * <br>
         * <b>Applicable methods</b>:
         * {@link PType#getFields()}
         *
         * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
         */
        @Deprecated
        ROW,

        /**
         * Ion's s-expression type. There is no size limit.
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>SEXP</code>, <code>SEXP(&lt;type&gt;)</code>
         * <br>
         * <b>Applicable methods</b>:
         * {@link PType#getTypeParameter()}
         *
         * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
         */
        @Deprecated
        SEXP,

        /**
         * Ion's struct type. Characterized as an open, unordered collection of fields (duplicates allowed).
         * <br>
         * <br>
         * <b>Type Syntax</b>: <code>STRUCT</code>
         * <br>
         * <b>Applicable methods</b>: NONE
         */
        STRUCT,

        /**
         * PartiQL's unknown type. This temporarily represents literal null and missing values.
         * <br>
         * <br>
         * <b>Type Syntax</b>: NONE
         * <br>
         * <b>Applicable methods</b>: NONE
         * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
         */
        @Deprecated
        UNKNOWN
    }

    /**
     * @return a PartiQL dynamic type
     */
    @NotNull
    static PType typeDynamic() {
        return new PTypePrimitive(Kind.DYNAMIC);
    }

    /**
     * @return a PartiQL list type with a component type of dynamic
     */
    @NotNull
    static PType typeList() {
        return new PTypeCollection(Kind.LIST, PType.typeDynamic());
    }

    /**
     * @return a PartiQL list type with a component type of {@code typeParam}
     */
    @NotNull
    static PType typeList(@NotNull PType typeParam) {
        return new PTypeCollection(Kind.LIST, typeParam);
    }

    /**
     * @return a PartiQL bag type with a component type of dynamic
     */
    @NotNull
    static PType typeBag() {
        return new PTypeCollection(Kind.BAG, PType.typeDynamic());
    }

    /**
     * @return a PartiQL bag type with a component type of {@code typeParam}
     */
    @NotNull
    static PType typeBag(@NotNull PType typeParam) {
        return new PTypeCollection(Kind.BAG, typeParam);
    }

    /**
     * @return a PartiQL sexp type containing a component type of dynamic.
     * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
     */
    @Deprecated
    @NotNull
    static PType typeSexp() {
        return new PTypeCollection(Kind.SEXP, PType.typeDynamic());
    }

    /**
     *
     * @param typeParam the component type to be used
     * @return a PartiQL sexp type containing a component type of {@code typeParam}.
     * @deprecated this is an experimental API and is subject to modification/deletion without prior notice.
     */
    @NotNull
    static PType typeSexp(@NotNull PType typeParam) {
        return new PTypeCollection(Kind.SEXP, typeParam);
    }

    /**
     * @return a PartiQL boolean type
     */
    @NotNull
    static PType typeBool() {
        return new PTypePrimitive(Kind.BOOL);
    }

    /**
     * @return a PartiQL real type.
     */
    @NotNull
    static PType typeReal() {
        return new PTypePrimitive(Kind.REAL);
    }

    /**
     * @return a PartiQL double precision type
     */
    @NotNull
    static PType typeDoublePrecision() {
        return new PTypePrimitive(Kind.DOUBLE_PRECISION);
    }

    /**
     * @return a PartiQL tiny integer type
     */
    @NotNull
    static PType typeTinyInt() {
        return new PTypePrimitive(Kind.TINYINT);
    }

    /**
     * @return a PartiQL small integer type
     */
    @NotNull
    static PType typeSmallInt() {
        return new PTypePrimitive(Kind.SMALLINT);
    }

    /**
     * @return a PartiQL integer type
     */
    @NotNull
    static PType typeInt() {
        return new PTypePrimitive(Kind.INT);
    }

    /**
     * @return a PartiQL big integer type
     */
    @NotNull
    static PType typeBigInt() {
        return new PTypePrimitive(Kind.BIGINT);
    }

    /**
     * @return a PartiQL int (arbitrary precision) type
     * @deprecated this API is experimental and is subject to modification/deletion without prior notice.
     */
    @NotNull
    @Deprecated
    static PType typeIntArbitrary() {
        return new PTypePrimitive(Kind.INT_ARBITRARY);
    }

    /**
     * @return a PartiQL decimal (arbitrary precision/scale) type
     * @deprecated this API is experimental and is subject to modification/deletion without prior notice.
     */
    @NotNull
    static PType typeDecimalArbitrary() {
        return new PTypePrimitive(Kind.DECIMAL_ARBITRARY);
    }

    /**
     * @return a PartiQL decimal type
     */
    @NotNull
    static PType typeDecimal(int precision, int scale) {
        return new PTypeDecimal(precision, scale);
    }

    /**
     * @return a PartiQL row type
     * @deprecated this API is experimental and is subject to modification/deletion without prior notice.
     */
    @NotNull
    static PType typeRow(@NotNull Collection<Field> fields) {
        return new PTypeRow(fields);
    }

    /**
     * @return a PartiQL struct type
     */
    @NotNull
    static PType typeStruct() {
        return new PTypePrimitive(Kind.STRUCT);
    }

    /**
     * @return a PartiQL timestamp with timezone type
     */
    @NotNull
    static PType typeTimestampWithTZ(int precision) {
        return new PTypeWithPrecisionOnly(Kind.TIMESTAMP_WITH_TZ, precision);
    }

    /**
     * @return a PartiQL timestamp without timezone type
     */
    @NotNull
    static PType typeTimestampWithoutTZ(int precision) {
        return new PTypeWithPrecisionOnly(Kind.TIMESTAMP_WITHOUT_TZ, precision);
    }

    /**
     * @return a PartiQL time with timezone type
     */
    @NotNull
    static PType typeTimeWithTZ(int precision) {
        return new PTypeWithPrecisionOnly(Kind.TIME_WITH_TZ, precision);
    }

    /**
     * @return a PartiQL time without timezone type
     */
    @NotNull
    static PType typeTimeWithoutTZ(int precision) {
        return new PTypeWithPrecisionOnly(Kind.TIME_WITHOUT_TZ, precision);
    }

    /**
     * @return a PartiQL string type
     */
    @NotNull
    static PType typeString() {
        return new PTypePrimitive(Kind.STRING);
    }

    /**
     * @return a PartiQL string type
     * @deprecated this API is experimental and is subject to modification/deletion without prior notice.
     */
    @NotNull
    @Deprecated
    static PType typeSymbol() {
        return new PTypePrimitive(Kind.SYMBOL);
    }

    /**
     * @return a PartiQL blob type
     * @deprecated this API is experimental and is subject to modification/deletion without prior notice.
     */
    @NotNull
    static PType typeBlob(int maxLength) {
        return new PTypeWithMaxLength(Kind.BLOB, maxLength);
    }

    /**
     * @return a PartiQL clob type
     * @deprecated this API is experimental and is subject to modification/deletion without prior notice.
     */
    @NotNull
    static PType typeClob(int maxLength) {
        return new PTypeWithMaxLength(Kind.CLOB, maxLength);
    }

    /**
     * @return a PartiQL char type
     */
    @NotNull
    static PType typeChar(int maxLength) {
        return new PTypeWithMaxLength(Kind.CHAR, maxLength);
    }

    /**
     * @return a PartiQL date type
     */
    @NotNull
    static PType typeDate() {
        return new PTypePrimitive(Kind.DATE);
    }

    /**
     * @return a PartiQL unknown type
     * @deprecated this API is experimental and is subject to modification/deletion without prior notice.
     */
    @NotNull
    static PType typeUnknown() {
        return new PTypePrimitive(Kind.UNKNOWN);
    }

    /**
     * @return a corresponding PType from a {@link PartiQLValueType}
     * @deprecated this API is experimental and is subject to modification/deletion without prior notice. This is
     * meant for use internally by the PartiQL library. Public consumers should not use this API.
     */
    @NotNull
    static PType fromPartiQLValueType(@NotNull PartiQLValueType type) {
        switch (type) {
            case DECIMAL:
            case DECIMAL_ARBITRARY:
                return PType.typeDecimalArbitrary();
            case INT8:
                return PType.typeTinyInt();
            case CHAR:
                return PType.typeChar(255);
            case TIMESTAMP:
                return PType.typeTimestampWithTZ(6);
            case DATE:
                return PType.typeDate();
            case BOOL:
                return PType.typeBool();
            case SYMBOL:
                return PType.typeSymbol();
            case STRING:
                return PType.typeString();
            case STRUCT:
                return PType.typeStruct();
            case SEXP:
                return PType.typeSexp();
            case LIST:
                return PType.typeList();
            case BAG:
                return PType.typeBag();
            case FLOAT32:
                return PType.typeReal();
            case INT:
                return PType.typeIntArbitrary();
            case INT64:
                return PType.typeBigInt();
            case INT32:
                return PType.typeInt();
            case INT16:
                return PType.typeSmallInt();
            case TIME:
                return PType.typeTimeWithoutTZ(6);
            case ANY:
                return PType.typeDynamic();
            case FLOAT64:
                return PType.typeDoublePrecision();
            case CLOB:
                return PType.typeClob(Integer.MAX_VALUE);
            case BLOB:
                return PType.typeBlob(Integer.MAX_VALUE);

            // TODO: Is this allowed? This is specifically for literals
            case NULL:
            case MISSING:
                return PType.typeUnknown();

            // Unsupported types
            case INTERVAL:
            case BYTE:
            case BINARY:
                return PType.typeDynamic(); // TODO: REMOVE THIS
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * @return a corresponding PType from a {@link StaticType}
     * @deprecated this API is experimental and is subject to modification/deletion without prior notice. This is
     * meant for use internally by the PartiQL library. Public consumers should not use this API.
     */
    @NotNull
    @Deprecated
    static PType fromStaticType(@NotNull StaticType type) {
        if (type instanceof AnyType) {
            return PType.typeDynamic();
        } else if (type instanceof AnyOfType) {
            HashSet<StaticType> allTypes = new HashSet<>(type.flatten().getAllTypes());
            if (allTypes.isEmpty()) {
                return PType.typeDynamic();
            } else if (allTypes.size() == 1) {
                return fromStaticType(allTypes.stream().findFirst().get());
            } else {
                return PType.typeDynamic();
            }
//            if (allTypes.stream().allMatch((subType) -> subType instanceof CollectionType)) {}
        } else if (type instanceof BagType) {
            PType elementType = fromStaticType(((BagType) type).getElementType());
            return PType.typeBag(elementType);
        } else if (type instanceof BlobType) {
            return PType.typeBlob(Integer.MAX_VALUE); // TODO: Update this
        } else if (type instanceof BoolType) {
            return PType.typeBool();
        } else if (type instanceof ClobType) {
            return PType.typeClob(Integer.MAX_VALUE); // TODO: Update this
        } else if (type instanceof DateType) {
            return PType.typeDate();
        } else if (type instanceof DecimalType) {
            DecimalType.PrecisionScaleConstraint precScale = ((DecimalType) type).getPrecisionScaleConstraint();
            if (precScale instanceof DecimalType.PrecisionScaleConstraint.Unconstrained) {
                return PType.typeDecimalArbitrary();
            } else if (precScale instanceof DecimalType.PrecisionScaleConstraint.Constrained) {
                DecimalType.PrecisionScaleConstraint.Constrained precisionScaleConstraint = (DecimalType.PrecisionScaleConstraint.Constrained) precScale;
                return PType.typeDecimal(precisionScaleConstraint.getPrecision(), precisionScaleConstraint.getScale());
            } else {
                throw new IllegalStateException();
            }
        } else if (type instanceof FloatType) {
            return PType.typeDoublePrecision();
        } else if (type instanceof IntType) {
            IntType.IntRangeConstraint cons = ((IntType) type).getRangeConstraint();
            if (cons == IntType.IntRangeConstraint.INT4) {
                return PType.typeInt();
            } else if (cons == IntType.IntRangeConstraint.SHORT) {
                return PType.typeSmallInt();
            } else if (cons == IntType.IntRangeConstraint.LONG) {
                return PType.typeBigInt();
            } else if (cons == IntType.IntRangeConstraint.UNCONSTRAINED) {
                return PType.typeIntArbitrary();
            } else {
                throw new IllegalStateException();
            }
        } else if (type instanceof ListType) {
            PType elementType = fromStaticType(((ListType) type).getElementType());
            return PType.typeList(elementType);
        } else if (type instanceof SexpType) {
            PType elementType = fromStaticType(((SexpType) type).getElementType());
            return PType.typeSexp(elementType);
        } else if (type instanceof StringType) {
            return PType.typeString();
        } else if (type instanceof StructType) {
            boolean isOrdered = ((StructType) type).getConstraints().contains(TupleConstraint.Ordered.INSTANCE);
            boolean isClosed = ((StructType) type).getContentClosed();
            List<Field> fields = ((StructType) type).getFields().stream().map((field) -> Field.of(field.getKey(), PType.fromStaticType(field.getValue()))).collect(Collectors.toList());
            if (isClosed && isOrdered) {
                return PType.typeRow(fields);
            } else if (isClosed) {
                return PType.typeRow(fields); // TODO: We currently use ROW when closed.
            } else {
                return PType.typeStruct();
            }
        } else if (type instanceof SymbolType) {
            return PType.typeSymbol();
        } else if (type instanceof TimeType) {
            Integer precision = ((TimeType) type).getPrecision();
            if (precision == null) {
                precision = 6;
            }
            return PType.typeTimeWithoutTZ(precision);
        } else if (type instanceof TimestampType) {
            Integer precision = ((TimestampType) type).getPrecision();
            if (precision == null) {
                precision = 6;
            }
            return PType.typeTimestampWithoutTZ(precision);
        } else {
            throw new IllegalStateException("Unsupported type: " + type);
        }
    }
}
