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

import org.partiql.lang.*;
import org.partiql.lang.ast.ExprNode;
import org.partiql.lang.eval.*;
import org.partiql.lang.syntax.*;
import java.util.concurrent.*;
import org.openjdk.jmh.annotations.*;


/**
 * BenchmarkCommand for compilation only, doesn't account for parsing and does not evaluate the query
 */
public class CompilationBenchmark
{
    @State(Scope.Thread)
    public static class BenchmarkState extends BaseState
    {
        public CompilerPipeline pipeline;
        public ExprNode ast;

        @Override
        public void doSetup() throws Exception
        {
            SqlParser parser = new SqlParser(ion);
            ast = parser.parseExprNode(sql);

            pipeline = CompilerPipeline.builder(ion)
                .compileOptions(getCompileOptions())
                .build();
        }

        @Override
        public void doTearDown()
        {
            pipeline = null;
            ast = null;
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public Expression compilationOnly(BenchmarkState state)
    {
        return state.pipeline.compile(state.ast);
    }
}
