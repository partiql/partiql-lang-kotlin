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
import org.partiql.planner.metadata.Namespace

/**
 * A mechanism by which PartiQL can access a Catalog.
 */
public interface Connector {

    /**
     * TODO
     */
    public fun getBindings(): String

    /**
     * Returns the root namespace of this catalog.
     */
    public fun getNamespace(): Namespace

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
         * @param catalogName
         * @param config
         * @return
         */
        public fun create(catalogName: String, config: StructElement? = null): Connector
    }
}
