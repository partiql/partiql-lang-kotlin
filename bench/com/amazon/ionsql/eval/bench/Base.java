/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval.bench;

import com.amazon.ion.IonSystem;
import com.amazon.ion.system.IonSystemBuilder;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

// TODO Switch this to Kotlin--current builds use Javac annotation processing for JMH

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public abstract class Base {
    protected static final IonSystem ION = IonSystemBuilder.standard().build();
}
