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
import com.amazon.ion.system.*;
import org.partiql.lang.eval.*;
import org.partiql.testframework.testcar.*;
import static org.partiql.testframework.testcar.benchmark.BenchmarkExecutorKt.*;
import java.io.*;
import java.util.*;
import org.openjdk.jmh.annotations.*;

public abstract class BaseState
{
    abstract protected void doSetup() throws Exception;
    abstract protected void doTearDown() throws Exception;

    public IonSystem ion;
    public ExprValueFactory valueFactory;
    public IonStruct config;
    public String sql;
    public EvaluationData evaluationData;
    public IonValue result;

    /**
     * @return config field with the given name or an empty [IonStruct]
     */
    private IonStruct getConfigOptionByName(String fieldName) {
        return (IonStruct) Optional.ofNullable(config.get(fieldName)).orElse(ion.newEmptyStruct());
    }

    private EvaluationData getEvaluationData() {
        if(evaluationData == null) {
            evaluationData = new EvaluationData(
                valueFactory,
                getConfigOptionByName("env"),
                getConfigOptionByName("session"),
                getConfigOptionByName("compile_option"));
        }

        return evaluationData;
    }

    protected EvaluationSession getSession() {
        return getEvaluationData().getEvaluationSession();
    }

    protected CompileOptions getCompileOptions() {
        return getEvaluationData().getCompileOptions();
    }

    @Setup(Level.Trial)
    public void doParentSetup() throws Exception
    {
        ion = IonSystemBuilder.standard().build();
        valueFactory = ExprValueFactory.standard(ion);
        config = (IonStruct) ion.getLoader().load(new File(CONFIG_PATH)).get(0);
        sql = ((IonString) config.get("sql")).stringValue();
        result = null;

        doSetup();
    }

    @TearDown(Level.Trial)
    public void doParentTearDown() throws Exception
    {
        if(result != null){
            try(IonWriter writer = ion.newTextWriter(new FileOutputStream(new File(QUERY_OUTPUT_PATH))))
            {
                result.writeTo(writer);
            }
        }

        ion = null;
        valueFactory = null;
        config = null;
        sql = null;
        result = null;

        doTearDown();
    }
}
