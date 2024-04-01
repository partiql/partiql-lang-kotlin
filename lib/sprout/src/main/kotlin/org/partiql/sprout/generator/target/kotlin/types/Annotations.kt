/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.sprout.generator.target.kotlin.types

import com.squareup.kotlinpoet.AnnotationSpec

object Annotations {

    val jvmStatic = AnnotationSpec.builder(JvmStatic::class).build()

    internal const val DO_NOT_IMPLEMENT_INTERFACE = "DoNotImplementInterface"

    internal const val DO_NOT_IMPLEMENT_INTERFACE_WARNING = "WARNING: This interface should not be implemented or extended by code outside of this library"

    fun suppress(what: String) = AnnotationSpec.builder(Suppress::class).addMember("\"$what\"").build()
}
