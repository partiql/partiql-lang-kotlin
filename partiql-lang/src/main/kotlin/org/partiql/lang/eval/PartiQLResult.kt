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

package org.partiql.lang.eval

import org.partiql.pig.runtime.DomainNode

/**
 * Result of an evaluated PartiQLStatement.
 *
 */
sealed class PartiQLResult {

    /**
     * @return information relevant to which branches and branch conditions were executed. As [ExprValue]s
     * are lazily created, please materialize any relevant [ExprValue]s before accessing [coverageData].
     */
    public abstract fun getCoverageData(): CoverageData?

    class Value(
        val value: ExprValue,
        private val coverageData: () -> CoverageData? = { null }
    ) : PartiQLResult() {
        override fun getCoverageData(): CoverageData? = coverageData.invoke()
    }

    class Insert(
        val target: String,
        val rows: Iterable<ExprValue>,
        private val coverageData: () -> CoverageData? = { null }
    ) : PartiQLResult() {
        override fun getCoverageData(): CoverageData? = coverageData.invoke()
    }

    class Delete(
        val target: String,
        val rows: Iterable<ExprValue>,
        private val coverageData: () -> CoverageData? = { null }
    ) : PartiQLResult() {
        override fun getCoverageData(): CoverageData? = coverageData.invoke()
    }

    class Replace(
        val target: String,
        val rows: Iterable<ExprValue>,
        private val coverageData: () -> CoverageData? = { null }
    ) : PartiQLResult() {
        override fun getCoverageData(): CoverageData? = coverageData.invoke()
    }

    sealed class Explain : PartiQLResult() {
        data class Domain(val value: DomainNode, val format: String?) : Explain() {
            override fun getCoverageData(): CoverageData? = null
        }
    }
}
