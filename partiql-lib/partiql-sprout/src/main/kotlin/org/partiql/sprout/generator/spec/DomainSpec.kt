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

package org.partiql.sprout.generator.spec

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import org.partiql.sprout.model.Domain

/**
 * Wraps a [Domain] with KotlinPoet builders
 *
 * @property domain         Domain definition
 * @property name           Domain class ClassName
 * @property builder        Domain class builder
 * @property companion      Companion object builder
 * @property packages       Additional packages to include in the domain package
 * @property files          Additional files to include in the domain package
 */
class DomainSpec(
    val domain: Domain,
    val name: ClassName,
    val builder: TypeSpec.Builder,
    val companion: TypeSpec.Builder,
    val packages: MutableList<PackageSpec> = mutableListOf(),
    val files: MutableList<FileSpec> = mutableListOf()
) {

    fun build() = with(builder) {
        addType(companion.build())
        build()
    }
}
