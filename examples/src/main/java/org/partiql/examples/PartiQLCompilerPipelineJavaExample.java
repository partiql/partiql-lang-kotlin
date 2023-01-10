package org.partiql.examples;

import com.amazon.ion.IonSystem;
import com.amazon.ion.system.IonSystemBuilder;
import kotlin.OptIn;
import org.jetbrains.annotations.NotNull;
import org.partiql.annotations.PartiQLExperimental;
import org.partiql.examples.util.Example;
import org.partiql.lang.compiler.PartiQLCompiler;
import org.partiql.lang.compiler.PartiQLCompilerBuilder;
import org.partiql.lang.compiler.PartiQLCompilerPipeline;
import org.partiql.lang.eval.*;
import org.partiql.lang.planner.*;
import org.partiql.lang.syntax.Parser;
import org.partiql.lang.syntax.PartiQLParserBuilder;

import java.io.PrintStream;


public class PartiQLCompilerPipelineJavaExample extends Example {

    public PartiQLCompilerPipelineJavaExample(@NotNull PrintStream out) {
        super(out);
    }

    final IonSystem ion = IonSystemBuilder.standard().build();

    final String myTable = "[ " +
            "{name: \"zoe\",  age: 12}," +
            "{name: \"jan\",  age: 20}," +
            "{name: \"bill\", age: 19}," +
            "{name: \"lisa\", age: 10}," +
            "{name: \"tim\",  age: 30}," +
            "{name: \"mary\", age: 19}" +
            "]";

    final Bindings<ExprValue> globalVariables = Bindings.<ExprValue>lazyBindingsBuilder().addBinding("myTable", () -> {
        ExprValue exprValue = ExprValue.of(ion.singleValue(myTable));
        return exprValue;
    }).build();

    final EvaluationSession session = EvaluationSession.builder()
            .globals(globalVariables)
            .build();

    final GlobalVariableResolver globalVariableResolver = bindingName -> {
        ExprValue value = session.getGlobals().get(bindingName);

        if (value != null) {
            return new GlobalResolutionResult.GlobalVariable(bindingName.getName());
        }
        else {
            return GlobalResolutionResult.Undefined.INSTANCE;
        }
    };

    final EvaluatorOptions evaluatorOptions = new EvaluatorOptions.Builder()
            .projectionIteration(ProjectionIterationBehavior.UNFILTERED)
            .build();

    final Parser parser = PartiQLParserBuilder.standard().ionSystem(ion).build();
    final PartiQLPlanner planner = PartiQLPlannerBuilder.standard().globalVariableResolver(globalVariableResolver).build();
    final PartiQLCompiler compiler = PartiQLCompilerBuilder.standard().ionSystem(ion).options(evaluatorOptions).build();

    final PartiQLCompilerPipeline pipeline = new PartiQLCompilerPipeline(
            parser, planner, compiler
    );

    @Override
    public void run() {
        String query = "SELECT t.name FROM myTable AS t WHERE t.age > 20";

        print("PartiQL query:", query);
        PartiQLResult result = pipeline.compile(query).eval(session);
        ExprValue exprValue = null;
        if (result instanceof PartiQLResult.Value) {
            exprValue = ((PartiQLResult.Value) result).getValue();
        }
        else {
            System.out.println("DML and Explain not covered in this example");
        }

        print("result", exprValue);
    }
}