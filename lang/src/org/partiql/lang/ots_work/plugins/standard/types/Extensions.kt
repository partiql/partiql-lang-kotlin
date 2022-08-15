package org.partiql.lang.ots_work.plugins.standard.types

import org.partiql.lang.ots_work.interfaces.ScalarType

internal fun ScalarType.isNumeric() =
    this === Int2Type || this === Int4Type || this === Int8Type || this === IntType || this === FloatType || this === DecimalType

internal fun ScalarType.isText() =
    this === SymbolType || this === StringType || this === VarcharType || this === CharType

internal fun ScalarType.isLob(): Boolean =
    this === BlobType || this === ClobType
