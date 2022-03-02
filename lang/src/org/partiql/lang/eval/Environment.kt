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

/**
 * The environment for execution.
 *
 * TODO: would love to make this class `internal`, but it is currently an argument to `ExprFunction.call`.  That's
 * TODO: a big refactor.
 *
 * @param session The evaluation session.
 * @param registers An array of registers containing [ExprValue]s needed during query execution.  Generally, there is
 * one register per local variable.  When query execution begins, every register should be set to `MISSING`.
 */
class Environment(
    val session: EvaluationSession,
    val inputBindingsMap: BindingsMap = newBindingsMap()
) {

    companion object {
        fun standard() = Environment(session = EvaluationSession.standard())
    }
}
