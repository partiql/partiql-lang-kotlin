package org.partiql.examples;

import com.amazon.ion.IonSystem;
import com.amazon.ion.system.IonSystemBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import kotlin.OptIn;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.future.FutureKt;
import org.jetbrains.annotations.NotNull;
import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline;
import org.partiql.examples.util.Example;
import org.partiql.lang.compiler.PartiQLCompilerAsync;
import org.partiql.lang.compiler.PartiQLCompilerAsyncBuilder;
import org.partiql.lang.compiler.PartiQLCompilerPipelineAsync;
import org.partiql.lang.eval.BindingId;
import org.partiql.lang.eval.BindingName;
import org.partiql.lang.eval.Bindings;
import org.partiql.lang.eval.EvaluationSession;
import org.partiql.lang.eval.ExprValue;
import org.partiql.lang.eval.PartiQLResult;
import org.partiql.lang.eval.PartiQLStatementAsync;
import org.partiql.lang.eval.ProjectionIterationBehavior;
import org.partiql.lang.planner.EvaluatorOptions;
import org.partiql.lang.planner.GlobalResolutionResult;
import org.partiql.lang.planner.GlobalVariableResolver;
import org.partiql.lang.planner.PartiQLPlanner;
import org.partiql.lang.planner.PartiQLPlannerBuilder;
import org.partiql.lang.syntax.Parser;
import org.partiql.lang.syntax.PartiQLParserBuilder;

import java.io.PrintStream;

/**
 * This is an example of using PartiQLCompilerPipelineAsync in Java.
 * It is an experimental feature and is marked as such, with @OptIn, in this example.
 * Unfortunately, it seems like the Java does not recognize the OptIn annotation specified in Kotlin.
 * Java users will be able to access the experimental APIs freely, and not be warned at all.
 */
public class PartiQLCompilerPipelineAsyncJavaExample extends Example {

    public PartiQLCompilerPipelineAsyncJavaExample(@NotNull PrintStream out) {
        super(out);
    }

    @Override
    public void run() {
        final IonSystem ion = IonSystemBuilder.standard().build();

        final String myTable = "[ " +
                "{name: \"zoe\",  age: 12}," +
                "{name: \"jan\",  age: 20}," +
                "{name: \"bill\", age: 19}," +
                "{name: \"lisa\", age: 10}," +
                "{name: \"tim\",  age: 30}," +
                "{name: \"mary\", age: 19}" +
                "]";

        final Bindings<ExprValue> globalVariables = Bindings.<ExprValue>lazyBindingsBuilder().addBinding("myTable", () -> ExprValue.of(ion.singleValue(myTable))).build();

        final EvaluationSession session = EvaluationSession.builder()
                .globals(globalVariables)
                .build();

        final GlobalVariableResolver globalVariableResolver = new GlobalVariableResolver() {
            @NotNull
            @Override
            public GlobalResolutionResult resolveGlobal(@NotNull BindingId bindingId) {
                // In this example, we don't allow for qualified identifiers.
                List<BindingName> parts = bindingId.getParts();
                if (parts.size() == 1) {
                    return resolveGlobal(parts.get(0));
                }
                return GlobalResolutionResult.Undefined.INSTANCE;
            }

            @NotNull
            @Override
            public GlobalResolutionResult resolveGlobal(@NotNull BindingName bindingName) {
                ExprValue value = session.getGlobals().get(bindingName);

                if (value != null) {
                    return new GlobalResolutionResult.GlobalVariable(bindingName.getName());
                }
                else {
                    return GlobalResolutionResult.Undefined.INSTANCE;
                }
            }
        };

        final EvaluatorOptions evaluatorOptions = new EvaluatorOptions.Builder()
                .projectionIteration(ProjectionIterationBehavior.UNFILTERED)
                .build();

        final Parser parser = PartiQLParserBuilder.standard().build();

        @OptIn(markerClass = ExperimentalPartiQLCompilerPipeline.class)
        final PartiQLPlanner planner = PartiQLPlannerBuilder.standard().globalVariableResolver(globalVariableResolver).build();

        @OptIn(markerClass = ExperimentalPartiQLCompilerPipeline.class)
        final PartiQLCompilerAsync compiler = PartiQLCompilerAsyncBuilder.standard().options(evaluatorOptions).build();

        @OptIn(markerClass = ExperimentalPartiQLCompilerPipeline.class)
        final PartiQLCompilerPipelineAsync pipeline = new PartiQLCompilerPipelineAsync(
                parser, planner, compiler
        );

        String query = "SELECT t.name FROM myTable AS t WHERE t.age > 20";

        print("PartiQL query:", query);

        // Calling Kotlin coroutines from Java requires some additional libraries from `kotlinx.coroutines.future`
        // to return a `java.util.concurrent.CompletableFuture`. If a use case arises to call the
        // `PartiQLCompilerPipelineAsync` APIs directly from Java, we can add Kotlin functions that directly return
        // Java's async libraries (e.g. in https://stackoverflow.com/a/52887677).
        CompletableFuture<PartiQLStatementAsync> statementFuture = FutureKt.future(
                CoroutineScopeKt.CoroutineScope(EmptyCoroutineContext.INSTANCE),
                EmptyCoroutineContext.INSTANCE,
                CoroutineStart.DEFAULT,
                (scope, continuation) -> pipeline.compile(query, continuation)
        );

        PartiQLResult result;
        try {
            PartiQLStatementAsync statement = statementFuture.get();
            CompletableFuture<PartiQLResult> resultFuture = FutureKt.future(
                    CoroutineScopeKt.CoroutineScope(EmptyCoroutineContext.INSTANCE),
                    EmptyCoroutineContext.INSTANCE,
                    CoroutineStart.DEFAULT,
                    (scope, continuation) -> statement.eval(session, continuation)
            );
            result = resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
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
