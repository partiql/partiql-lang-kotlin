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

import org.partiql.spi.fn.AggSignature
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.types.StaticType

/**
 * A ConnectorObject represents an object in an external data source connected to PartiQL.
 *
 * This is used by plugin implementers to store logic in relation to the [ConnectorMetadata].
 *
 * At the moment, objects supported by PartiQL are
 *
 * 1. Data
 * 2. Scalar Function
 * 3. Aggregation function.
 *
 */
public sealed interface ConnectorObject {

    public interface Fn : ConnectorObject {
        /**
         * Returns a function's variants.
         *
         * @return
         */
        @OptIn(FnExperimental::class)
        public fun getVariants(): List<FnSignature>
    }

    public interface Agg : ConnectorObject {
        /**
         * Returns a function's variants.
         *
         * @return
         */
        @OptIn(FnExperimental::class)
        public fun getVariants(): List<AggSignature>
    }

    // TODO: Come up with a better name
    public interface Data : ConnectorObject {

        /**
         * Returns the type descriptor of a data object in a catalog.
         *
         * If the handle is unable to produce a [StaticType], implementers should return null.
         *
         * @return
         */
        public fun getType(): StaticType
    }
}
