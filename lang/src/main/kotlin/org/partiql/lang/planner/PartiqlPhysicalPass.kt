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

package org.partiql.lang.planner

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler

/**
 * Represents a pass over the physical plan that accepts a physical plan and returns a modified
 * physical plan.
 *
 * Passes accept as input a [PartiqlPhysical.Plan] which is cloned & modified in some way before being returned.
 * A second input to the pass is an instance of [ProblemHandler], which can be used to report semantic errors and
 * warnings to the query author.
 *
 * Examples of passes:
 *
 * - Select optimal physical operator implementations.
 * - Push down predicates or projections.
 * - Convert filter predicates to index lookups.
 * - Fold constants.
 * - And many, many others, some will be specific to the application embedding PartiQL.
 *
 * Notes on exceptions and semantic problems:
 *
 * - The passes may throw any exception, however these will always abort query planning and bypass the user-friendly
 * error reporting ([ProblemHandler]) mechanisms used for
 * [syntax and semantic errors](https://www.educative.io/edpresso/what-is-the-difference-between-syntax-and-semantic-errors)
 * - Use the [ProblemHandler] to report semantic errors and warnings in the query to the query author.
 *
 * @see [ProblemHandler.handleProblem]
 * @see [Problem]
 * @see [Problem.details]
 * @see [org.partiql.lang.errors.ProblemSeverity]
 */
interface PartiqlPhysicalPass {
    val passName: String
    fun rewrite(inputPlan: PartiqlPhysical.Plan, problemHandler: ProblemHandler): PartiqlPhysical.Plan
}
