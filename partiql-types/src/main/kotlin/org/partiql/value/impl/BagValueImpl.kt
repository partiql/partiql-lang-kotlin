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

package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.BagValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal class BagValueImpl<T : PartiQLValue>(
    private val delegate: Iterable<T>?,
    override val annotations: PersistentList<String>,
) : BagValue<T>() {

    override val isNull: Boolean = delegate == null

    override fun iterator(): Iterator<T> = delegate!!.iterator()

    override fun copy(annotations: Annotations) = BagValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): BagValue<T> =
        _withAnnotations(annotations.toPersistentList())

    override fun withoutAnnotations(): BagValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitBag(this, ctx)

    override fun toString(): String {
        return delegate?.joinToString(separator = ", ", prefix = "<< ", postfix = " >>") ?: "null.bag"
    }
}
