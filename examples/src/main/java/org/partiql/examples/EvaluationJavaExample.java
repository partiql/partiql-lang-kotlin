/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.examples;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.partiql.examples.util.Example;
import org.partiql.lang.CompilerPipeline;
import org.partiql.lang.eval.Bindings;
import org.partiql.lang.eval.EvaluationSession;
import org.partiql.lang.eval.ExprValue;
import org.partiql.lang.eval.Expression;

import java.io.PrintStream;
import java.util.Map;

import static java.util.Collections.singletonMap;

/**
 * This example demonstrates evaluating a PartiQL query against values defined in the global environment.
 */
public class EvaluationJavaExample extends Example {

    public EvaluationJavaExample(@NotNull PrintStream out) {
        super(out);
    }

    @Override
    public void run() {

        // An instance of [CompilerPipeline].
        final CompilerPipeline pipeline = CompilerPipeline.standard();

        // Compiles a simple expression containing a reference to a global variable.
        final String query = "'Hello, ' || user_name";
        print("PartiQL query:", query);
        final Expression e = pipeline.compile(query);

        // This is the value of the global variable.
        final String userName = "Homer Simpson";
        final ExprValue usernameValue = ExprValue.newString(userName);

        // [Bindings.ofMap] can be used to construct a [Bindings] instance of
        // bindings with previously materialized values.
        final Map<String, ExprValue> globals = singletonMap("user_name", usernameValue);
        final Bindings<ExprValue> globalVariables = Bindings.ofMap(globals);
        print("global variables:", globals);

        // Include globalVariables when building the EvaluationSession.
        final EvaluationSession session = EvaluationSession.Companion.build((builder) -> {
            builder.globals(globalVariables);
            return Unit.INSTANCE;
        });

        // Evaluate the compiled expression with the session containing the global variables.
        final ExprValue result = e.eval(session);
        print("result", result.toString());

    }

}
