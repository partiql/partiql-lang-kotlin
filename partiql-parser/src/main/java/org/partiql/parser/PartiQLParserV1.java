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
import org.partiql.ast.v1.Statement;
import org.partiql.parser.internal.PartiQLParserDefaultV1;
import org.partiql.spi.Context;
import org.partiql.spi.SourceLocation;
import org.partiql.spi.SourceLocations;
import org.partiql.spi.errors.PError;
import org.partiql.spi.errors.PErrorKind;
import org.partiql.spi.errors.PErrorListenerException;
import org.partiql.spi.errors.Severity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: Rename to PartiQLParser
 */
public interface PartiQLParserV1 {

    /**
     * Parses the [source] into an AST.
     * @param source the user's input
     * @param ctx a configuration object for the parser
     * @throws PErrorListenerException when the [org.partiql.spi.errors.PErrorListener] defined in the [ctx] throws an
     * [PErrorListenerException], this method halts execution and propagates the exception.
     * @see PartiQLParserV1#parseSingle(String, Context)
     */
    @NotNull
    Result parse(@NotNull String source, @NotNull Context ctx) throws PErrorListenerException;

    /**
     * TODO
     * @param source TODO
     * @param ctx TODO
     * @return TODO
     * @throws PErrorListenerException TODO
     * @see PartiQLParserV1#parse(String, Context)
     */
    @NotNull
    default Result parseSingle(@NotNull String source, @NotNull Context ctx) throws PErrorListenerException {
        Result result = parse(source, ctx);
        if (result.statements.size() != 1) {
            SourceLocation location;
            if (result.statements.size() > 1) {
                location = result.locations.get(result.statements.get(1).tag);
            } else {
                location = null;
            }
            Map<String, Object> properties = new HashMap<String, Object>() {{
                put("EXPECTED_TOKENS", new ArrayList<String>() {{
                    add("EOF");
                }});
            }};
            PError pError = new PError(PError.UNEXPECTED_TOKEN, Severity.ERROR(), PErrorKind.SYNTAX(), location, properties);
            ctx.getErrorListener().report(pError);
        }
        return result;
    }

    /**
     * Parses the [source] into an AST.
     * @param source the user's input
     * @throws PErrorListenerException when the [org.partiql.spi.errors.PErrorListener] defined in the context throws an
     * [PErrorListenerException], this method halts execution and propagates the exception.
     */
    @NotNull
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
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public static PartiQLParserV1 standard() {
        return new PartiQLParserDefaultV1();
    }

    /**
     * A builder class to instantiate a {@link PartiQLParserV1}.
     */
    public class Builder {
        // TODO: Can this be replaced with Lombok?
        // TODO: https://github.com/partiql/partiql-lang-kotlin/issues/1632

        /**
         * TODO
         * @return TODO
         */
        @NotNull
        public PartiQLParserV1 build() {
            return new PartiQLParserDefaultV1();
        }
    }
}
