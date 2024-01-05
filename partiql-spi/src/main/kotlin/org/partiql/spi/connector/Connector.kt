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

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue

/**
 * A mechanism by which PartiQL can access a Catalog.
 */
public interface Connector {

    /**
     * Returns a [ConnectorMetadata] for the given [ConnectorSession]. The [ConnectorMetadata] is responsible
     * for accessing catalog metadata.
     *
     * @param session
     * @return
     */
    public fun getMetadata(session: ConnectorSession): ConnectorMetadata

    /**
     * Returns a [ConnectorBindings] which the engine uses to load values.
     *
     * @return
     */
    public fun getBindings(): ConnectorBindings

    /**
     * Returns a [ConnectorFunctions] which the engine uses to load user-defined-function implementations.
     *
     * @return
     */
    @OptIn(ConnectorFunctionExperimental::class)
    public fun getFunctions(): ConnectorFunctions

    /**
     * A Plugin leverages a [Factory] to produce a [Connector] which is used for catalog metadata and data access.
     */
    public interface Factory {

        /**
         * The connector name used to register the factory.
         */
        public val name: String

        /**
         * The connector factory method.
         *
         * @param catalogName   The name of the catalog to be backed by this [Connector] instance.
         * @param config        Configuration
         * @return
         */
        @OptIn(PartiQLValueExperimental::class)
        public fun create(catalogName: String, config: StructValue<PartiQLValue>? = null): Connector
    }
}
