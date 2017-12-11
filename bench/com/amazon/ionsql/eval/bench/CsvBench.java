/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval.bench;

import com.amazon.ionsql.eval.*;
import com.amazon.ionsql.eval.io.DelimitedValues;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import sun.misc.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

// TODO Switch this to Kotlin--current builds use Javac annotation processing for JMH

public class CsvBench extends Base {
    // TODO clean this up an make it more general

    private static final String DIR = "build/private/bench-data/";

    @Param({
        "PARKING.csv.gz"
    })
    private String fileName;

    private byte[] data;

    @Setup(Level.Trial)
    public void loadData() throws Exception {
        try (final InputStream in = new GZIPInputStream(new FileInputStream(DIR + fileName))) {
            data = IOUtils.readFully(in, -1, false);
        }
        System.err.println("Read " + data.length + " bytes from " + fileName);
    }

    public enum Mode {
        RAW("data"),
        SELECT_NO_FILTER(
            "SELECT r[0], r[2] FROM data AS r"
        ),
        SELECT_WITH_FILTER(
            "SELECT r[0], r[2], r[8], r[9] FROM data AS r WHERE r[1] = 'HI'"
        ),
        COUNT(
            "count(SELECT * FROM data)"
        )
        ;

        public String query;

        private Mode(String query) {
            this.query = query;
        }
    }

    @Param
    private Mode queryMode;

    private EvaluatingCompiler compiler = new EvaluatingCompiler(ION);

    private ExprValue source() {
        return DelimitedValues.exprValue(
            ION,
            new InputStreamReader(new ByteArrayInputStream(data)),
            ",",
            false,
            DelimitedValues.ConversionMode.NONE
        );
    }

    private void materialize(ExprValue value, Blackhole bh) {
        ExprValueType type = value.getType();

        switch (type) {
            case LIST:
            case SEXP:
            case BAG:
            case STRUCT:
                for (ExprValue child : value) {
                    materialize(child, bh);
                }
                break;
            default:
                // TODO break down scalars
                bh.consume(value);
                break;
        }
    }

    @Benchmark
    public void process(Blackhole bh) {
        Expression expr = compiler.compile(queryMode.query);
        ExprValue value = expr.eval(
            EvaluationSession
                .builder()
                .globals((name) -> "data".equals(name) ? source() : null)
                .build()
        );
        materialize(value, bh);
    }
}
