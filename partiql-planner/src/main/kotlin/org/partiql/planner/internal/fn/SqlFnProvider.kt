/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.planner.internal.fn

/**
 * TODO !! TEMPORARY AS FUNCTIONS ARE MOVED FROM CONNECTORS TO PLANNER.
 */
public object SqlFnProvider {

    private val fnNameIndex = SqlBuiltins.builtins.groupBy({ it.signature.name }, { it.signature })
    private val fnSpecIndex = SqlBuiltins.builtins.associateBy { it.signature.specific }
    private val aggNameIndex = SqlBuiltins.aggregations.groupBy({ it.signature.name }, { it.signature })
    private val aggSpecIndex = SqlBuiltins.aggregations.associateBy { it.signature.specific }

    //
    // INTERNAL PLANNER APIS
    //
    internal fun lookupFn(name: String) = fnNameIndex[name]
    internal fun lookupAgg(name: String) = aggNameIndex[name]

    //
    // TEMPORARY PUBLIC EVALUATOR APIS
    //
    public fun getFn(specific: String): Fn? = fnSpecIndex[specific]
    public fun getAgg(specific: String): Agg? = aggSpecIndex[specific]
}
