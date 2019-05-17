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

import com.amazon.ion.*
import com.amazon.ion.system.*
import junitparams.*
import org.assertj.core.api.Assertions.*
import org.junit.*
import org.junit.runner.*
import kotlin.test.*


@RunWith(JUnitParamsRunner::class)
class IonValueInterpolatorTest {
    private val ions = IonSystemBuilder.standard().build()

    private val variables = ions.singleValue("""
{
    aString: "this is aString",
    aList: [ "this is aList" ],
    aStruct: { any: "this is aStruct" },
    aSexp: ( "this is aSexp" ),
    anInt: 123,
    aClob: '''this is aClob''',
    aBool: true
}
""") as IonStruct

    class SuccessTestCase(val expectedIon: String, val ionToInterpolate: String)

    fun parametersForSuccessCases() =
        listOf(
            SuccessTestCase(
                "\"this is aString\"", "\$aString"
            ),

            SuccessTestCase(
                "[\"this is aList\"]", "\$aList"
            ),

            SuccessTestCase(
                "{any:\"this is aStruct\"}", "\$aStruct"
            ),

            SuccessTestCase(
                "(\"this is aSexp\")", "\$aSexp"
            ),

            SuccessTestCase(
                "123", "\$anInt"
            ),

            SuccessTestCase(
                "\"this is aClob\"", "\$aClob"
            ),

            SuccessTestCase(
                "true", "\$aBool"
            ),

            SuccessTestCase(
                "{foo:[\"this is aList\"]}", "{ foo: \$aList }"
            ),

            SuccessTestCase(
                "{foo:{any:\"this is aStruct\"}}", "{ foo: \$aStruct }"
            ),

            SuccessTestCase(
                "{foo:(\"this is aSexp\")}", "{ foo: \$aSexp }"
            ),

            SuccessTestCase(
                "{foo:true}", "{ foo: \$aBool }"
            ),

            SuccessTestCase(
                "[[\"this is aList\"],{any:\"this is aStruct\"},(\"this is aSexp\"),123]",
                "[ \$aList, \$aStruct, \$aSexp, \$anInt ]"
            ),

            SuccessTestCase(
                "((((({any:\"this is aStruct\"})))))", "((((( \$aStruct )))))"
            )
        )

    @Test
    @Parameters
    fun successCases(testCase: SuccessTestCase) {
        val expected = ions.singleValue(testCase.expectedIon)
        val input = ions.singleValue(testCase.ionToInterpolate)
        val result = input.interpolate(variables, ions)

        assertEquals(expected, result)
    }

    fun parametersForUndefinedVariableCases(): List<String>
        = listOf(
            "\$undefinedVariable",
            "\"\${undefinedVariable}\"",
            "{ a_field: \$undefinedVariable }",
            "[ \$undefinedVariable ]",
            "( \$undefinedVariable )"
        )

    @Test
    @Parameters
    fun undefinedVariableCases(ionToInterpolate: String) {
        val input = ions.singleValue(ionToInterpolate)
        assertThatThrownBy {
            input.interpolate(variables, ions)
        }.matches {
            (it as UndefinedVariableInterpolationException).variableName == "undefinedVariable"
        }
    }
    }

