// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.SMALLINT
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.BIGINT
import org.partiql.value.PartiQLValueType.TINYINT


internal object Fn_POS__TINYINT__TINYINT : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = TINYINT,
        parameters = listOf(FnParameter("value", TINYINT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}


internal object Fn_POS__SMALLINT__SMALLINT : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = SMALLINT,
        parameters = listOf(FnParameter("value", SMALLINT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}


internal object Fn_POS__INT__INT : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = INT,
        parameters = listOf(FnParameter("value", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}


internal object Fn_POS__BIGINT__BIGINT : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = BIGINT,
        parameters = listOf(FnParameter("value", BIGINT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}


internal object Fn_POS__NUMERIC__INT : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = INT,
        parameters = listOf(FnParameter("value", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}


internal object Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FnParameter("value", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}


internal object Fn_POS__FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = FLOAT32,
        parameters = listOf(FnParameter("value", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}


internal object Fn_POS__FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "pos",
        returns = FLOAT64,
        parameters = listOf(FnParameter("value", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}
