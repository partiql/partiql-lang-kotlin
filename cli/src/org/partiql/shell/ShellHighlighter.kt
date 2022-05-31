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

import org.jline.builtins.Nano.SyntaxHighlighter
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import java.io.PrintStream
import java.nio.file.Path
import java.util.regex.Pattern

private val SUCCESS: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)
private val ERROR: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
private val INFO: AttributedStyle = AttributedStyle.DEFAULT
private val WARN: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)

class ShellHighlighter(private val syntaxFile: Path) : Highlighter {

    private val syntax = SyntaxHighlighter.build(syntaxFile, "PartiQL")

    override fun highlight(reader: LineReader, line: String): AttributedString = syntax.highlight(line)

    override fun setErrorPattern(errorPattern: Pattern?) {}

    override fun setErrorIndex(errorIndex: Int) {}
}

private fun ansi(string: String, style: AttributedStyle) = AttributedString(string, style).toAnsi()

fun PrintStream.success(string: String) {
    this.println(ansi(string, SUCCESS))
}

fun PrintStream.error(string: String) {
    this.println(ansi(string, ERROR))
}

fun PrintStream.info(string: String) {
    this.println(ansi(string, INFO))
}

fun PrintStream.warn(string: String) {
    this.println(ansi(string, WARN))
}
