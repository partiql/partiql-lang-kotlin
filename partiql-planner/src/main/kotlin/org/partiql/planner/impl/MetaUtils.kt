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

package org.partiql.planner.impl

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.Meta
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.UNKNOWN_SOURCE_LOCATION

/** Constructs a container with the specified metas. */
internal fun metaContainerOf(vararg metas: Meta): MetaContainer =
    com.amazon.ionelement.api.metaContainerOf(metas.map { Pair(it.tag, it) })

internal val MetaContainer.sourceLocationMeta get() = this[SourceLocationMeta.TAG] as? SourceLocationMeta
internal val MetaContainer.sourceLocationMetaOrUnknown get() = this.sourceLocationMeta ?: UNKNOWN_SOURCE_LOCATION
