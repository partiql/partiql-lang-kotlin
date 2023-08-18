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

package org.partiql.planner.transforms

import org.partiql.planner.ExperimentalPartiQLPlanner

// TODO: Migrate the tests from :partiql-lang.
class AggregationVisitorTransformTests {

    /**
     * TODO: Currently, since the VisitorTransform tests requires the Pig AST parser (which is only within :partiql-lang),
     *  we need to keep the VisitorTransform tests within :partiql-lang while allowing the tests to see the
     *  previously-internal constants (below). By exposing them using a "testArtifacts" configuration, we can avoid making
     *  these things public.
     */
    companion object {
        @OptIn(ExperimentalPartiQLPlanner::class)
        const val GROUP_PREFIX: String = AggregationVisitorTransform.GROUP_PREFIX

        @OptIn(ExperimentalPartiQLPlanner::class)
        const val GROUP_DELIMITER: String = AggregationVisitorTransform.GROUP_DELIMITER
    }
}
