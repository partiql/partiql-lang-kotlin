package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

/**
 * DATE_ADD is not SQL spec and `datetime + interval` should be used; I'm only adding this for conformance tests.
 */
internal object FnDateAdd : Function {

    override fun getInstance(args: Array<PType>): Function.Instance? {
        return super.getInstance(args)
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getParameters(): Array<Parameter> {
        return super.getParameters()
    }

    override fun getReturnType(args: Array<PType>): PType {
        TODO("Not yet implemented")
    }
}
