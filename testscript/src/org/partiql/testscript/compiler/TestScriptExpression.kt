package org.partiql.testscript.compiler

import com.amazon.ion.IonSexp
import com.amazon.ion.IonStruct
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
                          val expected: IonSexp,
                          override val scriptLocation: ScriptLocation) : TestScriptExpression()

/**
 * Compiled expression for an appended test.
 */
data class AppendedTestExpression(override val id: String,
                                  val original: TestExpression,
                                  val additionalData: IonStruct,
                                  override val scriptLocation: ScriptLocation) : TestScriptExpression()
