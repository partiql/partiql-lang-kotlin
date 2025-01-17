/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.parser;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.Statement;
import org.partiql.parser.internal.PartiQLParserDefault;
import org.partiql.spi.Context;
import org.partiql.spi.SourceLocations;
import org.partiql.spi.errors.PRuntimeException;

import java.util.List;

/**
 * Parses PartiQL text into abstract syntax trees (AST). This parser interface allows for parsing of multiple PartiQL
 * statements.
 *
 * @see PartiQLParser.Result
 * @see Statement
 * @see SourceLocations
 */
public interface PartiQLParser {

    /**
     * Parses the source text into an AST.
     * @param source the user's input
     * @param ctx a configuration object for the parser
     * @throws PRuntimeException when the {@link org.partiql.spi.errors.PErrorListener} defined in the context
     * throws a {@link PRuntimeException}, this method halts execution and propagates the exception.
     */
    @NotNull
    Result parse(@NotNull String source, @NotNull Context ctx) throws PRuntimeException;

    /**
     * Parses the source text into an AST with a default context.
     * @param source the user's input
     * @throws PRuntimeException when the {@link org.partiql.spi.errors.PErrorListener} defined in the context
     * throws a {@link PRuntimeException}, this method halts execution and propagates the exception.
     */
    @NotNull
    default Result parse(@NotNull String source) throws PRuntimeException {
        return parse(source, Context.standard());
    }

    /**
     * Parse result with the parsed AST {@link Statement}s and source locations {@link SourceLocations}.
     *
     * @see PartiQLParser#parse(String)
     * @see PartiQLParser#parse(String, Context)
     */
    final class Result {

        /**
         * The parsed AST {@link Statement}s.
         */
        @NotNull
        public List<Statement> statements;

        /**
         * The source locations of the parsed AST {@link Statement}s.
         */
        @NotNull
        public SourceLocations locations;

        /**
         * Constructs a parse result of AST {@link Statement}s and {@link SourceLocations}.
         * @param statements parsed AST {@link Statement}s
         * @param locations {@link SourceLocations} for the parsed statements
         */
        public Result(@NotNull List<Statement> statements, @NotNull SourceLocations locations) {
            this.statements = statements;
            this.locations = locations;
        }
    }

    /**
     * Static method to get a new builder.
     * @return a new builder instance
     */
    @NotNull
    static Builder builder() {
        return new Builder();
    }

    /**
     * Static method to get a new default parser.
     * @return a new default parser instance
     */
    @NotNull
    static PartiQLParser standard() {
        return new PartiQLParserDefault();
    }

    /**
     * A builder class to instantiate a {@link PartiQLParser}.
     */
    class Builder {
        @NotNull
        public PartiQLParser build() {
            return new PartiQLParserDefault();
        }
    }
}
