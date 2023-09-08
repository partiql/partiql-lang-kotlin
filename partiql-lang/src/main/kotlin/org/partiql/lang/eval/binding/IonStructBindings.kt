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

package org.partiql.lang.eval.binding

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import org.partiql.lang.Ident
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.namedValue
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
internal class IonStructBindings(private val myStruct: IonStruct) : Bindings<ExprValue> {

    private val caseSensitiveFieldMap by lazy {
        HashMap<Ident, ArrayList<IonValue>>().apply {
            for (field in myStruct) {
                // SQL-ids Ident.createAsIs (compared to createFromDelimited) might end up being essential here,
                // if we want to avoid doing any normalization on the text content of the field name.
                val entries = getOrPut(Ident.createAsIs(field.fieldName)) { ArrayList(1) }
                entries.add(field)
            }
        }
    }

    private fun caseSensitiveLookup(fieldName: Ident): IonValue? =
        caseSensitiveFieldMap[fieldName]?.let { entries -> handleMatches(entries, fieldName) }

    private fun handleMatches(entries: List<IonValue>, fieldName: Ident): IonValue? =
        when (entries.size) {
            0 -> null
            1 -> entries[0]
            else ->
                errAmbiguousBinding(fieldName, entries.map { it.fieldName })
        }

    override operator fun get(bindingName: Ident): ExprValue? =
        caseSensitiveLookup(bindingName)?.let {
            ExprValue.of(it).namedValue(ExprValue.newString(it.fieldName))
        }
}
