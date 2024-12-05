package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

/**
 * DATE_DIFF i
 */
internal object FnDateDiff : Function {

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getReturnType(args: Array<PType>): PType {
        TODO("Not yet implemented")
    }

    override fun getInstance(args: Array<PType>): Function.Instance? {
        return super.getInstance(args)
    }

    override fun getParameters(): Array<Parameter> {
        return super.getParameters()
    }
}
