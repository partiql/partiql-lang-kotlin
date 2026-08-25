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

import java.util.Collections

/**
 * One catalog-local SQL routine binding and all of its overloads for one routine kind.
 *
 * [canonicalName] contains the complete catalog-local name, including any namespace and the routine leaf. It never
 * contains a catalog name and must not contain an empty part. The name and overload collection are copied, while the
 * overload objects are retained. Accessing [canonicalName] returns a copy so callers cannot mutate the binding through
 * the existing [Namespace.getLevels] API.
 */
public class RoutineBinding<T>(
    canonicalName: Name,
    overloads: Collection<T>,
) {
    private val canonicalNameParts: List<String> = canonicalName.toList().also { parts ->
        require(parts.none(String::isEmpty)) { "Routine binding canonical name cannot contain an empty part" }
    }

    public val canonicalName: Name
        get() = Name.of(canonicalNameParts)

    public val overloads: List<T> = Collections.unmodifiableList(ArrayList(overloads))

    init {
        require(this.overloads.isNotEmpty()) { "Routine binding overloads cannot be empty" }
    }
}
