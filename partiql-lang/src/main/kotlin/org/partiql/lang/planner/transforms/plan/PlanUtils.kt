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

package org.partiql.lang.planner.transforms.plan

import org.partiql.plan.Attribute
import org.partiql.plan.Rel

internal object PlanUtils {
    internal fun getSchema(input: Rel): List<Attribute> = when (input) {
        is Rel.Project -> input.common.schema
        is Rel.Aggregate -> input.common.schema
        is Rel.Bag -> input.common.schema
        is Rel.Fetch -> input.common.schema
        is Rel.Filter -> input.common.schema
        is Rel.Join -> input.common.schema
        is Rel.Scan -> input.common.schema
        is Rel.Sort -> input.common.schema
        is Rel.Unpivot -> input.common.schema
    }
}
