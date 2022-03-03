package org.partiql.testscript.compiler

import com.amazon.ion.IonType
import org.partiql.testscript.TestScriptError
import org.partiql.testscript.parser.ScriptLocation

internal sealed class CompilerErrors(
    override val errorMessage: String,
    override val scriptLocation: ScriptLocation
) : TestScriptError()

internal class TestIdNotUniqueError(
    testId: String,
    scriptLocation: ScriptLocation,
    otherScriptLocation: ScriptLocation
) :
    CompilerErrors("testId: $testId not unique also found in: $otherScriptLocation", scriptLocation)

internal class NoTestMatchForAppendTestError(
    pattern: String,
    scriptLocation: ScriptLocation
) :
    CompilerErrors("No testId matched the pattern: $pattern", scriptLocation)

internal class AppendingAppendedTestError(
    testId: String,
    otherLocation: ScriptLocation,
    scriptLocation: ScriptLocation
) :
    CompilerErrors("testId: $testId was already appended on $otherLocation", scriptLocation)

internal class FileSetDefaultEnvironmentNotSingleValue(path: String, scriptLocation: ScriptLocation) :
    CompilerErrors("Environment file $path is not a single value", scriptLocation)

internal class FileSetDefaultEnvironmentNotExists(path: String, scriptLocation: ScriptLocation) :
    CompilerErrors("Environment file $path does not exist", scriptLocation)

internal class FileSetDefaultEnvironmentNotStruct(path: String, actualType: IonType, scriptLocation: ScriptLocation) :
    CompilerErrors("Environment file $path does not contain a STRUCT but a $actualType", scriptLocation)
