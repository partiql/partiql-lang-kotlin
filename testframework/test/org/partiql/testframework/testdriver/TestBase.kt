/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.testframework.testdriver

import com.amazon.ion.system.*
import org.partiql.testframework.testcar.*
import org.partiql.testframework.testdriver.ast.*
import org.partiql.testframework.testdriver.error.*
import org.partiql.testframework.testdriver.parser.*
import org.partiql.testframework.testdriver.rewriters.*
import org.partiql.testframework.testdriver.visitors.*
import org.junit.*
import org.partiql.lang.eval.*
import kotlin.test.*

abstract class TestBase {
    protected val ion = IonSystemBuilder.standard().build()!!
    protected val valueFactory = ExprValueFactory.standard(ion)
    protected var resultStream: ResultStream = createResultStream()
    protected var visitor = createScriptExecuteVisitor()

    private fun createScriptExecuteVisitor(): ScriptExecuteVisitor = ScriptExecuteVisitor(ReferenceSqlCar(ion), resultStream, valueFactory)
    private fun createResultStream(): ResultStream = ResultStream(ion, noOpPrintStream, noOpPrintStream)

    protected fun parseScript(script: String): TestScript {
        script.replace('#', '$').byteInputStream().use {
            val parser = ScriptParser(ion.newReader(it), "unit test", ion, resultStream)
            assertEquals(0, resultStream.errorCount)
            val testScript = parser.parse()

            return ForUnrollingRewriter(ion, resultStream).rewriteTestScript(testScript)
        }
    }

    @Before
    fun beforeTestMethod() {
        resultStream = createResultStream()
        visitor = createScriptExecuteVisitor()
    }
}