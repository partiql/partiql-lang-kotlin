// ktlint-disable filename
@file:Suppress("ClassName", "DEPRECATION")

package org.partiql.system.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object FnInCollection : Function {

    override fun getName(): String {
        return "in_collection"
    }

    override fun getReturnType(args: Array<PType>): PType {
        return getInstance(args)!!.returns
    }

    override fun getParameters(): Array<Parameter> {
        return arrayOf(Parameter.dynamic("value"), Parameter.collection("collection"))
    }

    override fun getInstance(args: Array<PType>): Function.Instance? {
        val vType = args[0]
        val cType = args[1]
        if (cType.code() !in setOf(PType.UNKNOWN, PType.ARRAY, PType.BAG)) {
            return null
        }
        return object : Function.Instance(
            "in_collection",
            arrayOf(vType, cType),
            PType.bool(),
        ) {
            override fun invoke(args: Array<Datum>): Datum {
                val value = args[0]
                val collection = args[1]
                val iter = collection.iterator()
                while (iter.hasNext()) {
                    val v = iter.next()
                    if (Datum.comparator().compare(value, v) == 0) {
                        return Datum.bool(true)
                    }
                }
                return Datum.bool(false)
            }
        }
    }
}
