// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum

/**
 * Function (operator) for the `IS MISSING` special form. Its name is hidden via [FunctionUtils.hide].
 */
private val name = FunctionUtils.hide("is_missing")
internal val Fn_IS_MISSING__ANY__BOOL = FnOverload.Builder(name)
    .returns(PType.bool())
    .addParameter(Parameter("value", PType.dynamic()))
    .isNullCall(false)
    .isMissingCall(false)
    .body { args ->
        Datum.bool(args[0].isMissing)
    }
    .build()
