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

package org.partiql.lang.compiler

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.PartiQLStatement
import org.partiql.lang.util.PartiQLExperimental

/**
 * [PartiQLCompiler] is responsible for transforming a [PartiqlPhysical.Plan] into an executable [PartiQLStatement].
 */
@PartiQLExperimental
interface PartiQLCompiler {

    /**
     * Compiles the [PartiqlPhysical.Plan] to an executable [PartiQLStatement].
     */
    fun compile(statement: PartiqlPhysical.Plan): PartiQLStatement
}
