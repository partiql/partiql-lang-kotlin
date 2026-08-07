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

import java.util.Collection;
import java.util.Collections;

/**
 * Supplies scalar and aggregate routine definitions for explicit loading by a host.
 * <p>
 * Loading a provider validates and snapshots its inventory but does not expose any routine to SQL. Default-empty
 * methods let a provider implement only the routine kinds it supplies and allow future kinds to be introduced without
 * breaking existing Java implementations.
 */
public interface RoutineProvider {

    /**
     * Returns this provider's scalar routine definitions.
     *
     * @return scalar routine definitions
     */
    @NotNull
    default Collection<RoutineDefinition<FnOverload>> getFunctions() {
        return Collections.emptyList();
    }

    /**
     * Returns this provider's aggregate routine definitions.
     *
     * @return aggregate routine definitions
     */
    @NotNull
    default Collection<RoutineDefinition<AggOverload>> getAggregations() {
        return Collections.emptyList();
    }
}
