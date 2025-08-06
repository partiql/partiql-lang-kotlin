// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.FunctionUtils.logicalNot

/**
 * This is the boolean NOT predicate. Its name is hidden via the use of [FunctionUtils.hide].
 */
private val name = FunctionUtils.hide("not")
internal val Fn_NOT__BOOL__BOOL = FnOverload.Builder(name)
    .isNullCall(true)
    .isMissingCall(false)
    .addParameter(Parameter("value", PType.dynamic()))
    .returns(PType.bool())
    .body { args ->
        logicalNot(args[0])
    }
    .build()
