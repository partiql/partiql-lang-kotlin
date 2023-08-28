@file:OptIn(PartiQLValueExperimental::class)

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

import org.partiql.value.Annotations
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
@Suppress("FunctionName")
internal inline fun <reified T : PartiQLValue> T._withAnnotations(annotations: Annotations): T =
    when {
        annotations.isEmpty() -> this
        else -> copy(annotations = this.annotations.plus(annotations)) as T
    }

@OptIn(PartiQLValueExperimental::class)
@Suppress("FunctionName")
internal inline fun <reified T : PartiQLValue> T._withoutAnnotations(): T =
    when {
        this.annotations.isNotEmpty() -> copy(annotations = emptyList()) as T
        else -> this
    }
