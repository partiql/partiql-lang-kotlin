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

package org.partiql.lang.syntax

import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTree
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.Companion.INSTANCE
import org.partiql.lang.types.CustomType
import org.partiql.lang.visitors.AntlrTreeToPartiQLVisitor
import java.nio.charset.StandardCharsets
import org.partiql.lang.generated.PartiQLParser as GeneratedParser
import org.partiql.lang.generated.PartiQLTokens as GeneratedLexer

class PartiQLParser(
    private val ion: IonSystem,
    customTypes: List<CustomType> = listOf()
) : Parser {

    override fun parseAstStatement(source: String): PartiqlAst.Statement {
        val tree = parseQuery(source)
        val visitor = AntlrTreeToPartiQLVisitor(ion)
        return visitor.visit(tree) as PartiqlAst.Statement
    }

    fun parseQuery(source: String): ParseTree {
        val parser = getParser(source)
        return parser.topQuery()
    }

    fun getParser(source: String): GeneratedParser {
        // Configure Lexer
        val inputStream = CharStreams.fromStream(source.byteInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
        val lexer = GeneratedLexer(inputStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(PartiQLLexer.TokenizeErrorListener.INSTANCE)

        // Configure Parser
        val tokens = CommonTokenStream(lexer)
        val parser = GeneratedParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(INSTANCE)
        return parser
    }

    @Deprecated("Please use parseAstStatement() instead--ExprNode is deprecated.")
    override fun parseExprNode(source: String): ExprNode {
        return parseAstStatement(source).toExprNode(ion)
    }

    @Deprecated("Please use parseAstStatement() instead--the return value can be deserialized to backward-compatible IonSexp.")
    override fun parse(source: String): IonSexp =
        @Suppress("DEPRECATION")
        org.partiql.lang.ast.AstSerializer.serialize(
            parseExprNode(source),
            org.partiql.lang.ast.AstVersion.V0, ion
        )

    class ParseErrorListener : BaseErrorListener() {
        @Throws(ParseCancellationException::class)
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            throw ParseException(msg, e)
        }

        class ParseException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)
        companion object {
            val INSTANCE = ParseErrorListener()
        }
    }
}
