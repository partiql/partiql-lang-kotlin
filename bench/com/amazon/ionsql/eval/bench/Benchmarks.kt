/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

@file:JvmName("Benchmarks")

package com.amazon.ionsql.eval.bench

import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.CommandLineOptions
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.runner.options.TimeValue
import java.util.concurrent.TimeUnit

private fun booleanProperty(name: String): Boolean =
    (System.getProperty(name) ?: "").toLowerCase() == "true"

private val err = System.err

/** Entry point for running the benchmarks. */
fun main(args: Array<String>) {
    val options = CommandLineOptions(*args);

    val builder = OptionsBuilder()
        .parent(options)
        .forks(1)
        .jvmArgs("-Xms3g", "-Xmx3g", "-ea")
    if (options.includes.isEmpty()) {
        err.println("Including all benchmarks by default!");
        builder.include("^.*Bench");
    }
    if (booleanProperty("benchmarks.fast")) {
        err.println("Warning, running in fast mode!");
        builder
            .warmupIterations(1)
            .warmupTime(TimeValue(1, TimeUnit.SECONDS))
            .measurementIterations(1)
            .measurementTime(TimeValue(1, TimeUnit.SECONDS))
        ;
    } else if (booleanProperty("benchmarks.profile")) {
        err.println("Warning, running in profile mode (one long measured interval for profiling)!");
        builder
            .warmupIterations(10)
            .warmupTime(TimeValue(2, TimeUnit.SECONDS))
            .measurementIterations(1)
            .measurementTime(TimeValue(120, TimeUnit.SECONDS))
    }
    val flrPath = System.getProperty("benchmarks.jfr.path");
    if (flrPath != null) {
        err.println("Enabling Flight Recorder Path: " + flrPath);
        builder.jvmArgsAppend(
            "-XX:+UnlockCommercialFeatures",
            "-XX:+FlightRecorder",
            "-XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=" + flrPath
        );
    }

    Runner(builder.build()).run();
}
