/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.tools;

import com.amazon.ion.IonSystem;
import com.amazon.ion.IonValue;
import com.amazon.ion.system.IonSystemBuilder;
import com.amazon.ionsql.*;
import kotlin.jvm.functions.Function2;
import kotlin.sequences.SequencesKt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
        Map<String, Function2<Environment, List<ExprValue>, ExprValue>> funcs = new HashMap<>();
        funcs.put(
            // a very trivial extension function
            "bork",
            (env, funArgs) -> new IonExprValue(ion.newString("BORK!"))
        );

        EvaluatingCompiler evaluator = new EvaluatingCompiler(ion, funcs);

        // create a streaming source out of stdin (only allowing one pass)
        AtomicReference<Iterator<IonValue>> input = new AtomicReference<>(ion.iterate(System.in));
        ExprValue data = new SequenceExprValue(
            ion,
            () -> {
                Iterator<IonValue> iter = input.getAndSet(null);
                if (iter == null) {
                    throw new IllegalStateException("Cannot range over input more than once");
                }

                // adapt to Kotlin sequence
                return SequencesKt.map(SequencesKt.asSequence(iter), IonExprValue::new);
            }
        );

        String source = args[0];
        Expression expr = evaluator.compile(source);
        ExprValue results = expr.eval(
            // provide a mapping to the to standard input
            (name) -> "stdin".equals(name) ? data : null
        );

        for (ExprValue result : results) {
            System.err.println(result.getIonValue());
        }
    }
}
