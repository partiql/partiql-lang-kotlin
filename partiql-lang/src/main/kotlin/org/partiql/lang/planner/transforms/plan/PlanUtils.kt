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

import org.partiql.plan.Arg
import org.partiql.plan.Attribute
import org.partiql.plan.Binding
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Step
import org.partiql.types.StaticType

internal object PlanUtils {
    internal fun getTypeEnv(input: Rel): List<Attribute> = when (input) {
        is Rel.Project -> input.common.typeEnv
        is Rel.Aggregate -> input.common.typeEnv
        is Rel.Bag -> input.common.typeEnv
        is Rel.Fetch -> input.common.typeEnv
        is Rel.Filter -> input.common.typeEnv
        is Rel.Join -> input.common.typeEnv
        is Rel.Scan -> input.common.typeEnv
        is Rel.Sort -> input.common.typeEnv
        is Rel.Unpivot -> input.common.typeEnv
        is Rel.Exclude -> input.common.typeEnv
    }

    internal fun Rex.addType(type: StaticType): Rex = when (this) {
        is Rex.Agg -> this.copy(type = type)
        is Rex.Binary -> this.copy(type = type)
        is Rex.Call -> this.copy(type = type)
        is Rex.Collection.Array -> this.copy(type = type)
        is Rex.Collection.Bag -> this.copy(type = type)
        is Rex.Id -> this.copy(type = type)
        is Rex.Lit -> this.copy(type = type)
        is Rex.Path -> this.copy(type = type)
        is Rex.Query.Collection -> this.copy(type = type)
        is Rex.Query.Scalar.Pivot -> this.copy(type = type)
        is Rex.Query.Scalar.Subquery -> this.copy(type = type)
        is Rex.Switch -> this.copy(type = type)
        is Rex.Tuple -> this.copy(type = type)
        is Rex.Unary -> this.copy(type = type)
    }

    internal fun Rex.grabType(): StaticType? = when (this) {
        is Rex.Agg -> this.type
        is Rex.Binary -> this.type
        is Rex.Call -> this.type
        is Rex.Collection.Array -> this.type
        is Rex.Collection.Bag -> this.type
        is Rex.Id -> this.type
        is Rex.Lit -> this.type
        is Rex.Path -> this.type
        is Rex.Query.Collection -> this.type
        is Rex.Query.Scalar.Pivot -> this.type
        is Rex.Tuple -> this.type
        is Rex.Unary -> this.type
        is Rex.Query.Scalar.Subquery -> this.type
        is Rex.Switch -> this.type
    }

    internal fun PlanNode.grabType(): StaticType? = when (this) {
        is Rex -> this.grabType()
        is Arg.Value -> this.value.grabType()
        is Arg.Type -> this.type
        is Step.Key -> this.value.grabType()
        is Binding -> this.value.grabType()
        else -> error("Unable to grab static type of $this")
    }
}
