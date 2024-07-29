package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Fn
import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.FnSignature
import org.partiql.value.BagValue
import org.partiql.value.ListValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.SexpValue
import org.partiql.value.StructValue
import org.partiql.value.check
import org.partiql.value.int32Value

@OptIn(PartiQLValueExperimental::class)
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
        return int32Value(container.count())
    }
}

@OptIn(PartiQLValueExperimental::class)
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
        return int32Value(container.count())
    }
}

@OptIn(PartiQLValueExperimental::class)
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
        return int32Value(container.count())
    }
}

@OptIn(PartiQLValueExperimental::class)
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
        return int32Value(container.fields.count())
    }
}
