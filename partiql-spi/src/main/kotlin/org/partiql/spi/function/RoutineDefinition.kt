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

package org.partiql.spi.function

import org.partiql.spi.catalog.Name
import java.util.Collections

/**
 * One provider-owned routine definition.
 *
 * [sourceName] is the stable selector within one provider and is independent of Java package names and SQL catalog
 * placement. [overloads] contains every overload contributed by this definition for the routine kind selected by the
 * [RoutineProvider] callback.
 *
 * Loading a definition does not expose it to SQL. A host must separately mount it into a catalog-local namespace.
 */
public class RoutineDefinition<T>(
    sourceName: Name,
    overloads: Collection<T>,
) {
    public val sourceName: Name = Name.of(sourceName.toList())
    public val overloads: List<T> = Collections.unmodifiableList(ArrayList(overloads))
}
