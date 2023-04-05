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

package org.partiql.lang.planner.transforms

import org.partiql.spi.connector.ConnectorObjectHandle

/**
 * Represents a [ConnectorObjectHandle], but also adds information relevant to the associated Catalog.
 *
 * This way, the [ConnectorObjectHandle] can be returned by a [org.partiql.spi.connector.Connector] without the Connector
 * knowing which Catalog it is mapped to internally.
 */
internal class ObjectHandle(
    val connectorHandle: ConnectorObjectHandle,
    val catalogName: String
)
