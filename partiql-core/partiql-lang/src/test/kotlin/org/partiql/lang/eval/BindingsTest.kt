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

package org.partiql.lang.eval

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.partiql.lang.ION
import org.partiql.lang.SqlException
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.util.newFromIonText

class BindingsTest {
    private val valueFactory = ExprValueFactory.standard(ION)

    @Test
    fun delegate() {
        val innerBindings = valueFactory.newFromIonText("{a:1, b:2}").bindings
        val outerBindings = valueFactory.newFromIonText("{b:3, c:4}").bindings

        val delegatedBindings = innerBindings.delegate(outerBindings)

        // note that "b" in the inner bindings is shadowed by the "b" in the outer bindings.
        assertEquals(2, delegatedBindings[BindingName("b", BindingCase.INSENSITIVE)]!!.intValue())
        assertEquals(4, delegatedBindings[BindingName("c", BindingCase.INSENSITIVE)]!!.intValue())
    }

    @Test
    fun testLazyBindings() {
        var fooEvaluateCount = 0
        var barEvaluateCount = 0
        var bAtEvaluateCount = 0
        var BaTEvaluateCount = 0

        val testBindings = Bindings.buildLazyBindings<ExprValue> {
            addBinding("foo") {
                fooEvaluateCount++
                valueFactory.newInt(10)
            }
            addBinding("bar") {
                barEvaluateCount++
                valueFactory.newInt(20)
            }
            addBinding("bAt") {
                bAtEvaluateCount++
                valueFactory.newInt(30)
            }
            addBinding("BaT") {
                BaTEvaluateCount++
                valueFactory.newInt(40)
            }
        }

        fun lookupSensitive(name: String) =
            testBindings[BindingName(name, BindingCase.SENSITIVE)]!!.scalar.numberValue()!!.toInt()

        fun lookupInsensitive(name: String) =
            testBindings[BindingName(name, BindingCase.INSENSITIVE)]!!.scalar.numberValue()!!.toInt()

        // Nothing should be evaluated yet
        kotlin.test.assertEquals(0, fooEvaluateCount, "foo should not yet be evaluated")
        kotlin.test.assertEquals(0, barEvaluateCount, "bar should not yet be evaluated")
        kotlin.test.assertEquals(0, bAtEvaluateCount, "bAt should not yet be evaluated")
        kotlin.test.assertEquals(0, BaTEvaluateCount, "BaT should not yet be evaluated")

        // Multiple case-sensitive lookups of foo should cause it to only be evaluated once
        assertEquals(10, lookupSensitive("foo"))
        assertEquals(10, lookupSensitive("foo"))

        kotlin.test.assertEquals(1, fooEvaluateCount, "foo should be evaluated once")
        kotlin.test.assertEquals(0, barEvaluateCount, "bar should not yet be evaluated")
        kotlin.test.assertEquals(0, bAtEvaluateCount, "bAt should not yet be evaluated")
        kotlin.test.assertEquals(0, BaTEvaluateCount, "BaT should not yet be evaluated")

        // A case-sensitive and case-insensitive lookup of var should still only cause it to be evaluated once
        assertEquals(20, lookupInsensitive("BaR"))
        assertEquals(20, lookupSensitive("bar"))

        kotlin.test.assertEquals(1, fooEvaluateCount, "foo should be evaluated once")
        kotlin.test.assertEquals(1, barEvaluateCount, "bar should be evaluated once")
        kotlin.test.assertEquals(0, bAtEvaluateCount, "bAt should not yet be evaluated")
        kotlin.test.assertEquals(0, BaTEvaluateCount, "BaT should not yet be evaluated")

        // Multiple case-sensitive lookups of bAt should cause it to only be evaluated once
        assertEquals(30, lookupSensitive("bAt"))
        assertEquals(30, lookupSensitive("bAt"))

        kotlin.test.assertEquals(1, fooEvaluateCount, "foo should be evaluated once")
        kotlin.test.assertEquals(1, barEvaluateCount, "bar should be evaluated once")
        kotlin.test.assertEquals(1, bAtEvaluateCount, "bAt should be evaluated once")
        kotlin.test.assertEquals(0, BaTEvaluateCount, "BaT should not yet be evaluated")

        // Multiple case-sensitive lookups of BaT should cause it to only be evaluated once
        assertEquals(40, lookupSensitive("BaT"))
        assertEquals(40, lookupSensitive("BaT"))

        kotlin.test.assertEquals(1, fooEvaluateCount, "foo should be evaluated once")
        kotlin.test.assertEquals(1, barEvaluateCount, "bar should be evaluated once")
        kotlin.test.assertEquals(1, bAtEvaluateCount, "bAt should be evaluated once")
        kotlin.test.assertEquals(1, BaTEvaluateCount, "BaT should be evaluated once")

        val ex = assertThrows<SqlException>("case insensitive lookup of bat should result in ambiguous binding error") {
            testBindings[BindingName("bat", BindingCase.INSENSITIVE)]
        }
        assertEquals(ErrorCode.EVALUATOR_AMBIGUOUS_BINDING, ex.errorCode)
    }

    private val bindingForCaseSensitiveTests = Bindings.ofIonStruct(
        ION.newEmptyStruct().apply {
            add("valueThatExists", ION.newInt(1))
            add("duplicateFieldName", ION.newInt(1))
            add("duplicateFieldName", ION.newInt(2))
        },
        valueFactory
    )

    private val bindingForCaseInsensitiveTests = Bindings.ofIonStruct(
        ION.newEmptyStruct().apply {
            add("valueThatExists", ION.newInt(1))
            add("ambiguousFieldName", ION.newInt(1))
            add("AmbiguousFieldName", ION.newInt(2))
        },
        valueFactory
    )

    @Test
    fun BindingsOfIonStruct_caseSensitiveNotFound() =
        assertNull(bindingForCaseSensitiveTests[BindingName("doesnt_exist", BindingCase.SENSITIVE)])

    @Test
    fun BindingsOfIonStruct_caseSensitiveFound() =
        assertEquals(
            ION.newInt(1),
            bindingForCaseSensitiveTests[BindingName("valueThatExists", BindingCase.SENSITIVE)]?.toIonValue(ION)
        )

    @Test
    fun BindingsOfIonStruct_caseSensitiveAmbiguous() =
        try {
            bindingForCaseSensitiveTests[BindingName("duplicateFieldName", BindingCase.SENSITIVE)]
            fail("Didn't throw")
        } catch (ex: EvaluationException) {
            assertEquals(ErrorCode.EVALUATOR_AMBIGUOUS_BINDING, ex.errorCode)
        }

    @Test
    fun BindingsOfIonStruct_caseInsensitiveNotFound() =
        assertNull(bindingForCaseInsensitiveTests[BindingName("doesnt_exist", BindingCase.INSENSITIVE)])

    @Test
    fun BindingsOfIonStruct_caseInsensitiveFound() =
        assertEquals(
            ION.newInt(1),
            bindingForCaseInsensitiveTests[BindingName("valueThatExists", BindingCase.INSENSITIVE)]?.toIonValue(ION)
        )

    @Test
    fun BindingsOfIonStruct_caseInsensitiveAmbiguous() =
        try {
            bindingForCaseInsensitiveTests[BindingName("AMBIGUOUSFIELDNAME", BindingCase.INSENSITIVE)]
            fail("Didn't throw")
        } catch (ex: EvaluationException) {
            assertEquals(ErrorCode.EVALUATOR_AMBIGUOUS_BINDING, ex.errorCode)
        }
}
