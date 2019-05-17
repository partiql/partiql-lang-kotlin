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

import org.partiql.lang.CompilerPipeline;
import org.partiql.lang.ast.ExprNode;
import org.partiql.lang.eval.*;
import org.partiql.lang.syntax.*;
import java.util.concurrent.*;
import org.openjdk.jmh.annotations.*;


/**
 * BenchmarkCommand for parsing and compilation, does not evaluate the query
 */
public class ParsingAndCompilationBenchmark
{
    @State(Scope.Thread)
    public static class BenchmarkState extends BaseState
    {
        public CompilerPipeline pipeline;
        public SqlParser parser;

        @Override
        public void doSetup() throws Exception
        {
            parser = new SqlParser(ion);
            pipeline = CompilerPipeline.builder(ion)
                .compileOptions(getCompileOptions())
                .build();
        }

        @Override
        public void doTearDown()
        {
            pipeline = null;
            parser = null;
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public Expression parsingAndCompilation(BenchmarkState state)
    {
        ExprNode ast = state.parser.parseExprNode(state.sql);
        return state.pipeline.compile(ast);
    }
}
