/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.lang.planner

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemHandler
import org.partiql.pig.runtime.DomainNode

/**
 * [PartiQLPlannerPass] is a transformation of the plan representation of a PartiQL query.
 *
 * TODO lower the upper bound of T to `Plan` after https://github.com/partiql/partiql-ir-generator/issues/65
 */
fun interface PartiQLPlannerPass<T : DomainNode> {
    fun apply(plan: T, problemHandler: ProblemHandler): T

    fun interface Physical : PartiQLPlannerPass<PartiqlPhysical.Plan>
}
