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

package org.partiql.lang.planner.transforms.impl

import org.partiql.lang.planner.transforms.ObjectHandle
import org.partiql.lang.planner.transforms.PlannerSession
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.sources.ValueDescriptor

/**
 * Acts to consolidate multiple [org.partiql.spi.connector.ConnectorMetadata]'s.
 */
internal interface Metadata {

    public fun catalogExists(session: PlannerSession, catalogName: BindingName): Boolean

    public fun getObjectHandle(session: PlannerSession, catalog: BindingName, path: BindingPath): ObjectHandle?

    public fun getObjectDescriptor(session: PlannerSession, handle: ObjectHandle): ValueDescriptor
}
