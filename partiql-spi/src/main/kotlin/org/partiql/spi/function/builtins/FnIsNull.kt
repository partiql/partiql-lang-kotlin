// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Function (operator) for the `IS NULL` special form. Its name is hidden via [FunctionUtils.hide].
 */
private val name = FunctionUtils.hide("is_null")
internal val Fn_IS_NULL__ANY__BOOL = FnOverload.Builder(name)
    .addParameter(PType.dynamic())
    .returns(PType.bool())
    .isNullCall(false)
    .isMissingCall(false)
    .body { args ->
        if (args[0].isMissing) {
            return@body Datum.bool(true)
        }
        return@body Datum.bool(args[0].isNull)
    }
    .build()
