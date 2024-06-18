// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.SMALLINT
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.BIGINT
import org.partiql.value.PType.Kind.TINYINT
import org.partiql.value.check
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import kotlin.experimental.and


internal object Fn_BITWISE_AND__TINYINT_TINYINT__TINYINT : Routine {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = TINYINT,
        parameters = listOf(
            FnParameter("lhs", TINYINT),
            FnParameter("rhs", TINYINT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<Int8Value>().value!!
        val arg1 = args[1].check<Int8Value>().value!!
        return int8Value(arg0 and arg1)
    }
}


internal object Fn_BITWISE_AND__SMALLINT_SMALLINT__SMALLINT : Routine {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = SMALLINT,
        parameters = listOf(
            FnParameter("lhs", SMALLINT),
            FnParameter("rhs", SMALLINT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<Int16Value>().value!!
        val arg1 = args[1].check<Int16Value>().value!!
        return int16Value(arg0 and arg1)
    }
}


internal object Fn_BITWISE_AND__INT_INT__INT : Routine {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = INT,
        parameters = listOf(
            FnParameter("lhs", INT),
            FnParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<Int32Value>().value!!
        val arg1 = args[1].check<Int32Value>().value!!
        return int32Value(arg0 and arg1)
    }
}


internal object Fn_BITWISE_AND__BIGINT_BIGINT__BIGINT : Routine {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = BIGINT,
        parameters = listOf(
            FnParameter("lhs", BIGINT),
            FnParameter("rhs", BIGINT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<Int64Value>().value!!
        val arg1 = args[1].check<Int64Value>().value!!
        return int64Value(arg0 and arg1)
    }
}


internal object Fn_BITWISE_AND__NUMERIC_NUMERIC__INT : Routine {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = INT,
        parameters = listOf(
            FnParameter("lhs", INT),
            FnParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<IntValue>().value!!
        val arg1 = args[1].check<IntValue>().value!!
        return intValue(arg0 and arg1)
    }
}
