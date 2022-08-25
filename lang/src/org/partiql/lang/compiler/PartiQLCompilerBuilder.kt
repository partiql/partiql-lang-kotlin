package org.partiql.lang.compiler

import com.amazon.ion.IonSystem
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ThunkReturnTypeAssertions
import org.partiql.lang.eval.builtins.DynamicLookupExprFunction
import org.partiql.lang.eval.builtins.createBuiltinFunctions
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.physical.operators.DEFAULT_RELATIONAL_OPERATOR_FACTORIES
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactory
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.types.CustomType
import org.partiql.lang.types.TypedOpParameter

class PartiQLCompilerBuilder(val ion: IonSystem) {

    private var valueFactory: ExprValueFactory = ExprValueFactory.standard(ion)
    private var options: EvaluatorOptions = EvaluatorOptions.standard()
    private var customTypes: List<CustomType> = mutableListOf()
    private val customFunctions: MutableList<ExprFunction> = mutableListOf()
    private val customProcedures: MutableMap<String, StoredProcedure> = mutableMapOf()
    private val customOperatorFactories: MutableList<RelationalOperatorFactory> = mutableListOf()

    companion object {

        @JvmStatic
        fun standard(ion: IonSystem) = PartiQLCompilerBuilder(ion)
    }

    fun build(): PartiQLCompiler {
        if (options.thunkOptions.thunkReturnTypeAssertions == ThunkReturnTypeAssertions.ENABLED) {
            TODO("ThunkReturnTypeAssertions.ENABLED requires a static type pass")
        }
        return PartiQLCompilerImpl(
            valueFactory = valueFactory,
            evaluatorOptions = options,
            customTypedOpParameters = customTypes.toMap(),
            functions = allFunctions(),
            procedures = customProcedures,
            operatorFactories = allOperatorFactories()
        )
    }

    fun options(evaluatorOptions: EvaluatorOptions) = this.apply {
        options = evaluatorOptions
    }

    /**
     * This will be replaced by the open type system.
     * https://github.com/partiql/partiql-lang-kotlin/milestone/4
     */
    internal fun addFunction(function: ExprFunction) = this.apply {
        customFunctions.add(function)
    }

    /**
     * This will be replaced by the open type system.
     * https://github.com/partiql/partiql-lang-kotlin/milestone/4
     */
    internal fun customDataTypes(types: List<CustomType>) = this.apply {
        customTypes = types
    }

    /**
     * This will be replaced by the open type system.
     * https://github.com/partiql/partiql-lang-kotlin/milestone/4
     */
    internal fun addProcedure(procedure: StoredProcedure) = this.apply {
        customProcedures[procedure.signature.name] = procedure
    }

    internal fun addOperatorFactory(operator: RelationalOperatorFactory) = this.apply {
        customOperatorFactories.add(operator)
    }

    // --- Internal ----------------------------------

    // To be replaced by OTS — https://media.giphy.com/media/3o6Zt3l22wlJ0HLpUk/giphy.gif
    private fun allFunctions(): Map<String, ExprFunction> {
        val builtins = createBuiltinFunctions(valueFactory)
        val allFunctions = builtins + customFunctions + DynamicLookupExprFunction()
        return allFunctions.associateBy { it.signature.name }
    }

    private fun List<CustomType>.toMap(): Map<String, TypedOpParameter> = this.associateBy(
        keySelector = { it.name },
        valueTransform = { it.typedOpParameter }
    )

    private fun allOperatorFactories() = (DEFAULT_RELATIONAL_OPERATOR_FACTORIES + customOperatorFactories).apply {
        groupBy { it.key }.entries.firstOrNull { it.value.size > 1 }?.let {
            error(
                "More than one BindingsOperatorFactory for ${it.key.operator} named '${it.value}' was specified."
            )
        }
    }.associateBy { it.key }
}
