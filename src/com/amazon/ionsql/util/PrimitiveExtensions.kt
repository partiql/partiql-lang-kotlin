package com.amazon.ionsql.util

import com.amazon.ion.IonSystem
import com.amazon.ionsql.eval.ExprValue

internal fun Boolean.exprValue(ion: IonSystem): ExprValue = ion.newBool(this).seal().exprValue()
internal fun String.exprValue(ion: IonSystem): ExprValue = ion.newString(this).seal().exprValue()
internal fun Int.exprValue(ion: IonSystem): ExprValue = ion.newInt(this).seal().exprValue()

