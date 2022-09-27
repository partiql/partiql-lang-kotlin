package ots.legacy.types

import ots.type.ScalarType

internal fun ScalarType.isNumeric() =
    this === Int2Type || this === Int4Type || this === Int8Type || this === IntType || this === FloatType || this === DecimalType

internal fun ScalarType.isText() =
    this === SymbolType || this === StringType || this === VarcharType || this === CharType

internal fun ScalarType.isLob(): Boolean =
    this === BlobType || this === ClobType

internal val numberTypesPrecedence = listOf(Int2Type, Int4Type, Int8Type, IntType, FloatType, DecimalType)
