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

import org.partiql.lang.ast.ExprNode;
import org.partiql.lang.syntax.*;
import java.util.concurrent.*;
import org.openjdk.jmh.annotations.*;


/**
 * BenchmarkCommand for parsing only, does not compile or evaluate the query
 */
public class ParsingBenchmark
{
    @State(Scope.Thread)
    public static class BenchmarkState extends BaseState
    {
        public SqlParser parser;

        @Override
        public void doSetup() throws Exception
        {
            parser = new SqlParser(ion);
        }

        @Override
        public void doTearDown()
        {
            parser = null;
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public ExprNode parsingOnly(BenchmarkState state)
    {
        return state.parser.parseExprNode(state.sql);
    }
}
