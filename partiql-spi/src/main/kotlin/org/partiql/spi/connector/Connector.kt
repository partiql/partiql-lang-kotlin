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

import com.amazon.ionelement.api.StructElement

/**
 * A mechanism by which PartiQL can access a Catalog.
 */
public interface Connector {
    public fun getMetadata(session: ConnectorSession): ConnectorMetadata

    public interface Factory {
        public fun getName(): String
        public fun create(catalogName: String, config: StructElement): Connector
    }
}
