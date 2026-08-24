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

package org.partiql.spi.catalog

import org.partiql.spi.function.AggOverload
import org.partiql.spi.function.FnOverload

/**
 * Optional [Catalog] extension for exact, namespace-aware routine lookup.
 *
 * The supplied identifier is complete and catalog-local: it contains every namespace part and the routine leaf, but
 * never the catalog name. Lookup matches the complete identifier depth without table-style longest-prefix matching.
 * Regular parts match case-insensitively and delimited parts match case-sensitively.
 *
 * These methods are authoritative for an implementing catalog. PLK does not fall back to [Catalog.getFunctions] or
 * [Catalog.getAggregations] when exact lookup returns no binding. An adopting host must combine every routine source it
 * intends to expose behind these methods. Implementations must return stable, thread-safe, immutable bindings that
 * satisfy the same exact-result contract as [org.partiql.spi.function.RoutineProvider].
 */
public interface RoutineCatalog : Catalog {

    /**
     * Resolves scalar bindings matching the complete catalog-local [identifier].
     */
    public fun resolveFunctions(
        session: Session,
        identifier: Identifier,
    ): Collection<RoutineBinding<FnOverload>>

    /**
     * Resolves aggregate bindings matching the complete catalog-local [identifier].
     */
    public fun resolveAggregations(
        session: Session,
        identifier: Identifier,
    ): Collection<RoutineBinding<AggOverload>>
}
