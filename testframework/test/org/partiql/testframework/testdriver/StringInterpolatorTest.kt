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
class StringInterpolatorTest {
    private val ions = IonSystemBuilder.standard().build()
    private val s = ions.singleValue("""
        {
            someBool: true,
            someInt: 123,
            someString: "PartiQL Test Suite",
            someTimestamp: 2012-04-15T23:59:59.999+08:00,
            someStruct: { this: "is", a: "struct" },
            someSexp: (this is an s exp),
            intWithAnnotation: annotations_are_ignored::234,
            someNull: null
        } """.trimIndent()) as IonStruct
    
    data class TestCase(val expected: String, val stringToInterpolate: String)

    fun parametersForInterpolateStringTest() : List<TestCase>
        = listOf(
            TestCase("true", "\${someBool}"),
            TestCase("123", "\${someInt}"),
            TestCase("PartiQL Test Suite", "\${someString}"),
            TestCase(
                "PartiQL Test Suite",
                "\${ someString }"
            ),
            TestCase(
                "PartiQL Test Suite",
                "\${ \tsomeString \t}"
            ),
            TestCase(
                "PartiQL Test Suite",
                "\${    someString    }"
            ),
            TestCase(
                "2012-04-15T23:59:59.999+08:00",
                "\${someTimestamp}"
            ),
            TestCase(
                "(this is an s exp)",
                "\${someSexp}"
            ),
            TestCase(
                "{this:\"is\",a:\"struct\"}",
                "\${someStruct}"
            ),

                //Interpolation works at various locations within the string
            TestCase(
                "PartiQL Test Suiteabcdef",
                "\${someString}abcdef"
            ),
            TestCase(
                "abPartiQL Test Suitecd",
                "ab\${someString}cd"
            ),
            TestCase(
                "abcdPartiQL Test Suite",
                "abcd\${someString}"
            ),

                //How to deal with annotated values
            TestCase("234", "\${intWithAnnotation}"),

                //Null value
            TestCase(
                "null is an empty string",
                "null is an emp\${someNull}ty string"
            )
        )
    
    @Test
    @Parameters
    fun interpolateStringTest(testCase: TestCase) {
        assertEquals(testCase.expected, testCase.stringToInterpolate.interpolate(s, ions))
    }

    @Test
    fun interpolateStringUndefinedVariable() {
        assertThatThrownBy {
            "\${whyCantIEscapeDollarSignHereButNotElsewhere}".interpolate(ions.newEmptyStruct(), ions)
        }.matches { (it as UndefinedVariableInterpolationException).variableName == "whyCantIEscapeDollarSignHereButNotElsewhere"}
    }
}

