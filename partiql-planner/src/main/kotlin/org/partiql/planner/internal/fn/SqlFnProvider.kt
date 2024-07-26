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

    // planner and evaluator lookup
    private val fnNameIndex: Map<String, Fn> = SqlBuiltins.builtins.associateBy { it.signature.name }
    private val fnSpecIndex: Map<String, Fn> = SqlBuiltins.builtins.associateBy { it.signature.specific }
    public fun getFnByName(name: String): Fn? = fnNameIndex[name]
    public fun getFnBySpecific(specific: String): Fn? = fnSpecIndex[specific]

    // planner and evaluator lookup
    private val aggNameIndex: Map<String, Agg> = SqlBuiltins.aggregations.associateBy { it.signature.name }
    private val aggSpecIndex: Map<String, Agg> = SqlBuiltins.aggregations.associateBy { it.signature.specific }
    public fun getAggByName(name: String): Agg? = aggNameIndex[name]
    public fun getAggBySpecific(specific: String): Agg? = aggSpecIndex[specific]
}
