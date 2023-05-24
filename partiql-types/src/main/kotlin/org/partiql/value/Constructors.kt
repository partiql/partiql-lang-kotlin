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

@file: JvmName("PartiQL")
package org.partiql.value

import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.impl.BoolValueImpl

@JvmOverloads
public fun boolValue(
    v: Boolean,
    annotations: Annotations = emptyList()
): BoolValue = BoolValueImpl(v, annotations.toPersistentList())
