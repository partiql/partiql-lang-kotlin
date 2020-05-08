
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

package org.partiql.lang.ast

import com.amazon.ion.*
import org.partiql.lang.ast.SetQuantifier.*
import org.partiql.lang.errors.*

fun PropertyValueMap.addSourceLocation(metas: MetaContainer): PropertyValueMap {
    (metas.find(SourceLocationMeta.TAG) as? SourceLocationMeta)?.let {
        this[Property.LINE_NUMBER] = it.lineNum
        this[Property.COLUMN_NUMBER] = it.charOffset
    }
    return this
}

/**
 * Creates an instance of [CallAgg] which is intended to be used to represent `COUNT(*)` when it is
 * used in a select list.
 */
fun createCountStar(ion: IonSystem, metas: MetaContainer): CallAgg {
    // The [VariableReference] and [Literal] below should only get the [SourceLocationMeta] if present,
    // not any other metas.
    val srcLocationMetaOnly = metas.find(SourceLocationMeta.TAG)
                                  ?.let { metaContainerOf(it) } ?: metaContainerOf()
    
    // optimize count(*) to count(1). 
    return CallAgg(
        funcExpr = VariableReference(
            id = "count",
            case = CaseSensitivity.INSENSITIVE,
            scopeQualifier = ScopeQualifier.UNQUALIFIED,
            metas = srcLocationMetaOnly),
        setQuantifier = ALL,
        arg = Literal(ion.newInt(1), srcLocationMetaOnly), 
        metas = metas.add(IsCountStarMeta.instance)
    )
}
