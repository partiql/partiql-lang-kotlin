/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.parser

/**
 * Each node is hashable and has a unique identifier. Metadata is kept externally.
 * Delegate once we are on Kotlin 1.7
 */
public class SourceLocations private constructor(
    private val delegate: Map<String, SourceLocation>
) : Map<String, SourceLocation> {

    override val entries: Set<Map.Entry<String, SourceLocation>> = delegate.entries

    override val keys: Set<String> = delegate.keys

    override val size: Int = delegate.size

    override val values: Collection<SourceLocation> = delegate.values

    override fun containsKey(key: String): Boolean = delegate.containsKey(key)

    override fun containsValue(value: SourceLocation): Boolean = delegate.containsValue(value)

    override fun get(key: String): SourceLocation? = delegate[key]

    override fun isEmpty(): Boolean = delegate.isEmpty()

    internal class Mutable {

        private val delegate = mutableMapOf<String, SourceLocation>()

        operator fun set(id: String, value: SourceLocation) = delegate.put(id, value)

        fun toMap() = SourceLocations(delegate)
    }
}
