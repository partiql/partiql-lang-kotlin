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

package org.partiql.lang.eval.physical

import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue

/**
 * Contains state needed during query evaluation such as an instance of [EvaluationSession] and an array of [registers]
 * for each local variable that is part of the query.
 *
 * Since the elements of [registers] are mutable, when/if we decide to make query execution multi-threaded, we'll have
 * to take care to not share [EvaluatorState] instances among different threads.
 *
 * This class must be public because it has to be accessible to custom physical operator implementations.
 *
 * @param session The evaluation session.
 * @param registers An array of registers containing [ExprValue]s needed during query execution.  Generally, there is
 * one register per local variable.  This is an array (and not a [List]) because its semantics match exactly what we
 * need: fixed length but mutable elements.
 */
class EvaluatorState(
    val session: EvaluationSession,
    /**
     * The current state of all variables during query execution.  This is marked as `internal` because there is no
     * reasonable use-case for applications embedding PartiQL to either read or write directly to this state.  Doing
     * so is very dangerous. Modification of this state by applications embedding PartiQL should be considered
     * prohibited, except through [org.partiql.lang.eval.physical.SetVariableFunc] arguments to certain custom physical
     * operators.
     */
    internal val registers: Array<ExprValue>
)
