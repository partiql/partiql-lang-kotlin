package org.partiql.examples

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline
import org.partiql.examples.util.Example
import org.partiql.lang.compiler.PartiQLCompilerPipeline
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.ProjectionIterationBehavior
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.planner.GlobalResolutionResult
import org.partiql.lang.planner.GlobalVariableResolver
import java.io.PrintStream

/**
 * This example demonstrate how to use PartiQLCompilerPipeline. This feature is currently in experimental stage.
 * To use this experimental feature, you would have to explicitly opt in to avoid the compiler warning.
 * One way to do so is to add the `Optin(Experimental<X>::class) before the class. where <X> is the feature name.
 * Also see: https://kotlinlang.org/docs/opt-in-requirements.html#module-wide-opt-in
 */
class PartiQLCompilerPipelineExample(out: PrintStream) : Example(out) {

    private val myIonSystem = IonSystemBuilder.standard().build()

    private val myTable = """[ 
        {name: "zoe",  age: 12},
        {name: "jan",  age: 20},
        {name: "bill", age: 19},
        {name: "lisa", age: 10},
        {name: "tim",  age: 30},
        {name: "mary", age: 19}
    ]"""

    private val globalVariables = Bindings.buildLazyBindings<ExprValue> {
        addBinding("myTable") {
            ExprValue.of(
                myIonSystem.singleValue(myTable)
            )
        }
    }

    private val session = EvaluationSession.builder()
        .globals(globalVariables)
        .build()

    private val globalVariableResolver = GlobalVariableResolver {
        val value = session.globals[it]
        if (value != null) {
            GlobalResolutionResult.GlobalVariable(it.name)
        } else {
            GlobalResolutionResult.Undefined
        }
    }

    private val evaluatorOptions = EvaluatorOptions.Builder()
        .projectionIteration(ProjectionIterationBehavior.UNFILTERED)
        .build()

    @OptIn(ExperimentalPartiQLCompilerPipeline::class)
    private val partiQLCompilerPipeline = PartiQLCompilerPipeline.build {
        planner
            .globalVariableResolver(globalVariableResolver)
        compiler
            .options(evaluatorOptions)
    }

    override fun run() {
        val query = "SELECT t.name FROM myTable AS t WHERE t.age > 20"

        print("PartiQL query:", query)
        @OptIn(ExperimentalPartiQLCompilerPipeline::class)
        val exprValue = when (val result = partiQLCompilerPipeline.compile(query).eval(session)) {
            is PartiQLResult.Value -> result.value
            is PartiQLResult.Delete,
            is PartiQLResult.Explain.Domain,
            is PartiQLResult.Insert,
            is PartiQLResult.Replace -> TODO("DML and Explain not covered in this example")
        }
        print("result", exprValue)
    }
}
