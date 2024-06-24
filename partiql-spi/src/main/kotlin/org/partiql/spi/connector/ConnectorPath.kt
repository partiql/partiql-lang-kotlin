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

import java.util.Spliterator
import java.util.function.Consumer

/**
 * The path to an object within the current Catalog.
 */
public data class ConnectorPath(public val steps: List<String>) : Iterable<String> {

    public companion object {

        @JvmStatic
        public fun of(vararg steps: String): ConnectorPath = ConnectorPath(steps.toList())
    }

    public operator fun get(index: Int): String = steps[index]

    override fun forEach(action: Consumer<in String>?): Unit = steps.forEach(action)

    override fun iterator(): Iterator<String> = steps.iterator()

    override fun spliterator(): Spliterator<String> = steps.spliterator()
}
