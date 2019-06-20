package org.partiql.testscript.compiler

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import org.partiql.testscript.parser.ScriptLocation

/**
 * Top level PTS compiled expression  
 */
sealed class TestScriptExpression {
    abstract val id: String
    abstract val scriptLocation: ScriptLocation
}


/**
 * Compiled expression for a skipped test. 
 */
data class SkippedTestExpression(override val id: String,
                                 val original: TestExpression,
                                 override val scriptLocation: ScriptLocation) : TestScriptExpression()

/**
 * Compiled expression for a single test.
 */
data class TestExpression(override val id: String,
                          val description: String?,
                          val statement: String,
                          val environment: IonStruct,
                          val expected: ExpectedResult,
                          override val scriptLocation: ScriptLocation) : TestScriptExpression()

/**
 * Compiled expression for an appended test.
 */
data class AppendedTestExpression(override val id: String,
                                  val original: TestExpression,
                                  val additionalData: IonStruct,
                                  override val scriptLocation: ScriptLocation) : TestScriptExpression()

/**
 * A test expected result
 */
sealed class ExpectedResult

/**
 * A success expectation with the statement result represented as an IonValue
 */
data class ExpectedSuccess(val expected: IonValue) : ExpectedResult()

/**
 * Singleton for expected errors  
 */
object ExpectedError : ExpectedResult() 