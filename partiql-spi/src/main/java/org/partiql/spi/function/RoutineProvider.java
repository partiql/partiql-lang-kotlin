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

package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.catalog.Identifier;
import org.partiql.spi.catalog.RoutineBinding;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Provides exact, catalog-local scalar and aggregate routine lookup.
 *
 * <p>A provider does not own a catalog name or session path. A host exposes a provider through a
 * {@code RoutineCatalog} and independently configures the namespaces searched for unqualified calls.</p>
 *
 * <p>Implementations must return stable, thread-safe, immutable results for their host catalog's lifetime. Each
 * returned binding must match the complete identifier using regular or delimited matching for every part. Default-empty
 * methods let an implementation support only one routine kind. Binding names and overload collections must be
 * nonempty, each overload signature name must equal its binding's leaf, and one binding must not contain duplicate
 * parameter-type sequences.</p>
 */
public interface RoutineProvider {

    /**
     * Returns scalar bindings matching the complete catalog-local identifier.
     *
     * @param identifier complete catalog-local identifier
     * @return matching scalar bindings
     */
    @NotNull
    default Collection<RoutineBinding<FnOverload>> getFunctions(@NotNull Identifier identifier) {
        Objects.requireNonNull(identifier, "identifier");
        return Collections.emptyList();
    }

    /**
     * Returns aggregate bindings matching the complete catalog-local identifier.
     *
     * @param identifier complete catalog-local identifier
     * @return matching aggregate bindings
     */
    @NotNull
    default Collection<RoutineBinding<AggOverload>> getAggregations(@NotNull Identifier identifier) {
        Objects.requireNonNull(identifier, "identifier");
        return Collections.emptyList();
    }
}
