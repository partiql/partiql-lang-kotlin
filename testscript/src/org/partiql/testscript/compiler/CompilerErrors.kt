package org.partiql.testscript.compiler

import org.partiql.testscript.TestScriptError
import org.partiql.testscript.parser.ScriptLocation
import org.partiql.testscript.parser.ast.TestNode

internal sealed class CompilerErrors(
        override val errorMessage: String,
        override val scriptLocation: ScriptLocation) : TestScriptError()

internal class TestIdNotUniqueError(
        testId: String,
        scriptLocation: ScriptLocation,
        otherScriptLocation: ScriptLocation)
    : CompilerErrors("testId: $testId not unique also found in: $otherScriptLocation", scriptLocation)

internal class NoTestMatchForAppendTestError(
        pattern: String,
        scriptLocation: ScriptLocation)
    : CompilerErrors("No testId matched the pattern: $pattern", scriptLocation)

internal class AppendingAppendedTestError(
        testId: String,
        otherLocation: ScriptLocation,
        scriptLocation: ScriptLocation)
    : CompilerErrors("testId: $testId was already appended on $otherLocation", scriptLocation)
