package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.BagValue
import org.partiql.value.ListValue
import org.partiql.value.Datum
import org.partiql.value.PType.Kind
import org.partiql.value.SexpValue
import org.partiql.value.StructValue
import org.partiql.value.check
import org.partiql.value.int32Value


internal object Fn_CARDINALITY__BAG__INT : Routine {

    override val signature = FnSignature(
        name = "cardinality",
        returns = PType.Kind.INT,
        parameters = listOf(
            FnParameter("container", PType.Kind.BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0].check<BagValue<*>>()
        return int32Value(container.count())
    }
}


internal object Fn_CARDINALITY__LIST__INT : Routine {

    override val signature = FnSignature(
        name = "cardinality",
        returns = PType.Kind.INT,
        parameters = listOf(
            FnParameter("container", PType.Kind.LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0].check<ListValue<*>>()
        return int32Value(container.count())
    }
}


internal object Fn_CARDINALITY__SEXP__INT : Routine {

    override val signature = FnSignature(
        name = "cardinality",
        returns = PType.Kind.INT,
        parameters = listOf(
            FnParameter("container", PType.Kind.SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0].check<SexpValue<*>>()
        return int32Value(container.count())
    }
}


internal object Fn_CARDINALITY__STRUCT__INT : Routine {

    override val signature = FnSignature(
        name = "cardinality",
        returns = PType.Kind.INT,
        parameters = listOf(
            FnParameter("container", PType.Kind.STRUCT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0].check<StructValue<*>>()
        return int32Value(container.fields.count())
    }
}
