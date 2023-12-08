package org.partiql.lang.eval.internal

import org.partiql.lang.eval.BaseExprValue
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.OrdinalBindings
import org.partiql.lang.eval.internal.ext.namedValue

internal class ListExprValue(val values: Sequence<ExprValue>) : BaseExprValue() {
    override val type = ExprValueType.LIST
    override val ordinalBindings by lazy { OrdinalBindings.ofList(toList()) }
    override fun iterator() = values.mapIndexed { i, v -> v.namedValue(ExprValue.newInt(i)) }.iterator()

    constructor(values: List<ExprValue>) : this(values.asSequence())
}

internal class BagExprValue(val values: Sequence<ExprValue>) : BaseExprValue() {
    override val type = ExprValueType.BAG
    override val ordinalBindings = OrdinalBindings.EMPTY
    override fun iterator() = values.iterator()

    constructor(values: List<ExprValue>) : this(values.asSequence())
}

internal class SexpExprValue(val values: Sequence<ExprValue>) : BaseExprValue() {
    override val type = ExprValueType.SEXP
    override val ordinalBindings by lazy { OrdinalBindings.ofList(toList()) }
    override fun iterator() = values.mapIndexed { i, v -> v.namedValue(ExprValue.newInt(i)) }.iterator()

    constructor(values: List<ExprValue>) : this(values.asSequence())
}

/**
 * Returns an [ExprValue] created from a sequence of [seq]. Requires [type] to be a sequence type
 * (i.e. [ExprValueType.isSequence] == true).
 */
internal fun newSequenceExprValue(type: ExprValueType, seq: Sequence<ExprValue>): ExprValue {
    return when (type) {
        ExprValueType.LIST -> ListExprValue(seq)
        ExprValueType.BAG -> BagExprValue(seq)
        ExprValueType.SEXP -> SexpExprValue(seq)
        else -> error("Sequence type required")
    }
}
