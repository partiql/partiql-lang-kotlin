package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.physical.ExprThunkEnv
import org.partiql.lang.eval.physical.RelationThunkEnv

data class OperatorCompilerId(
    val implementationName: String,
    val implementsClass: Class<*>
)

class OperatorCompilerCatalog(operatorList: List<PhysicalOperatorCompiler<*>>) {
    private val operatorMap = operatorList.associateBy { it.id }

    fun <T : PartiqlPhysical.Bexpr> getOperator(
        implementationName: String,
        operatorClass: Class<T>
    ): PhysicalOperatorCompiler<T> {
        val id = OperatorCompilerId(implementationName, operatorClass)
        @Suppress("UNCHECKED_CAST")
        return operatorMap[id] as? PhysicalOperatorCompiler<T>
            ?: error("Could not locate compiler for operator '$operatorClass' named '$implementationName'")
    }
}

// DL TODO: rename
interface ThunkConverter {
    // DL TODO: rename, kdoc
    fun compile(bexpr: PartiqlPhysical.Bexpr): RelationThunkEnv
    // DL TODO: rename, kdoc
    fun compile(expr: PartiqlPhysical.Expr): ExprThunkEnv
}

// DL TODO: rename, kdoc
interface PhysicalOperatorCompiler<TOperator : PartiqlPhysical.Bexpr> {
    val id: OperatorCompilerId
    fun compile(operator: TOperator, compiler: ThunkConverter, valueFactory: ExprValueFactory): RelationThunkEnv
}

/**
 * Reduces some syntactic overhead of creating physical operator compilers.
 */
inline fun <reified TOperator : PartiqlPhysical.Bexpr> createPhysicalOperatorCompiler(
    implementationName: String,
    crossinline compileBlock: (TOperator, ThunkConverter, ExprValueFactory) -> RelationThunkEnv
): PhysicalOperatorCompiler<TOperator> =
    object : PhysicalOperatorCompiler<TOperator> {

        override fun compile(
            operator: TOperator,
            compiler: ThunkConverter,
            valueFactory: ExprValueFactory
        ): RelationThunkEnv =
            compileBlock(operator, compiler, valueFactory)

        override val id: OperatorCompilerId get() = OperatorCompilerId(implementationName, TOperator::class.java)
    }
