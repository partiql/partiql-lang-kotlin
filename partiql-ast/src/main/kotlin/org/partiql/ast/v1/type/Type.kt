package org.partiql.ast.v1.type

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public abstract class Type : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is TypeNull -> visitor.visitTypeNull(this, ctx)
        is TypeMissing -> visitor.visitTypeMissing(this, ctx)
        is TypeBool -> visitor.visitTypeBool(this, ctx)
        is TypeTinyint -> visitor.visitTypeTinyint(this, ctx)
        is TypeSmallint -> visitor.visitTypeSmallint(this, ctx)
        is TypeInt2 -> visitor.visitTypeInt2(this, ctx)
        is TypeInt4 -> visitor.visitTypeInt4(this, ctx)
        is TypeBigint -> visitor.visitTypeBigint(this, ctx)
        is TypeInt8 -> visitor.visitTypeInt8(this, ctx)
        is TypeInt -> visitor.visitTypeInt(this, ctx)
        is TypeReal -> visitor.visitTypeReal(this, ctx)
        is TypeFloat32 -> visitor.visitTypeFloat32(this, ctx)
        is TypeFloat64 -> visitor.visitTypeFloat64(this, ctx)
        is TypeDecimal -> visitor.visitTypeDecimal(this, ctx)
        is TypeNumeric -> visitor.visitTypeNumeric(this, ctx)
        is TypeChar -> visitor.visitTypeChar(this, ctx)
        is TypeVarchar -> visitor.visitTypeVarchar(this, ctx)
        is TypeString -> visitor.visitTypeString(this, ctx)
        is TypeSymbol -> visitor.visitTypeSymbol(this, ctx)
        is TypeBit -> visitor.visitTypeBit(this, ctx)
        is TypeBitVarying -> visitor.visitTypeBitVarying(this, ctx)
        is TypeByteString -> visitor.visitTypeByteString(this, ctx)
        is TypeBlob -> visitor.visitTypeBlob(this, ctx)
        is TypeClob -> visitor.visitTypeClob(this, ctx)
        is TypeDate -> visitor.visitTypeDate(this, ctx)
        is TypeTime -> visitor.visitTypeTime(this, ctx)
        is TypeTimeWithTz -> visitor.visitTypeTimeWithTz(this, ctx)
        is TypeTimestamp -> visitor.visitTypeTimestamp(this, ctx)
        is TypeTimestampWithTz -> visitor.visitTypeTimestampWithTz(this, ctx)
        is TypeInterval -> visitor.visitTypeInterval(this, ctx)
        is TypeBag -> visitor.visitTypeBag(this, ctx)
        is TypeList -> visitor.visitTypeList(this, ctx)
        is TypeSexp -> visitor.visitTypeSexp(this, ctx)
        is TypeTuple -> visitor.visitTypeTuple(this, ctx)
        is TypeStruct -> visitor.visitTypeStruct(this, ctx)
        is TypeAny -> visitor.visitTypeAny(this, ctx)
        is TypeCustom -> visitor.visitTypeCustom(this, ctx)
        else -> throw NotImplementedError()
    }
}
