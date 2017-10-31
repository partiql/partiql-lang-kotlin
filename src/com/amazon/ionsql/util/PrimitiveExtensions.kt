package com.amazon.ionsql.util

import com.amazon.ion.*
import com.amazon.ionsql.eval.ExprValue

internal fun Boolean.exprValue(ion: IonSystem): ExprValue = ion.newBool(this).seal().exprValue()
internal fun String.exprValue(ion: IonSystem): ExprValue = ion.newString(this).seal().exprValue()
internal fun Int.exprValue(ion: IonSystem): ExprValue = ion.newInt(this).seal().exprValue()
internal fun Decimal.exprValue(ion: IonSystem): ExprValue = ion.newDecimal(this).seal().exprValue()
internal fun Timestamp.exprValue(ion: IonSystem): ExprValue = ion.newTimestamp(this).seal().exprValue()

