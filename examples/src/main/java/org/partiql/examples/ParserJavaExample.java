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

import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonTextWriterBuilder;
import com.amazon.ionelement.api.SexpElement;
import org.jetbrains.annotations.NotNull;
import org.partiql.examples.util.Example;
import org.partiql.lang.domains.PartiqlAst;
import org.partiql.lang.syntax.Parser;
import org.partiql.lang.syntax.PartiQLParserBuilder;

import java.io.PrintStream;

/**
 * This example demonstrates producing a PartiQL AST from a PartiQL query.
 */
public class ParserJavaExample extends Example {

    public ParserJavaExample(@NotNull PrintStream out) {
        super(out);
    }

    @Override
    public void run() {
        // An instance of [SqlParser].
        final Parser parser = new PartiQLParserBuilder().build();

        // A query in string format
        final String query = "SELECT exampleField FROM exampleTable WHERE anotherField > 10";
        print("PartiQL query", query);

        // Use the SqlParser instance to parse the example query and get the AST as a PartiqlAst Statement.
        final PartiqlAst.Statement ast = parser.parseAstStatement(query);

        // We can transform the AST into SexpElement form to pretty print
        final SexpElement elements = ast.toIonElement();

        // Create an IonWriter to print the AST
        final StringBuilder astString = new StringBuilder();
        final IonWriter ionWriter = IonTextWriterBuilder.minimal().withPrettyPrinting().build(astString);

        // Now use the IonWriter to write the SexpElement AST into the StringBuilder and pretty print it
        elements.writeTo(ionWriter);
        print("Serialized AST", astString.toString());

        // We can also convert the SexpElement AST back into a PartiqlAst statement
        final PartiqlAst.Statement roundTrippedStatement = (PartiqlAst.Statement) PartiqlAst.Companion.transform(elements);
        // Verify that we have the original Partiql Ast statement
        if (!ast.equals(roundTrippedStatement)) {
            throw new RuntimeException("Expected statements to be the same");
        }

    }
}
