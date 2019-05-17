/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.testframework.benchmarks;

import com.amazon.ion.*;
import org.partiql.lang.*;
import org.partiql.lang.eval.*;
import java.util.concurrent.*;
import org.openjdk.jmh.annotations.*;


/**
 * End to end BenchmarkCommand accounting for parsing, compilation and evaluation
 */
public class EndToEndBenchmark
{
    @State(Scope.Thread)
    public static class BenchmarkState extends BaseState
    {
        public CompilerPipeline pipeline;
        public Expression expr;
        public EvaluationSession session;

        @Override
        public void doSetup() throws Exception
        {
            pipeline = CompilerPipeline.builder(ion)
                .compileOptions(getCompileOptions())
                .build();

            session = getSession();
        }

        @Override
        public void doTearDown()
        {
            pipeline = null;
            expr = null;
            session = null;
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public IonValue endToEnd(BenchmarkState state)
    {
        IonValue ionValue = state.pipeline.compile(state.sql).eval(state.session).getIonValue();
        state.result = ionValue;
        return ionValue;
    }
}
