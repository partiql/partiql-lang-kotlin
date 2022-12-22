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

package org.partiql.sprout.generator.poems

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import org.partiql.sprout.generator.Poem
import org.partiql.sprout.generator.Symbols
import org.partiql.sprout.generator.spec.UniverseSpec

class MetasPoem(symbols: Symbols) : Poem(symbols) {

    override val id = "metas"

    // Consider a TypeAlias
    private val stringMapOfAny = MUTABLE_MAP.parameterizedBy(STRING, ANY)

    private val metas = PropertySpec.builder("metadata", stringMapOfAny).initializer("mutableMapOf()").build()

    override fun apply(universe: UniverseSpec) {
        universe.base.addProperty(metas.toBuilder().build())
        super.apply(universe)
    }
}
