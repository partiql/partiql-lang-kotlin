/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.tools;

import com.amazon.ion.IonSexp;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonSystemBuilder;
import com.amazon.ion.system.IonTextWriterBuilder;
import com.amazon.ionsql.*;

import java.io.OutputStream;
import java.util.List;

/**
 * An example embedding the Ion SQL parser using Java.
 * <p/>
 * This class is purely to demonstrate how to use the APIs from Java,
 * not to be a serious REPL or application.
 */
public class ExampleParse {
    private ExampleParse() {}

    private static final IonTextWriterBuilder PRETTY = IonTextWriterBuilder.pretty();

    /** Simple example driver. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ExampleParse EXPRESSION");
        }

        IonSystem ion = IonSystemBuilder.standard().build();
        Lexer lexer = new IonSqlLexer(ion);
        Parser parser = new IonSqlParser(ion);
        // generate
        String source = args[0];
        List<Token> tokens = lexer.tokenize(source);
        IonSexp ast = parser.parse(tokens);

        // Dump the AST
        try (IonWriter out = PRETTY.build((OutputStream) System.out)) {
            ast.writeTo(out);
        }
    }
}
