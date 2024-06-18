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
     * @return null when the structure is open; fields when struct is closed
     * @throws UnsupportedOperationException if this is called on a type whose {@link Kind} is not:
     * {@link Kind#STRUCT}, {@link Kind#ROW}
     */
    // TODO: Do we support NULL here?
    default Collection<Field> getFields() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * The decimal precision of the type
     * @return decimal precision
     * @throws UnsupportedOperationException if this is called on a type whose {@link Kind} is not:
     * {@link Kind#DECIMAL}, {@link Kind#TIMESTAMP_WITH_TZ}, {@link Kind#TIMESTAMP_WITHOUT_TZ}, {@link Kind#TIME_WITH_TZ},
     * {@link Kind#TIME_WITHOUT_TZ}
     */
    default int getPrecision() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * The max length of the type
     * @return max length of a type
     * @throws UnsupportedOperationException if this is called on a type whose {@link Kind} is not:
     * {@link Kind#CHAR}, {@link Kind#CLOB}, {@link Kind#BLOB}
     * @deprecated EXPERIMENTAL ! Internal: Should this be larger than an int?
     */
    default int getMaxLength() throws UnsupportedOperationException {
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
     * PartiQL Core Types
     * <p></p>
     * Each of these types correspond with a subset of APIs established in {@link PType}. Each of these can be seen as
     * a category of types, distinguished only by the APIs available to them. For instance, all instances of {@link Kind#DECIMAL}
     * may utilize {@link PType#getPrecision()} (and may return different results), however, they may never return a
     * valid value for {@link PType#getFields()}. Consumers of this API should be careful to read the documentation
     * for each API exposed in {@link PType} before using them.
     * @see PType
     */
    enum Kind {
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
        BIT,
        VARBIT,
        BYTE,
        BLOB,
        CLOB,
        DATE,
        TIME_WITH_TZ,
        TIME_WITHOUT_TZ,
        TIMESTAMP_WITH_TZ,
        TIMESTAMP_WITHOUT_TZ,

        /**
         * The bag type. There is no size limit.
         * <p></p>
         * Type Syntax: <code>BAG</code>, <code>BAG(&lt;type&gt;)</code>
         * <p></p>
         * Applicable methods:
         * {@link PType#getTypeParameter()}
         */
        BAG,

        /**
         * Ion's list type. There is no size limit.
         * <p></p>
         * Type Syntax: <code>LIST</code>, <code>LIST(&lt;type&gt;)</code>
         * <p></p>
         * Applicable methods:
         * {@link PType#getTypeParameter()}
         */
        LIST,

        /**
         * SQL's row type. Characterized as a closed, ordered collection of fields.
         * <p></p>
         * Type Syntax: <code>ROW(&lt;str&gt;: &lt;type&gt;, ...)</code>
         * <p></p>
         * Applicable methods:
         * {@link PType#getFields()}
         */
        ROW,

        /**
         * Ion's s-expression type. There is no size limit.
         * <p></p>
         * Type Syntax: <code>SEXP</code>, <code>SEXP(&lt;type&gt;)</code>
         * <p></p>
         * Applicable methods:
         * {@link PType#getTypeParameter()}
         */
        SEXP,

        /**
         * Ion's struct type. Characterized as an open, unordered collection of fields (duplicates allowed).
         * <p></p>
         * Type Syntax: <code>STRUCT</code>
         * <p></p>
         * Applicable methods: NONE
         */
        STRUCT,

        /**
         *
         */
        UNKNOWN // TODO: Define
        // TODO: Do we allow this for first release?
        // INTERVAL, // TODO: Add different variants of interval
    }

    @NotNull
    static PType typeDynamic() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeList() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeList(@NotNull PType typeParam) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeBag() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeBag(@NotNull PType typeParam) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeSexp() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeSexp(@NotNull PType typeParam) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeBool() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeReal() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeDoublePrecision() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeTinyInt() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeSmallInt() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeInt() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeBigInt() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeIntArbitrary() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeDecimalArbitrary() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeDecimal(int precision, int scale) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeRow(@NotNull Collection<Field> fields) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeStruct(@NotNull Collection<Field> fields) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeStruct() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeTimestampWithTZ(int precision) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeTimestampWithoutTZ(int precision) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeTimeWithTZ(int precision) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeTimeWithoutTZ(int precision) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeString() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeSymbol() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeBlob(int maxLength) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeClob(int maxLength) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeChar(int maxLength) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeVarchar(int maxLength) {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeDate() {
        throw new IllegalArgumentException();
    }

    @NotNull
    static PType typeUnknown() {
        throw new IllegalArgumentException();
    }

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
            // TODO
            boolean isOrdered = ((StructType) type).getConstraints().contains(TupleConstraint.Ordered.INSTANCE);
            boolean isClosed = ((StructType) type).getContentClosed();
            List<Field> fields = ((StructType) type).getFields().stream().map((field) -> Field.of(field.getKey(), PType.fromStaticType(field.getValue()))).collect(Collectors.toList());
            if (isClosed && isOrdered) {
                return PType.typeStruct(fields); // TODO: Type ROW?
            } else if (isClosed) {
                return PType.typeStruct(fields);
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