// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum

internal val Fn_IS_MAP__ANY__BOOL = FunctionUtils.hidden(
    name = "is_map",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("key_type_code", PType.integer()),
        Parameter("value_type_code", PType.integer()),
        Parameter("value", PType.dynamic()),
    ),
) { args ->
    val expectedKeyTypeCode = args[0].int
    val expectedValueTypeCode = args[1].int
    val value = args[2]
    if (value.type.code() != PType.MAP) {
        Datum.bool(false)
    } else {
        val keyMatch = value.type.keyType.code() == expectedKeyTypeCode
        val valueMatch = expectedValueTypeCode == PType.DYNAMIC || value.type.valueType.code() == expectedValueTypeCode
        Datum.bool(keyMatch && valueMatch)
    }
}
