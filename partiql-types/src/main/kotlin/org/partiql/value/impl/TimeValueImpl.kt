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
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.TimeValue
import org.partiql.value.datetime.Time
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class TimeValueImpl(
    override val value: Time?,
    override val annotations: PersistentList<String>,
) : TimeValue() {

    override fun copy(annotations: Annotations) = TimeValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): TimeValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): TimeValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitTime(this, ctx)
}
