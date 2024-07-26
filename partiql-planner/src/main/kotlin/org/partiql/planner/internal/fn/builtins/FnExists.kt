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
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class)
internal object Fn_EXISTS__BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "exists",
        returns = PartiQLValueType.BOOL,
        parameters = listOf(
            FnParameter("container", PartiQLValueType.BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val container = args[0].check<BagValue<*>>()
        val exists = container.iterator().hasNext()
        return boolValue(exists)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_EXISTS__LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "exists",
        returns = PartiQLValueType.BOOL,
        parameters = listOf(
            FnParameter("container", PartiQLValueType.LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val container = args[0].check<ListValue<*>>()
        val exists = container.iterator().hasNext()
        return boolValue(exists)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_EXISTS__SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "exists",
        returns = PartiQLValueType.BOOL,
        parameters = listOf(
            FnParameter("container", PartiQLValueType.SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val container = args[0].check<SexpValue<*>>()
        val exists = container.iterator().hasNext()
        return boolValue(exists)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_EXISTS__STRUCT__BOOL : Fn {

    override val signature = FnSignature(
        name = "exists",
        returns = PartiQLValueType.BOOL,
        parameters = listOf(
            FnParameter("container", PartiQLValueType.STRUCT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val container = args[0].check<StructValue<*>>()
        val exists = container.fields.iterator().hasNext()
        return boolValue(exists)
    }
}
