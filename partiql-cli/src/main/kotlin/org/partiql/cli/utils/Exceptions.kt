package org.partiql.cli.utils

import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/** Throw an error when the there's a type mismatch when converting PartiQLValue to ExprValue */
@OptIn(PartiQLValueExperimental::class)
internal class PartiQLtoExprValueTypeMismatchException(expectedType: String, actualType: PartiQLValueType) :
    Exception("When converting PartiQLValue to ExprValue, expected a $expectedType, but received $actualType")

/** Throw an error when the there's a type mismatch when converting ExprValue to PartiQLValue */
@OptIn(PartiQLValueExperimental::class)
internal class ExprToPartiQLValueTypeMismatchException(expectedType: PartiQLValueType, actualType: PartiQLValueType) :
    Exception("When converting ExprValue to PartiQLValue, expected a $expectedType, but received $actualType")
