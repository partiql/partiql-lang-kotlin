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
import org.partiql.spi.errors.PErrorListenerException;

import java.util.List;

/**
 * TODO
 */
public interface PartiQLParser {

    /**
     * Parses the [source] into an AST.
     * @param source the user's input
     * @param ctx a configuration object for the parser
     * @throws PErrorListenerException when the [org.partiql.spi.errors.PErrorListener] defined in the [ctx] throws an
     * [PErrorListenerException], this method halts execution and propagates the exception.
     */
    @NotNull
    Result parse(@NotNull String source, @NotNull Context ctx) throws PErrorListenerException;

    /**
     * Parses the [source] into an AST.
     * @param source the user's input
     * @throws PErrorListenerException when the [org.partiql.spi.errors.PErrorListener] defined in the context throws an
     * [PErrorListenerException], this method halts execution and propagates the exception.
     */
    default Result parse(@NotNull String source) throws PErrorListenerException {
        return parse(source, Context.standard());
    }

    /**
     * TODO
     */
    final class Result {

        /**
         * TODO
         */
        @NotNull
        public List<Statement> statements;

        /**
         * TODO
         */
        @NotNull
        public SourceLocations locations;

        /**
         * TODO
         * @param statements TODO
         * @param locations TODO
         */
        public Result(@NotNull List<Statement> statements, @NotNull SourceLocations locations) {
            this.statements = statements;
            this.locations = locations;
        }
    }

    /**
     * TODO
     * @return TODO
     */
    public static PartiQLParserBuilder builder() {
        return new PartiQLParserBuilder();
    }

    /**
     * TODO
     * @return TODO
     */
    public static PartiQLParser standard() {
        return new PartiQLParserDefault();
    }
}
