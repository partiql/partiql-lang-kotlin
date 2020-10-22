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
import org.partiql.lang.domains.PartiqlAst

/**
 * Contains references to each of the aggregate call-sites in a given [Select].
 */
data class AggregateCallSiteListMeta(val aggregateCallSites: List<PartiqlAst.Expr.CallAgg>): Meta {
    override val tag = TAG

    override fun serialize(writer: IonWriter) {
        throw UnsupportedOperationException("AggregateCallSiteListMeta meant for internal use only and cannot be serialized.")
    }

    companion object {
        const val TAG = "\$aggregate_call_sites"
        //Note: no deserializer if we don't support serialization.
    }
}
