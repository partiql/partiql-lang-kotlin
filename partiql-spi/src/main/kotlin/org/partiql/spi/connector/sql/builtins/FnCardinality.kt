package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.BagValue
import org.partiql.value.ListValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.SexpValue
import org.partiql.value.StructValue
import org.partiql.value.check
import org.partiql.value.int32Value

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CARDINALITY__BAG__INT32 : Fn {

    override val signature = FnSignature(
        name = "cardinality",
        returns = PartiQLValueType.INT32,
        parameters = listOf(
            FnParameter("container", PartiQLValueType.BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val container = args[0].check<BagValue<*>>()
        val iter = container.iterator()
        var count = 0
        while (iter.hasNext()) {
            count ++
            iter.next()
        }
        return int32Value(count)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CARDINALITY__LIST__INT32 : Fn {

    override val signature = FnSignature(
        name = "cardinality",
        returns = PartiQLValueType.INT32,
        parameters = listOf(
            FnParameter("container", PartiQLValueType.LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val container = args[0].check<ListValue<*>>()
        val iter = container.iterator()
        var count = 0
        while (iter.hasNext()) {
            count ++
            iter.next()
        }
        return int32Value(count)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CARDINALITY__SEXP__INT32 : Fn {

    override val signature = FnSignature(
        name = "cardinality",
        returns = PartiQLValueType.INT32,
        parameters = listOf(
            FnParameter("container", PartiQLValueType.SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val container = args[0].check<SexpValue<*>>()
        val iter = container.iterator()
        var count = 0
        while (iter.hasNext()) {
            count ++
            iter.next()
        }
        return int32Value(count)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CARDINALITY__STRUCT__INT32 : Fn {

    override val signature = FnSignature(
        name = "cardinality",
        returns = PartiQLValueType.INT32,
        parameters = listOf(
            FnParameter("container", PartiQLValueType.STRUCT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val container = args[0].check<StructValue<*>>()
        val iter = container.fields.iterator()
        var count = 0
        while (iter.hasNext()) {
            count ++
            iter.next()
        }
        return int32Value(count)
    }
}
