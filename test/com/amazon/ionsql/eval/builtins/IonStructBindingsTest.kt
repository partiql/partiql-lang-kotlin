package com.amazon.ionsql.eval.builtins

import com.amazon.ionsql.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import org.junit.*

class IonStructBindingsTest : Base() {

    private val bindingForCaseSensitiveTests = IonStructBindings(
        ion.newEmptyStruct().apply {
            add("valueThatExists", ion.newInt(1))
            add("duplicateFieldName", ion.newInt(1))
            add("duplicateFieldName", ion.newInt(2))
        } )


    private val bindingForCaseInsensitiveTests = IonStructBindings(
        ion.newEmptyStruct().apply {
            add("valueThatExists", ion.newInt(1))
            add("ambiguousFieldName", ion.newInt(1))
            add("AmbiguousFieldName", ion.newInt(2))
        } )

    @Test
    fun caseSensitiveNotFound() =
        assertNull(bindingForCaseSensitiveTests[BindingName("doesnt_exist", BindingCase.SENSITIVE)])

    @Test
    fun caseSensitiveFound() =
        assertEquals(
            ion.newInt(1),
            bindingForCaseSensitiveTests[BindingName("valueThatExists", BindingCase.SENSITIVE)]?.ionValue)

    @Test
    fun caseSensitiveAmbiguous() =
        try {
            bindingForCaseSensitiveTests[BindingName("duplicateFieldName", BindingCase.SENSITIVE)]
            fail("Didn't throw")
        } catch(ex: EvaluationException) {
            assertEquals(ErrorCode.EVALUATOR_AMBIGUOUS_BINDING, ex.errorCode)
        }


    @Test
    fun caseInsensitiveNotFound() =
        assertNull(bindingForCaseInsensitiveTests[BindingName("doesnt_exist", BindingCase.INSENSITIVE)])

    @Test
    fun caseInsensitiveFound() =
        assertEquals(
            ion.newInt(1),
            bindingForCaseInsensitiveTests[BindingName("valueThatExists", BindingCase.INSENSITIVE)]?.ionValue)

    @Test
    fun caseInsensitiveAmbiguous() =
        try {
            bindingForCaseInsensitiveTests[BindingName("AMBIGUOUSFIELDNAME", BindingCase.INSENSITIVE)]
            fail("Didn't throw")
        } catch(ex: EvaluationException) {
            assertEquals(ErrorCode.EVALUATOR_AMBIGUOUS_BINDING, ex.errorCode)
        }
}
