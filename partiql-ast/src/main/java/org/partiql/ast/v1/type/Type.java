package org.partiql.ast.v1.type;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

/**
 * TODO docs, equals, hashcode
 */
public abstract class Type extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof TypeNull) {
            return visitor.visitTypeNull((TypeNull) this, ctx);
        } else if (this instanceof TypeMissing) {
            return visitor.visitTypeMissing((TypeMissing) this, ctx);
        } else if (this instanceof TypeBool) {
            return visitor.visitTypeBool((TypeBool) this, ctx);
        } else if (this instanceof TypeTinyint) {
            return visitor.visitTypeTinyint((TypeTinyint) this, ctx);
        } else if (this instanceof TypeSmallint) {
            return visitor.visitTypeSmallint((TypeSmallint) this, ctx);
        } else if (this instanceof TypeInt2) {
            return visitor.visitTypeInt2((TypeInt2) this, ctx);
        } else if (this instanceof TypeInt4) {
            return visitor.visitTypeInt4((TypeInt4) this, ctx);
        } else if (this instanceof TypeBigint) {
            return visitor.visitTypeBigint((TypeBigint) this, ctx);
        } else if (this instanceof TypeInt8) {
            return visitor.visitTypeInt8((TypeInt8) this, ctx);
        } else if (this instanceof TypeInt) {
            return visitor.visitTypeInt((TypeInt) this, ctx);
        } else if (this instanceof TypeReal) {
            return visitor.visitTypeReal((TypeReal) this, ctx);
        } else if (this instanceof TypeFloat32) {
            return visitor.visitTypeFloat32((TypeFloat32) this, ctx);
        } else if (this instanceof TypeFloat64) {
            return visitor.visitTypeFloat64((TypeFloat64) this, ctx);
        } else if (this instanceof TypeDecimal) {
            return visitor.visitTypeDecimal((TypeDecimal) this, ctx);
        } else if (this instanceof TypeNumeric) {
            return visitor.visitTypeNumeric((TypeNumeric) this, ctx);
        } else if (this instanceof TypeChar) {
            return visitor.visitTypeChar((TypeChar) this, ctx);
        } else if (this instanceof TypeVarchar) {
            return visitor.visitTypeVarchar((TypeVarchar) this, ctx);
        } else if (this instanceof TypeString) {
            return visitor.visitTypeString((TypeString) this, ctx);
        } else if (this instanceof TypeSymbol) {
            return visitor.visitTypeSymbol((TypeSymbol) this, ctx);
        } else if (this instanceof TypeBit) {
            return visitor.visitTypeBit((TypeBit) this, ctx);
        } else if (this instanceof TypeBitVarying) {
            return visitor.visitTypeBitVarying((TypeBitVarying) this, ctx);
        } else if (this instanceof TypeByteString) {
            return visitor.visitTypeByteString((TypeByteString) this, ctx);
        } else if (this instanceof TypeBlob) {
            return visitor.visitTypeBlob((TypeBlob) this, ctx);
        } else if (this instanceof TypeClob) {
            return visitor.visitTypeClob((TypeClob) this, ctx);
        } else if (this instanceof TypeDate) {
            return visitor.visitTypeDate((TypeDate) this, ctx);
        } else if (this instanceof TypeTime) {
            return visitor.visitTypeTime((TypeTime) this, ctx);
        } else if (this instanceof TypeTimeWithTz) {
            return visitor.visitTypeTimeWithTz((TypeTimeWithTz) this, ctx);
        } else if (this instanceof TypeTimestamp) {
            return visitor.visitTypeTimestamp((TypeTimestamp) this, ctx);
        } else if (this instanceof TypeTimestampWithTz) {
            return visitor.visitTypeTimestampWithTz((TypeTimestampWithTz) this, ctx);
        } else if (this instanceof TypeInterval) {
            return visitor.visitTypeInterval((TypeInterval) this, ctx);
        } else if (this instanceof TypeBag) {
            return visitor.visitTypeBag((TypeBag) this, ctx);
        } else if (this instanceof TypeList) {
            return visitor.visitTypeList((TypeList) this, ctx);
        } else if (this instanceof TypeSexp) {
            return visitor.visitTypeSexp((TypeSexp) this, ctx);
        } else if (this instanceof TypeTuple) {
            return visitor.visitTypeTuple((TypeTuple) this, ctx);
        } else if (this instanceof TypeStruct) {
            return visitor.visitTypeStruct((TypeStruct) this, ctx);
        } else if (this instanceof TypeAny) {
            return visitor.visitTypeAny((TypeAny) this, ctx);
        } else if (this instanceof TypeCustom) {
            return visitor.visitTypeCustom((TypeCustom) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
