/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.test.coverage

import org.partiql.coverage.api.PartiQLTest
import org.partiql.coverage.api.PartiQLTestCase
import org.partiql.coverage.api.PartiQLTestProvider
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.test.coverage.utils.PartiQLTestCaseDefault
import org.partiql.types.StaticType
import kotlin.test.assertEquals

/**
 * Tests to make sure we can test custom functions
 */
class CustomFunctionsTest {

    @PartiQLTest(provider = CustomFunctionTestProvider::class)
    fun testCustomFunction(tc: PartiQLTestCase, result: PartiQLResult.Value) {
        // Value Assertions
        val value = result.value
        assertEquals(ExprValueType.BAG, value.type)
        val struct = value.first()
        assertEquals(ExprValueType.STRUCT, struct.type)
        val echoed = struct.bindings[BindingName("echoed", BindingCase.SENSITIVE)]
        assertEquals("hello!hello!hello!", echoed?.stringValue())

        // Coverage Assertions
        val data = result.getCoverageData()!!
        val structure = result.getCoverageStructure()!!
        assertEquals(2, structure.branches.size) // TRUE, FALSE
        assertEquals(3, structure.branchConditions.size) // TRUE, FALSE, NULL
        assertEquals(1, data.branchCount.filter { (_, count) -> count > 0 }.size) // 1 Branch executed
        assertEquals(1, data.branchConditionCount.filter { (_, count) -> count > 0 }.size) // 1 Condition executed
    }

    object CustomFunctionTestProvider : PartiQLTestProvider {
        override val statement: String = """
            SELECT echo(t.str, t.times) AS echoed
            FROM << { 'str': 'hello!', 'times': 3 } >> AS t
            WHERE t.times > 0
        """

        override fun getTestCases(): Iterable<PartiQLTestCase> = listOf(PartiQLTestCaseDefault())

        override fun getPipelineBuilder(): CompilerPipeline.Builder =
            CompilerPipeline.builder().addFunction(CustomFunction)
    }

    /**
     * Echos an input string an input number of times.
     */
    object CustomFunction : ExprFunction {
        override val signature: FunctionSignature = FunctionSignature(
            name = "echo",
            requiredParameters = listOf(StaticType.STRING, StaticType.INT),
            returnType = StaticType.STRING
        )

        override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
            val str = required[0].stringValue()
            val count = required[1].numberValue().toInt()
            val returnStr = str.repeat(count)
            return ExprValue.newString(returnStr)
        }
    }
}
