package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.ots_work.plugins.standard.types.CharType
import org.partiql.lang.ots_work.plugins.standard.types.DecimalType
import org.partiql.lang.ots_work.plugins.standard.types.FloatType
import org.partiql.lang.ots_work.plugins.standard.types.Int2Type
import org.partiql.lang.ots_work.plugins.standard.types.Int4Type
import org.partiql.lang.ots_work.plugins.standard.types.Int8Type
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.types.VarcharType


internal val ALL_TEXT_TYPES = listOf(SymbolType, StringType, CharType, VarcharType)
internal val ALL_NUMBER_TYPES = listOf(Int2Type, Int4Type, Int8Type, IntType, FloatType, DecimalType)

internal val defaultReturnTypesOfArithmeticOp = listOf(
    Int2Type.compileTimeType,
    Int4Type.compileTimeType,
    Int8Type.compileTimeType,
    IntType.compileTimeType,
    FloatType.compileTimeType,
    DecimalType.compileTimeType
)
