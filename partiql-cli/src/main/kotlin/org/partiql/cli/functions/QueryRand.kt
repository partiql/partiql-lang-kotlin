package org.partiql.cli.functions

import com.amazon.ion.IonSystem
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType
import kotlin.random.Random

class QueryRand : ExprFunction {
    private val ion: IonSystem

    constructor(ion: IonSystem) {
        this.ion = ion
    }

    override val signature = FunctionSignature(
        name = "query_rand",
        requiredParameters = listOf(),
        returnType = StaticType.INT
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val rand = Random.nextInt()
        return ExprValue.Companion.newInt(rand)
    }
}
