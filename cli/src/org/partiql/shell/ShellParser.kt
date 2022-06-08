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

package org.partiql.shell

import org.jline.reader.EOFError
import org.jline.reader.ParsedLine
import org.jline.reader.Parser
import org.jline.reader.Parser.ParseContext.ACCEPT_LINE
import org.jline.reader.Parser.ParseContext.UNSPECIFIED
import org.jline.reader.impl.DefaultParser

/**
 * Line parser which executes on successive newlines, ';', or ``\n!!`` for printing the AST.
 */
object ShellParser : Parser {

    private val default = DefaultParser()
    private val nonTerminal = setOf(ACCEPT_LINE, UNSPECIFIED)
    private val suffixes = setOf("\n", ";", "!!")

    override fun parse(line: String, cursor: Int, ctx: Parser.ParseContext): ParsedLine {
        if (line.isBlank() || ctx == Parser.ParseContext.COMPLETE) {
            return default.parse(line, cursor, ctx)
        }
        if (nonTerminal.contains(ctx) && !line.endsWith(suffixes)) {
            throw EOFError(-1, -1, null)
        }
        return default.parse(line, cursor, ctx)
    }

    private fun String.endsWith(suffixes: Set<String>): Boolean {
        for (suffix in suffixes) {
            if (this.endsWith(suffix)) {
                return true
            }
        }
        return false
    }

    override fun isEscapeChar(ch: Char): Boolean = false
}
