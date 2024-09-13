package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class Type : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Null -> visitor.visitTypeNull(this, ctx)
        is Missing -> visitor.visitTypeMissing(this, ctx)
        is Bool -> visitor.visitTypeBool(this, ctx)
        is Tinyint -> visitor.visitTypeTinyint(this, ctx)
        is Smallint -> visitor.visitTypeSmallint(this, ctx)
        is Int2 -> visitor.visitTypeInt2(this, ctx)
        is Int4 -> visitor.visitTypeInt4(this, ctx)
        is Bigint -> visitor.visitTypeBigint(this, ctx)
        is Int8 -> visitor.visitTypeInt8(this, ctx)
        is Int -> visitor.visitTypeInt(this, ctx)
        is Real -> visitor.visitTypeReal(this, ctx)
        is Float32 -> visitor.visitTypeFloat32(this, ctx)
        is Float64 -> visitor.visitTypeFloat64(this, ctx)
        is Decimal -> visitor.visitTypeDecimal(this, ctx)
        is Numeric -> visitor.visitTypeNumeric(this, ctx)
        is Char -> visitor.visitTypeChar(this, ctx)
        is Varchar -> visitor.visitTypeVarchar(this, ctx)
        is String -> visitor.visitTypeString(this, ctx)
        is Symbol -> visitor.visitTypeSymbol(this, ctx)
        is Bit -> visitor.visitTypeBit(this, ctx)
        is BitVarying -> visitor.visitTypeBitVarying(this, ctx)
        is ByteString -> visitor.visitTypeByteString(this, ctx)
        is Blob -> visitor.visitTypeBlob(this, ctx)
        is Clob -> visitor.visitTypeClob(this, ctx)
        is Date -> visitor.visitTypeDate(this, ctx)
        is Time -> visitor.visitTypeTime(this, ctx)
        is TimeWithTz -> visitor.visitTypeTimeWithTz(this, ctx)
        is Timestamp -> visitor.visitTypeTimestamp(this, ctx)
        is TimestampWithTz -> visitor.visitTypeTimestampWithTz(this, ctx)
        is Interval -> visitor.visitTypeInterval(this, ctx)
        is Bag -> visitor.visitTypeBag(this, ctx)
        is List -> visitor.visitTypeList(this, ctx)
        is Sexp -> visitor.visitTypeSexp(this, ctx)
        is Tuple -> visitor.visitTypeTuple(this, ctx)
        is Struct -> visitor.visitTypeStruct(this, ctx)
        is Any -> visitor.visitTypeAny(this, ctx)
        is Custom -> visitor.visitTypeCustom(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Null : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeNull(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Missing : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeMissing(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Bool : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeBool(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Tinyint : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeTinyint(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Smallint : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeSmallint(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Int2 : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeInt2(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Int4 : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeInt4(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Bigint : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeBigint(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Int8 : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeInt8(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Int : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeInt(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Real : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeReal(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Float32 : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeFloat32(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Float64 : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeFloat64(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Decimal(
        @JvmField
        public var precision: kotlin.Int?,
        @JvmField
        public var scale: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeDecimal(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Numeric(
        @JvmField
        public var precision: kotlin.Int?,
        @JvmField
        public var scale: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeNumeric(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Char(
        @JvmField
        public var length: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeChar(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Varchar(
        @JvmField
        public var length: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeVarchar(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class String(
        @JvmField
        public var length: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeString(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Symbol : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeSymbol(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Bit(
        @JvmField
        public var length: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeBit(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class BitVarying(
        @JvmField
        public var length: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeBitVarying(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class ByteString(
        @JvmField
        public var length: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeByteString(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Blob(
        @JvmField
        public var length: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeBlob(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Clob(
        @JvmField
        public var length: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeClob(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Date : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeDate(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Time(
        @JvmField
        public var precision: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeTime(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class TimeWithTz(
        @JvmField
        public var precision: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeTimeWithTz(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Timestamp(
        @JvmField
        public var precision: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeTimestamp(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class TimestampWithTz(
        @JvmField
        public var precision: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeTimestampWithTz(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Interval(
        @JvmField
        public var precision: kotlin.Int?,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeInterval(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Bag : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeBag(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object List : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeList(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Sexp : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeSexp(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Tuple : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeTuple(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Struct : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeStruct(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Any : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeAny(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Custom(
        @JvmField
        public var name: kotlin.String,
    ) : Type() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTypeCustom(this, ctx)
    }
}
