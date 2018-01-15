/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.tools.example;

import com.amazon.ion.IonSystem;
import com.amazon.ion.IonValue;
import com.amazon.ion.system.IonSystemBuilder;
import com.amazon.ionsql.eval.EvaluatingCompiler;
import com.amazon.ionsql.eval.EvaluationSession;
import com.amazon.ionsql.eval.ExprFunction;
import com.amazon.ionsql.eval.ExprValue;
import com.amazon.ionsql.eval.Expression;
import com.amazon.ionsql.eval.IonExprValue;
import com.amazon.ionsql.eval.SequenceExprValue;
import com.amazon.ionsql.util.BindingHelper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;

/**
 * An example embedding of the Ion SQL interpreter using Java.
 * <p/>
 *
 * This class is purely to demonstrate how to use the APIs from Java,
 * not to be a serious REPL or application.
 * <p/>
 *
 * This implementation binds a global name <tt>stdin</tt> that represents the data
 * passed into standard input as a collection of Ion values that are lazily materialized
 * as {@link IonValue}.
 * <p/>
 *
 * Consider a file named <tt>DATA.ion</tt> containing the following:
 *
 * <pre>
 *     {name: "mary", age: 10}
 *     {name: "zoe", age: 20}
 * </pre>
 *
 * The following would run a query over that file:
 *
 * <pre>
 *     $ java ExampleEvaluator 'SELECT name, bork() AS bork FROM stdin WHERE age > 18' < DATA.ion
 * </pre>
 *
 * Would produce:
 *
 * <pre>
 *     {name: "zoe", bork: "BORK!"}
 * </pre>
 *
 */
public class ExampleEvaluator {
    private ExampleEvaluator() {}

    /** Simple example driver. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ExampleEvaluator EXPRESSION");
        }

        IonSystem ion = IonSystemBuilder.standard().build();

        // additional functions to be made available to the query
        Map<String, ExprFunction> funcs = new HashMap<>();
        funcs.put(
            // a very trivial extension function
            "bork",
            (env, funArgs) -> new IonExprValue(ion.newString("BORK!"))
        );

        EvaluatingCompiler evaluator = new EvaluatingCompiler(ion, funcs);

        // create a streaming source out of stdin (only allowing one pass)
        Iterator<IonValue> iter = ion.iterate(System.in);
        Sequence<ExprValue> seq = SequencesKt.map(
            SequencesKt.asSequence(iter),
            IonExprValue::new
        );
        ExprValue data = new SequenceExprValue(ion, seq);

        String source = args[0];
        Expression expr = evaluator.compile(source);

        Map<String, ExprValue> globalVariables = new HashMap<>();
        globalVariables.put("stdin", data);

        ExprValue results = expr.eval(
            EvaluationSession.builder()
                //NOTE:  BindingHelp0er.bindingNameLookup(...) will throw the appropriate exception
                //in the event of an ambiguous case-insensitive binding match.
                .globals((bindingName) -> BindingHelper.lookupBinding(globalVariables, bindingName))
                .build()
        );

        for (ExprValue result : results) {
            System.err.println(result.getIonValue());
        }
    }
}
