/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.plugins.memory

import com.amazon.ionelement.api.StructElement
import org.partiql.eval.bindings.Bindings
import org.partiql.planner.metadata.Metadata
import org.partiql.spi.connector.Connector

/**
 * This is a plugin used for testing and is not a versioned API per semver.
 */
public class MemoryConnector private constructor(private val catalog: MemoryCatalog) : Connector {

    public companion object {

        @JvmStatic
        public fun from(catalog: MemoryCatalog): MemoryConnector = MemoryConnector(catalog)
    }

    override fun getBindings(): Bindings = catalog.getBindings()
    override fun getMetadata(): Metadata = catalog.getMetadata()

    /**
     * For use with ServiceLoader to instantiate a connector from an Ion config.
     */
    internal class Factory : Connector.Factory {

        override val name: String = "memory"

        override fun create(config: StructElement): Connector {
            TODO("Instantiation of a MemoryConnector via the factory is currently not supported")
        }
    }
}
