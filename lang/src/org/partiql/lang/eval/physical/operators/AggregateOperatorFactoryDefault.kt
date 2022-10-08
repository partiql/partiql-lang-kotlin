/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

internal class AggregateOperatorFactoryDefault : AggregateOperatorFactory(DEFAULT_IMPL_NAME) {
    override fun create(impl: PartiqlPhysical.Impl): AggregateOperator = when (val operator = impl.name.text.toLowerCase()) {
        DEFAULT_IMPL_NAME -> AggregateOperatorDefault()
        else -> throw IllegalArgumentException("Unknown implementation of the Aggregate Operator: $operator")
    }
}
