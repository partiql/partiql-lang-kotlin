package com.amazon.ionsql.eval

import com.amazon.ion.*
import com.amazon.ionsql.util.*


/**
 * Custom implementation of [Bindings] that lazily computes case sensitive or insensitive hash tables which
 * will speed up the lookup of bindings within structs.
 *
 * Important: this class is critical to performance for many queries.  Change with due caution.
 */
class IonStructBindings(private val myStruct: IonStruct) : Bindings {

    private val caseInsensitiveFieldMap by lazy {
        HashMap<String, ArrayList<IonValue>>().apply {
            for (field in myStruct) {
                val entries = getOrPut(field.fieldName.toLowerCase()) { ArrayList(1) }
                entries.add(field)
            }
        }
    }

    private val caseSensitiveFieldMap by lazy {
        HashMap<String, ArrayList<IonValue>>().apply {
            for (field in myStruct) {
                val entries = getOrPut(field.fieldName) { ArrayList(1) }
                entries.add(field)
            }
        }
    }

    private fun caseSensitiveLookup(fieldName: String): IonValue? =
        caseSensitiveFieldMap[fieldName]?.let { entries -> handleMatches(entries, fieldName) }

    private fun caseInsensitiveLookup(fieldName: String): IonValue? =
        caseInsensitiveFieldMap[fieldName.toLowerCase()]?.let { entries -> handleMatches(entries, fieldName) }

    private fun handleMatches(entries: List<IonValue>, fieldName: String): IonValue? =
        when (entries.size) {
            0    -> null
            1    -> entries[0]
            else ->
                BindingHelper.throwAmbiguousBindingEvaluationException(fieldName, entries.map { it.fieldName })
        }


    override operator fun get(bindingName: BindingName): ExprValue? =
        when (bindingName.bindingCase) {
            BindingCase.SENSITIVE   -> caseSensitiveLookup(bindingName.name)
            BindingCase.INSENSITIVE -> caseInsensitiveLookup(bindingName.name)
        }?.exprValue()

}