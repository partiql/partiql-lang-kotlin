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

package org.partiql.lang.eval

import org.partiql.pig.runtime.DomainNode

/**
 * Result of an evaluated PartiQLStatement
 */
sealed class PartiQLResult {

    class Value(val value: ExprValue) : PartiQLResult()

    class Insert(
        val target: String,
        val rows: Iterable<ExprValue>
    ) : PartiQLResult()

    class Delete(
        val target: String,
        val rows: Iterable<ExprValue>
    ) : PartiQLResult()

    class Replace(
        val target: String,
        val rows: Iterable<ExprValue>
    ) : PartiQLResult()

    sealed class Explain : PartiQLResult() {
        class Domain(val value: DomainNode, val format: String?) : Explain()
    }
}
