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

package org.partiql.spi.connector

/**
 * Holds a reference to an entity and its location within the Catalog.
 *
 * @property path   The absolute path to this entity within the catalog
 * @property entity The catalog entity.
 */
public data class ConnectorHandle<T>(
    public val path: ConnectorPath,
    public val entity: T,
)
