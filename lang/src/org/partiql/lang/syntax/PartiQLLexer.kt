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

import com.amazon.ion.IonSystem
import org.antlr.v4.runtime.CommonTokenStream
import org.partiql.lang.util.getLexer
import org.partiql.lang.util.toPartiQLToken

internal class PartiQLLexer(private val ion: IonSystem) : Lexer {

    override fun tokenize(source: String): List<Token> {
        val lexer = getLexer(source, ion)
        val antlrTokens = CommonTokenStream(lexer)
        val tokens = mutableListOf<Token>()
        for (i in 0 until antlrTokens.numberOfOnChannelTokens) {
            tokens.add(antlrTokens[i].toPartiQLToken(ion = ion))
        }
        return tokens
    }
}
