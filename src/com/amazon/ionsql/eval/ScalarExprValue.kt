package com.amazon.ionsql.eval

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ionsql.util.seal

/** Basic implementation for scalar [ExprValue]. */
private class ScalarExprValue(
        override val type: ExprValueType,
        override val scalar: Scalar,
        private val ionValueFun: () -> IonValue
) : BaseExprValue() {

    // Pure functional (no-memoization because of overhead: both of the LazyXXX object and of locks.)
    override val ionValue get() = ionValueFun().seal()
}

fun nullExprValue(ion: IonSystem): ExprValue {
    return ScalarExprValue(ExprValueType.NULL, Scalar.empty()) {  ion.newNull() }
}

fun missingExprValue(ion: IonSystem): ExprValue {
    return ScalarExprValue(ExprValueType.MISSING, Scalar.empty()) { ion.newNull() }
}

fun booleanExprValue(value: Boolean, ion: IonSystem): ExprValue {
    val scalar = object: Scalar() { override fun booleanValue(): Boolean = value }
    return ScalarExprValue(ExprValueType.BOOL, scalar) { ion.newBool(value) }
}

fun stringExprValue(value: String, ion: IonSystem): ExprValue {
    val scalar = object: Scalar() { override fun stringValue(): String = value }
    return ScalarExprValue(ExprValueType.STRING, scalar) { ion.newString(value) }
}

fun integerExprValue(value: Long, ion: IonSystem): ExprValue {
    val scalar = object: Scalar() { override fun numberValue(): Number = value }
    return ScalarExprValue(ExprValueType.INT, scalar) { ion.newInt(value) }
}

fun floatExprValue(value: Double, ion: IonSystem): ExprValue {
    val scalar = object: Scalar() { override fun numberValue(): Number = value }
    return ScalarExprValue(ExprValueType.FLOAT, scalar) { ion.newFloat(value) }
}

// TODO implement other scalars, e.g. BLOB, CLOB, Timestamp, Symbol
