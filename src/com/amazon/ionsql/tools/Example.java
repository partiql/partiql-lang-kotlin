/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.tools;

import com.amazon.ion.IonSystem;
import com.amazon.ion.system.IonSystemBuilder;
import com.amazon.ionsql.Bindings;
import com.amazon.ionsql.EvaluatingCompiler;
import com.amazon.ionsql.Expression;

import static java.util.Collections.emptyMap;

/**
 * An example embedding of the Ion SQL interpreter using Java.
 * <p/>
 * This class is purely to demonstrate how to use the APIs from Java,
 * not to be a serious REPL or application.
 */
public class Example {
    private Example() {}

    /** Simple example driver. */
    public static void main(final String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: Example EXPRESSION");
        }

        // TODO add examples of environment binding and adding user functions.

        IonSystem ion = IonSystemBuilder.standard().build();
        EvaluatingCompiler evaluator = new EvaluatingCompiler(ion, emptyMap());

        String source = args[0];
        Expression expr = evaluator.compile(source);
        expr.eval(Bindings.Companion.empty());
    }
}
