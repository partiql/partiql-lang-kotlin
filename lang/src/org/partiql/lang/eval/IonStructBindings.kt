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

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import org.partiql.lang.util.errAmbiguousBinding

/**
 * Custom implementation of [Bindings] that lazily computes case sensitive or insensitive hash tables which
 * will speed up the lookup of bindings within structs.
 *
 * The key difference in behavior between this and other [Bindings] implementations is that it
 * can throw an ambiguous binding [EvaluationException] even for case-sensitive lookups as it is
 * entirely possible that fields with identical names can appear within [IonStruct]s.
 *
 * Important: this class is critical to performance for many queries.  Change with caution.
 */
internal class IonStructBindings(private val valueFactory: ExprValueFactory, private val myStruct: IonStruct) : Bindings<ExprValue> {

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
                errAmbiguousBinding(fieldName, entries.map { it.fieldName })
        }


    override operator fun get(bindingName: BindingName): ExprValue? =
        when (bindingName.bindingCase) {
            BindingCase.SENSITIVE   -> caseSensitiveLookup(bindingName.name)
            BindingCase.INSENSITIVE -> caseInsensitiveLookup(bindingName.name)
        }?.let { valueFactory.newFromIonValue(it) }
}