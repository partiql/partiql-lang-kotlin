package org.partiql.cli.utils

/** Throw an error when the converted ExprValue is NULL but the PartiQLValueType is not nullable */
internal class PartiQLtoExprValueNullException() : Exception("Mistakenly get a NULL ExprValue but the PartiQLValueType is not nullable when converting PartiQLValue to ExprValue.")

/** Throw an error when the converted PartiQLValue is not an integer but the ExprValueType is INT */
internal class ExprToPartiQLValueIntException() : Exception("Can't get an integer PartiQLValue but the ExprValueType is INT when converting ExprValue to PartiQLValue.")

/** Throw an error when the converted PartiQLValue is not a float but the ExprValueType is FLOAT */
internal class ExprToPartiQLValueFloatException() : Exception("Can't get a float PartiQLValue but the ExprValueType is FLOAT when converting ExprValue to PartiQLValue.")
